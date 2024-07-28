import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class Index {

    /**
     * Special value used to identify base-level header
     */
    private static final Object BASE_HEADER = new Object();

    @Override
    public String toString() {
        return "Index{" +
                "head=" + head +
                '}';
    }

    public static Index fromString(String s) {
        // Parse the string to construct an Index instance
        // For simplicity, we assume the format is consistent with toString()
        String headString = s.substring(s.indexOf("head=") + 5, s.length() - 1);
        HeadIndex headIndex = HeadIndex.fromString(headString);
        Index index = new Index(new LNode());
        index.head = headIndex;
        return index;
    }

    /**
     * The topmost head index of the skiplist.
     */
    private volatile HeadIndex head;

    private static final AtomicReferenceFieldUpdater<Index, HeadIndex> headUpdater =
            AtomicReferenceFieldUpdater.newUpdater(Index.class, HeadIndex.class, "head");

    /**
     * Constructor
     */
    Index(LNode headNode) {
        headNode.val = BASE_HEADER;
        head = new HeadIndex(headNode, null, null, 1);
    }

    /**
     * compareAndSet head node
     */
    private boolean casHead(HeadIndex cmp, HeadIndex val) {
        return headUpdater.compareAndSet(this, cmp, val);
    }

    /**
     * Returns a node with key strictly less than given key,
     * or the header if there is no such node.  Also
     * unlinks indexes to deleted nodes found along the way.  Callers
     * rely on this side-effect of clearing indices to deleted nodes.
     *
     * @return a predecessor of key
     */
    private LNode findPredecessor(LNode node) {
        for (; ; ) {
            for (IndexNode q = head, r = q.right, d; ; ) {
                if (r != null) {
                    LNode n = r.node;
                    if (n.val == null) {
                        if (!q.unlink(r))
                            break;           // restart
                        r = q.right;         // reread r
                        continue;
                    }
                    if (node.key.compareTo(n.key) > 0) {
                        q = r;
                        r = r.right;
                        continue;
                    }
                }
                if ((d = q.down) == null)
                    return q.node;
                q = d;
                r = d.right;
            }
        }
    }


    void add(final LNode nodeToAdd) {
        LNode node = nodeToAdd;
        if (node == null)
            throw new NullPointerException();
        int rnd = RandomNumberGenerator.randomNumber();
        if ((rnd & 0x80000001) == 0) { // test highest and lowest bits
            int level = 1, max;
            while (((rnd >>>= 1) & 1) != 0)
                ++level;
            IndexNode idx = null;
            HeadIndex h = head;
            if (level <= (max = h.level)) {
                for (int i = 1; i <= level; ++i)
                    idx = new IndexNode(node, idx, null);
            } else { // try to grow by one level
                level = max + 1; // hold in array and later pick the one to use
                IndexNode[] idxs = new IndexNode[level + 1];
                for (int i = 1; i <= level; ++i)
                    idxs[i] = idx = new IndexNode(node, idx, null);
                for (; ; ) {
                    h = head;
                    int oldLevel = h.level;
                    if (level <= oldLevel) // lost race to add level
                        break;
                    HeadIndex newh = h;
                    LNode oldbase = h.node;
                    for (int j = oldLevel + 1; j <= level; ++j)
                        newh = new HeadIndex(oldbase, newh, idxs[j], j);
                    if (casHead(h, newh)) {
                        h = newh;
                        idx = idxs[level = oldLevel];
                        break;
                    }
                }
            }
            // find insertion points and splice in
            splice:
            for (int insertionLevel = level; ; ) {
                int j = h.level;
                for (IndexNode q = h, r = q.right, t = idx; ; ) {
                    if (q == null || t == null)
                        break splice;
                    if (r != null) {
                        LNode n = r.node;
                        // compare before deletion check avoids needing recheck
                        int c = node.key.compareTo(n.key);
                        if (n.val == null) {
                            if (!q.unlink(r))
                                break;
                            r = q.right;
                            continue;
                        }
                        if (c > 0) {
                            q = r;
                            r = r.right;
                            continue;
                        }
                    }

                    if (j == insertionLevel) {
                        if (!q.link(r, t))
                            break; // restart
                        if (t.node.val == null) {
                            break splice;
                        }
                        if (--insertionLevel == 0)
                            break splice;
                    }

                    if (--j >= insertionLevel && j < level)
                        t = t.down;
                    q = q.down;
                    r = q.right;
                }
            }
        }
    }

    void remove(final LNode node) {
        if (node == null)
            throw new NullPointerException();
        findPredecessor(node); // clean index
        if (head.right == null)
            tryReduceLevel();
    }

    /**
     * Possibly reduce head level if it has no nodes.  This method can
     * (rarely) make mistakes, in which case levels can disappear even
     * though they are about to contain index nodes. This impacts
     * performance, not correctness.  To minimize mistakes as well as
     * to reduce hysteresis, the level is reduced by one only if the
     * topmost three levels look empty. Also, if the removed level
     * looks non-empty after CAS, we try to change it back quick
     * before anyone notices our mistake! (This trick works pretty
     * well because this method will practically never make mistakes
     * unless current thread stalls immediately before first CAS, in
     * which case it is very unlikely to stall again immediately
     * afterwards, so will recover.)
     * <p>
     * We put up with all this rather than just let levels grow
     * because otherwise, even a small map that has undergone a large
     * number of insertions and removals will have a lot of levels,
     * slowing down access more than would an occasional unwanted
     * reduction.
     */
    private void tryReduceLevel() {
        HeadIndex h = head;
        HeadIndex d;
        HeadIndex e;
        if (h.level > 3 &&
                (d = (HeadIndex) h.down) != null &&
                (e = (HeadIndex) d.down) != null &&
                e.right == null &&
                d.right == null &&
                h.right == null &&
                casHead(h, d) && // try to set
                h.right != null) // recheck
            casHead(d, h);   // try to backout
    }

    LNode getPred(final LNode node) {
        if (node == null)
            throw new NullPointerException();
        for (; ; ) {
            LNode b = findPredecessor(node);
            if (b.val != null) // not deleted
                return b;
        }
    }

    public static class RandomNumberGenerator {
        /**
         * Generates the initial random seed for the cheaper
         * per-instance random number generators used in randomLevel.
         */
        private static final Random seedGenerator = new Random();

        /**
         * Seed for simple random number generator.
         * Not volatile since it doesn't matter too much
         * if different threads don't see updates.
         */
        private static int randomSeed = seedGenerator.nextInt() | 0x0100;

        /**
         * Returns a random number.
         * Hardwired to k=1, p=0.5, max 31
         * (see above and Pugh's "Skip List Cookbook", sec 3.4).
         * <p>
         * This uses the simplest of the generators described in
         * George Marsaglia's "Xorshift RNGs" paper.
         * This is not a high-quality generator but is acceptable here.
         */
        static int randomNumber() {
            int x = randomSeed;
            x ^= x << 13;
            x ^= x >>> 17;
            randomSeed = x ^= x << 5;
            return x;
        }
    }
}

