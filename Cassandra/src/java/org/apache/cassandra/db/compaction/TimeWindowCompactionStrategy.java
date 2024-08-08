/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.db.compaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.cql3.statements.CFPropDefs;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.utils.Pair;

public class TimeWindowCompactionStrategy extends AbstractCompactionStrategy
{
    private static final Logger logger = LoggerFactory.getLogger(TimeWindowCompactionStrategy.class);

    private final TimeWindowCompactionStrategyOptions options;
    protected volatile int estimatedRemainingTasks;
    private final Set<SSTableReader> sstables = new HashSet<>();
    private long lastExpiredCheck;

    public TimeWindowCompactionStrategy(ColumnFamilyStore cfs, Map<String, String> options)
    {
        super(cfs, options);
        this.estimatedRemainingTasks = 0;
        this.options = new TimeWindowCompactionStrategyOptions(options);
        if (!options.containsKey(AbstractCompactionStrategy.TOMBSTONE_COMPACTION_INTERVAL_OPTION) && !options.containsKey(AbstractCompactionStrategy.TOMBSTONE_THRESHOLD_OPTION))
        {
            disableTombstoneCompactions = true;
            logger.debug("Disabling tombstone compactions for TWCS");
        }
        else
            logger.debug("Enabling tombstone compactions for TWCS");

    }

    @Override
    @SuppressWarnings("resource")
    public synchronized AbstractCompactionTask getNextBackgroundTask(int gcBefore)
    {
        if (!isEnabled())
            return null;

        while (true)
        {
            List<SSTableReader> latestBucket = getNextBackgroundSSTables(gcBefore);

            if (latestBucket.isEmpty())
                return null;

            LifecycleTransaction modifier = cfs.getTracker().tryModify(latestBucket, OperationType.COMPACTION);
            if (modifier != null)
                return new CompactionTask(cfs, modifier, gcBefore, false);
        }
    }

    /**
     *
     * @param gcBefore
     * @return
     */
    private List<SSTableReader> getNextBackgroundSSTables(final int gcBefore)
    {
        if (!isEnabled() || cfs.getSSTables().isEmpty())
            return Collections.emptyList();

        Set<SSTableReader> uncompacting = Sets.intersection(sstables, cfs.getUncompactingSSTables());

        Set<SSTableReader> expired = Collections.emptySet();

        if (System.currentTimeMillis() - lastExpiredCheck > options.expiredSSTableCheckFrequency)
        {
            logger.debug("TWCS expired check sufficiently far in the past, checking for fully expired SSTables");
            expired = CompactionController.getFullyExpiredSSTables(cfs, uncompacting, cfs.getOverlappingSSTables(uncompacting), gcBefore);
            lastExpiredCheck = System.currentTimeMillis();
        }
        else
        {
            logger.debug("TWCS skipping check for fully expired SSTables");
        }

        Set<SSTableReader> candidates = Sets.newHashSet(filterSuspectSSTables(uncompacting));

        List<SSTableReader> compactionCandidates = new ArrayList<>(getNextNonExpiredSSTables(Sets.difference(candidates, expired), gcBefore));
        if (!expired.isEmpty())
        {
            logger.debug("Including expired sstables: {}", expired);
            compactionCandidates.addAll(expired);
        }

        return compactionCandidates;
    }

    private List<SSTableReader> getNextNonExpiredSSTables(Iterable<SSTableReader> nonExpiringSSTables, final int gcBefore)
    {
        List<SSTableReader> mostInteresting = getCompactionCandidates(nonExpiringSSTables);

        if (mostInteresting != null)
        {
            return mostInteresting;
        }

        // if there is no sstable to compact in standard way, try compacting single sstable whose droppable tombstone
        // ratio is greater than threshold.
        List<SSTableReader> sstablesWithTombstones = new ArrayList<>();
        for (SSTableReader sstable : nonExpiringSSTables)
        {
            if (worthDroppingTombstones(sstable, gcBefore))
                sstablesWithTombstones.add(sstable);
        }
        if (sstablesWithTombstones.isEmpty())
            return Collections.emptyList();

        return Collections.singletonList(Collections.min(sstablesWithTombstones, new SSTableReader.SizeComparator()));
    }

    private List<SSTableReader> getCompactionCandidates(Iterable<SSTableReader> candidateSSTables)
    {
        HashMultimap<Long, SSTableReader> buckets = getBuckets(candidateSSTables, options.sstableWindowUnit, options.sstableWindowSize, options.timestampResolution);
        updateEstimatedCompactionsByTasks(buckets);
        List<SSTableReader> mostInteresting = newestBucket(buckets,
                                                           cfs.getMinimumCompactionThreshold(),
                                                           cfs.getMaximumCompactionThreshold(),
                                                           options.sstableWindowUnit,
                                                           options.sstableWindowSize,
                                                           options.stcsOptions);
        if (!mostInteresting.isEmpty())
            return mostInteresting;
        return null;
    }

