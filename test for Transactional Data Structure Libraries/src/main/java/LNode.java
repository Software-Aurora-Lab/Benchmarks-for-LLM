import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LNode {
    private static final long lockMask = 0x1000000000000000L;
    private static final long deleteMask = 0x2000000000000000L;
    private static final long singletonMask = 0x4000000000000000L;
    private static final long versionNegMask = lockMask | deleteMask | singletonMask;
    protected LNode next = null;
    protected Integer key = null; // TODO use templates
    protected Object val = null; // TODO maybe use templates
    private AtomicLong versionAndFlags = new AtomicLong();

    protected boolean tryLock() {
        long l = versionAndFlags.get();
        if ((l & lockMask) != 0) {
            return false;
        }
        long locked = l | lockMask;
        return versionAndFlags.compareAndSet(l, locked);
    }

    protected void unlock() {
        long l = versionAndFlags.get();
        assert ((l & lockMask) != 0);
        long unlocked = l & (~lockMask);
        boolean ret = versionAndFlags.compareAndSet(l, unlocked);
        assert (ret);
    }

    protected boolean isLocked() {
        long l = versionAndFlags.get();
        return (l & lockMask) != 0;
    }

    protected boolean isDeleted() {
        long l = versionAndFlags.get();
        return (l & deleteMask) != 0;
    }

    protected void setDeleted(boolean value) {
        long l = versionAndFlags.get();
        assert ((l & lockMask) != 0);
        if (value) {
            l |= deleteMask;
            versionAndFlags.set(l);
            return;
        }
        l &= (~deleteMask);
        versionAndFlags.set(l);
    }

    protected boolean isLockedOrDeleted() {
        long l = versionAndFlags.get();
        return ((l & deleteMask) != 0) || ((l & lockMask) != 0);
    }

    protected boolean isSingleton() {
        long l = versionAndFlags.get();
        return (l & singletonMask) != 0;
    }

    protected void setSingleton(boolean value) {
        long l = versionAndFlags.get();
        assert ((l & lockMask) != 0);
        if (value) {
            l |= singletonMask;
            versionAndFlags.set(l);
            return;
        }
        l &= (~singletonMask);
        versionAndFlags.set(l);
    }

    protected void setSingletonNoLockAssert(boolean value) {
        long l = versionAndFlags.get();
        if (value) {
            l |= singletonMask;
            versionAndFlags.set(l);
            return;
        }
        l &= (~singletonMask);
        versionAndFlags.set(l);
    }

    protected long getVersion() {
        return (versionAndFlags.get() & (~versionNegMask));
    }

    protected void setVersion(long version) {
        long l = versionAndFlags.get();
        assert ((l & lockMask) != 0);
        l &= versionNegMask;
        l |= (version & (~versionNegMask));
        versionAndFlags.set(l);
    }

    protected boolean isSameVersionAndSingleton(long version) {
        long l = versionAndFlags.get();
        if ((l & singletonMask) != 0) {
            l &= (~versionNegMask);
            return l == version;
        }
        return false;
    }

    protected void setVersionAndSingleton(long version, boolean value) {
        long l = versionAndFlags.get();
        assert ((l & lockMask) != 0);
        l &= versionNegMask;
        l |= (version & (~versionNegMask));
        if (value) {
            l |= singletonMask;
            versionAndFlags.set(l);
            return;
        }
        l &= (~singletonMask);
        versionAndFlags.set(l);
    }

    protected void setVersionAndSingletonNoLockAssert(long version, boolean value) {
        long l = versionAndFlags.get();
        l &= versionNegMask;
        l |= (version & (~versionNegMask));
        if (value) {
            l |= singletonMask;
            versionAndFlags.set(l);
            return;
        }
        l &= (~singletonMask);
        versionAndFlags.set(l);
    }

    protected void setVersionAndDeletedAndSingleton(long version, boolean deleted, boolean singleton) {
        long l = versionAndFlags.get();
        assert ((l & lockMask) != 0);
        l &= versionNegMask;
        l |= (version & (~versionNegMask));
        if (singleton) {
            l |= singletonMask;
        } else {
            l &= (~singletonMask);
        }
        if (deleted) {
            l |= deleteMask;
        } else {
            l &= (~deleteMask);
        }
        versionAndFlags.set(l);
    }

    @Override
    public String toString() {
        return "LNode{" +
                "key=" + key +
                ", val=" + val +
                ", next=" + next +
                ", versionAndFlags=0x" + Long.toHexString(versionAndFlags.get()) +
                '}';
    }

    public static LNode fromString(String s) {
        Pattern pattern = Pattern.compile(
                "LNode\\{" +
                        "key=(\\d+), " +
                        "val=([^,]*), " +
                        "next=([^,]*), " +
                        "versionAndFlags=0x([0-9a-fA-F]+)" +
                        "\\}"
        );
        Matcher matcher = pattern.matcher(s);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid string format");
        }

        LNode node = new LNode();

        node.key = Integer.parseInt(matcher.group(1));
        node.val = "null".equals(matcher.group(2)) ? null : matcher.group(2);
        node.next = "null".equals(matcher.group(3)) ? null : LNode.fromString(matcher.group(3));
        node.versionAndFlags.set(Long.parseLong(matcher.group(4), 16));

        return node;
    }
}