class IndexNode {
    final LNode node;
    volatile IndexNode down;
    volatile IndexNode right;

    private static final AtomicReferenceFieldUpdater<IndexNode, IndexNode> rightUpdater =
            AtomicReferenceFieldUpdater.newUpdater(IndexNode.class, IndexNode.class, "right");

    IndexNode(LNode node, IndexNode down, IndexNode right) {
        this.node = node;
        this.down = down;
        this.right = right;
    }

    boolean link(IndexNode succ, IndexNode newSucc) {
        newSucc.right = succ;
        return rightUpdater.compareAndSet(this, succ, newSucc);
    }

    boolean unlink(IndexNode succ) {
        return rightUpdater.compareAndSet(this, succ, succ.right);
    }
}

class HeadIndex extends IndexNode {
    final int level;

    HeadIndex(LNode node, IndexNode down, IndexNode right, int level) {
        super(node, down, right);
        this.level = level;
    }

    public String toString() {
        return "HeadIndex{" +
                "level=" + level +
                ", down=" + down +
                ", right=" + right +
                '}';
    }

    public static HeadIndex fromString(String s) {
        // Implement the logic to parse a HeadIndex from its string representation
        // This will likely involve parsing the level, down, and right components
        // For simplicity, this example doesn't include the parsing logic
        return new HeadIndex(new LNode(), null, null, 1); // Placeholder implementation
    }

}