    @Override
    public synchronized void addSSTable(SSTableReader sstable)
    {
        sstables.add(sstable);
    }

    @Override
    public synchronized void removeSSTable(SSTableReader sstable)
    {
        sstables.remove(sstable);
    }

    /**
     * Find the lowest and highest timestamps in a given timestamp/unit pair
     * Returns milliseconds, caller should adjust accordingly
     */
    public static Pair<Long,Long> getWindowBoundsInMillis(TimeUnit windowTimeUnit, int windowTimeSize, long timestampInMillis)
    {
        long lowerTimestamp;
        long upperTimestamp;
        long timestampInSeconds = timestampInMillis / 1000L;

        switch(windowTimeUnit)
        {
            case MINUTES:
                lowerTimestamp = timestampInSeconds - ((timestampInSeconds) % (60 * windowTimeSize));
                upperTimestamp = (lowerTimestamp + (60L * (windowTimeSize - 1L))) + 59L;
                break;
            case HOURS:
                lowerTimestamp = timestampInSeconds - ((timestampInSeconds) % (3600 * windowTimeSize));
                upperTimestamp = (lowerTimestamp + (3600L * (windowTimeSize - 1L))) + 3599L;
                break;
            case DAYS:
            default:
                lowerTimestamp = timestampInSeconds - ((timestampInSeconds) % (86400 * windowTimeSize));
                upperTimestamp = (lowerTimestamp + (86400L * (windowTimeSize - 1L))) + 86399L;
                break;
        }

        return Pair.create(lowerTimestamp * 1000L, upperTimestamp * 1000L);

    }

    /**
     * Group files with similar max timestamp into buckets.
     *
     * @param files pairs consisting of a file and its min timestamp
     * @param sstableWindowUnit
     * @param sstableWindowSize
     * @param timestampResolution
     * @return a list of buckets of files. The list is ordered such that the files with newest timestamps come first.
     *         Each bucket is also a list of files ordered from newest to oldest.
     */
    @VisibleForTesting
    static HashMultimap getBuckets(Iterable<SSTableReader> files, TimeUnit sstableWindowUnit, int sstableWindowSize, TimeUnit timestampResolution)
    {
        HashMultimap<Long, SSTableReader> buckets = HashMultimap.create();

        // Create hash map to represent buckets
        // For each sstable, add sstable to the time bucket
        // Where the bucket is the file's max timestamp rounded to the nearest window bucket
        for (SSTableReader f : files)
        {
            long tStamp = f.getMaxTimestamp();
            if (timestampResolution.equals(TimeUnit.MICROSECONDS))
                tStamp = tStamp / 1000L;
            else if (timestampResolution.equals(TimeUnit.NANOSECONDS))
                tStamp = tStamp / 1000000L;
            else if (timestampResolution.equals(TimeUnit.SECONDS))
                tStamp = tStamp * 1000L;
            else
                assert TimeWindowCompactionStrategyOptions.validTimestampTimeUnits.contains(timestampResolution);

            Pair<Long,Long> bounds = getWindowBoundsInMillis(sstableWindowUnit, sstableWindowSize, tStamp);
            buckets.put(bounds.left, f );
        }

        logger.debug("buckets {}", buckets);
        return buckets;
    }

    private void updateEstimatedCompactionsByTasks(HashMultimap<Long, SSTableReader> tasks)
    {
        int n = 0;
        long now = System.currentTimeMillis();

        Pair<Long,Long> nowBounds = getWindowBoundsInMillis(options.sstableWindowUnit, options.sstableWindowSize, now);
        now = nowBounds.left;

        for(Long key : tasks.keySet())
        {
            // For current window, make sure it's compactable
            if (key.compareTo(now) >= 0 && tasks.get(key).size() >= cfs.getMinimumCompactionThreshold())
                n++;
            else if (key.compareTo(now) < 0 && tasks.get(key).size() >= 2)
                n++;
        }
        this.estimatedRemainingTasks = n;
    }


    /**
     * @param buckets list of buckets, sorted from newest to oldest, from which to return the newest bucket within thresholds.
     * @param minThreshold minimum number of sstables in a bucket to qualify.
     * @param maxThreshold maximum number of sstables to compact at once (the returned bucket will be trimmed down to this).
     * @return a bucket (list) of sstables to compact.
     */
    @VisibleForTesting
    static List<SSTableReader> newestBucket(HashMultimap<Long, SSTableReader> buckets, int minThreshold, int maxThreshold, TimeUnit sstableWindowUnit, int sstableWindowSize, SizeTieredCompactionStrategyOptions stcsOptions)
    {
        // If the current bucket has at least minThreshold SSTables, choose that one.
        // For any other bucket, at least 2 SSTables is enough.
        // In any case, limit to maxThreshold SSTables.

        long now = System.currentTimeMillis();

        Pair<Long,Long> nowBounds = getWindowBoundsInMillis(sstableWindowUnit, sstableWindowSize, now);
        now = nowBounds.left;

        TreeSet<Long> allKeys = new TreeSet<>(buckets.keySet());

        Iterator<Long> it = allKeys.descendingIterator();
        while(it.hasNext())
        {
            Long key = it.next();
            Set<SSTableReader> bucket = buckets.get(key);
            if (bucket.size() >= minThreshold && key >= now)
            {
                // If we're in the newest bucket, we'll use STCS to prioritize sstables
                List<Pair<SSTableReader,Long>> pairs = SizeTieredCompactionStrategy.createSSTableAndLengthPairs(bucket);
                List<List<SSTableReader>> stcsBuckets = SizeTieredCompactionStrategy.getBuckets(pairs, stcsOptions.bucketHigh, stcsOptions.bucketLow, stcsOptions.minSSTableSize);
                logger.debug("Using STCS compaction for first window of bucket: data files {} , options {}", pairs, stcsOptions);
                List<SSTableReader> stcsInterestingBucket = SizeTieredCompactionStrategy.mostInterestingBucket(stcsBuckets, minThreshold, maxThreshold);
                if (!stcsInterestingBucket.isEmpty())
                    return stcsInterestingBucket;
            }
            else if (bucket.size() >= 2 && key < now)
            {
                logger.debug("bucket size {} >= 2 and not in current bucket, compacting what's here: {}", bucket.size(), bucket);
                return trimToThreshold(bucket, maxThreshold);
            }
            else
            {
                logger.debug("No compaction necessary for bucket size {} , key {}, now {}", bucket.size(), key, now);
            }
        }

        return Collections.<SSTableReader>emptyList();
    }

    /**
     * @param bucket set of sstables
     * @param maxThreshold maximum number of sstables in a single compaction task.
     * @return A bucket trimmed to the maxThreshold newest sstables.
     */
    @VisibleForTesting
    static List<SSTableReader> trimToThreshold(Set<SSTableReader> bucket, int maxThreshold)
    {
        List<SSTableReader> ssTableReaders = new ArrayList<>(bucket);

        // Trim the largest sstables off the end to meet the maxThreshold
        Collections.sort(ssTableReaders, new SSTableReader.SizeComparator());

        return ImmutableList.copyOf(Iterables.limit(ssTableReaders, maxThreshold));
    }

    @Override
    @SuppressWarnings("resource")
    public synchronized Collection<AbstractCompactionTask> getMaximalTask(int gcBefore, boolean splitOutput)
    {
        Iterable<SSTableReader> filteredSSTables = filterSuspectSSTables(sstables);
        if (Iterables.isEmpty(filteredSSTables))
            return null;
        LifecycleTransaction txn = cfs.getTracker().tryModify(filteredSSTables, OperationType.COMPACTION);
        if (txn == null)
            return null;
        return Collections.<AbstractCompactionTask>singleton(new CompactionTask(cfs, txn, gcBefore, false));
    }

    @Override
    @SuppressWarnings("resource")
    public synchronized AbstractCompactionTask getUserDefinedTask(Collection<SSTableReader> sstables, int gcBefore)
    {
        assert !sstables.isEmpty(); // checked for by CM.submitUserDefined

        LifecycleTransaction modifier = cfs.getTracker().tryModify(sstables, OperationType.COMPACTION);
        if (modifier == null)
        {
            logger.debug("Unable to mark {} for compaction; probably a background compaction got to it first.  You can disable background compactions temporarily if this is a problem", sstables);
            return null;
        }

        return new CompactionTask(cfs, modifier, gcBefore, false).setUserDefined(true);
    }

    public int getEstimatedRemainingTasks()
    {
        return this.estimatedRemainingTasks;
    }

    public long getMaxSSTableBytes()
    {
        return Long.MAX_VALUE;
    }


    public static Map<String, String> validateOptions(Map<String, String> options) throws ConfigurationException
    {
        Map<String, String> uncheckedOptions = AbstractCompactionStrategy.validateOptions(options);
        uncheckedOptions = TimeWindowCompactionStrategyOptions.validateOptions(options, uncheckedOptions);

        uncheckedOptions.remove(CFPropDefs.KW_MINCOMPACTIONTHRESHOLD);
        uncheckedOptions.remove(CFPropDefs.KW_MAXCOMPACTIONTHRESHOLD);

        return uncheckedOptions;
    }

    public String toString()
    {
        return String.format("TimeWindowCompactionStrategy[%s/%s]",
                             cfs.getMinimumCompactionThreshold(),
                             cfs.getMaximumCompactionThreshold());
    }
}