/*
 * Copyright 2011 LMAX Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.accertify.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Histogram for tracking the frequency of observations of values below interval upper bounds.<p/>
 *
 * This class is useful for recording timings across a large number of observations
 * when high performance is required.<p/>
 *
 * The interval bounds are used to define the ranges of the histogram buckets. If provided bounds
 * are [10,20,30,40,50] then there will be five buckets, accessible by index 0-4. Any value
 * 0-10 will fall into the first interval bar, values 11-20 will fall into the
 * second bar, and so on.
 */
public final class ConcurrentHistogram
{
    private static final Logger log = LoggerFactory.getLogger(ConcurrentHistogram.class);

    // tracks the upper intervals of each of the buckets/bars
    private final long[] upperBounds;
    // tracks the count of the corresponding bucket
    private final AtomicLongArray counts;
    // minimum value so far observed
    private final AtomicLong minValue = new AtomicLong(Long.MAX_VALUE);
    // maximum value so far observed
    private final AtomicLong maxValue = new AtomicLong(Long.MIN_VALUE);

    // Used to efficiently and accurately track the mean
    private final AtomicLong sumObservations = new AtomicLong(0);
    private final AtomicLong count = new AtomicLong(0);

    /**
     * Create a new Histogram with a provided list of interval bounds.
     *
     * @param upperBounds of the intervals. Bounds must be provided in order least to greatest, and
     * lowest bound must be greater than or equal to 1.
     * @throws IllegalArgumentException if any of the upper bounds are less than or equal to zero
     * @throws IllegalArgumentException if the bounds are not in order, least to greatest
     */
    public ConcurrentHistogram(final long[] upperBounds)
    {
        validateBounds(upperBounds);

        this.upperBounds = Arrays.copyOf(upperBounds, upperBounds.length);
        this.counts = new AtomicLongArray(upperBounds.length);
    }

    /**
     * Validates the input bounds; used by constructor only.
     */
    private void validateBounds(final long[] upperBounds)
    {
        long lastBound = -1L;
        if (upperBounds.length <= 0) {
            throw new IllegalArgumentException("Must provide at least one interval");
        }
        for (final long bound : upperBounds)
        {
            if (bound <= 0L)
            {
                throw new IllegalArgumentException("Bounds must be positive values");
            }

            if (bound <= lastBound)
            {
                throw new IllegalArgumentException("bound " + bound + " is not greater than " + lastBound);
            }

            lastBound = bound;
        }
    }


    /**
     * Add an observation to the histogram and increment the counter for the interval it matches.
     *
     * @param value for the observation to be added.
     * @return return true if in the range of intervals and successfully added observation; otherwise false.
     */
    public boolean addObservation(final long value)
    {
        int low = 0;
        int high = upperBounds.length - 1;

        // do a classic binary search to find the high value
        while (low < high)
        {
            int mid = low + ((high - low) >> 1);
            if (upperBounds[mid] < value)
            {
                low = mid + 1;
            }
            else
            {
                high = mid;
            }
        }

        // if the binary search found an eligible bucket, increment
        if (value <= upperBounds[high])
        {
            counts.getAndIncrement(high);
            trackRange(value);

            return true;
        }

        // otherwise value was not found
        return false;
    }

    /**
     * Track minimum and maximum observations
     *
     * @see getMin
     * @see getMax
     */
    private void trackRange(final long value)
    {
        {
            count.incrementAndGet();
            sumObservations.addAndGet(value);
        }
        {
            long currentMinValue;
            while (value < (currentMinValue = minValue.get())) {
                if (minValue.compareAndSet(currentMinValue, value)) {
                    break;
                }
            }
        }
        {
            long currentMaxValue;
            while (value > (currentMaxValue = maxValue.get())) {
                if (maxValue.compareAndSet(currentMaxValue, value)) {
                    break;
                }
            }
        }
    }

    /**
     * Add observations from another Histogram into this one.<p/>
     *
     * Histograms must have the same intervals.
     *
     * @param histogram from which to add the observation counts.
     * @throws IllegalArgumentException if interval count or values do not match exactly
     */
    public void addObservations(final ConcurrentHistogram histogram)
    {
        // validate the intervals
        if (upperBounds.length != histogram.upperBounds.length)
        {
            throw new IllegalArgumentException("Histograms must have matching intervals");
        }

        for (int i = 0, size = upperBounds.length; i < size; i++)
        {
            if (upperBounds[i] != histogram.upperBounds[i])
            {
                throw new IllegalArgumentException("Histograms must have matching intervals");
            }
        }

        // increment all of the internal counts
        for (int i = 0, size = counts.length(); i < size; i++)
        {
            counts.getAndAdd(i, histogram.counts.get(i));
        }

        // refresh the minimum and maximum observation ranges
        this.count.set(histogram.count.get());
        this.sumObservations.set(histogram.sumObservations.get());
        this.minValue.set(histogram.minValue.get());
        this.maxValue.set(histogram.maxValue.get());
    }

    /**
     * Clear the list of interval counters
     */
    public void clear()
    {
        maxValue.set(0L);
        minValue.set(Long.MAX_VALUE);
        count.set(0l);
        sumObservations.set(0l);

        for (int i = 0, size = counts.length(); i < size; i++)
        {
            counts.set(i, 0l);
        }
    }

    /**
     * Count total number of recorded observations.
     *
     * @return the total number of recorded observations.
     */
    public long getCount()
    {
        return count.longValue();
    }

    /**
     * Get the minimum observed value.
     *
     * @return the minimum value observed.
     */
    public long getMin()
    {
        return minValue.get();
    }

    /**
     * Get the maximum observed value.
     *
     * @return the maximum of the observed values;
     */
    public long getMax()
    {
        return maxValue.get();
    }

    /**
     * Calculate the mean of all recorded observations.<p/>
     *
     * The mean is calculated by summing the mid points of each interval multiplied by the count
     * for that interval, then dividing by the total count of observations.  The max and min are
     * considered for adjusting the top and bottom bin when calculating the mid point, this
     * minimises skew if the observed values are very far away from the possible histogram values.
     *
     * @return the mean of all recorded observations.
     */
    public BigDecimal getMean()
    {
        // early exit to avoid divide by zero later
        if (0L == getCount())
        {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(sumObservations.longValue()).divide(new BigDecimal(getCount()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Get the interval upper bound for a given factor of the observation population.<p/>
     *
     * Note this does not get the actual percentile measurement, it only gets the bucket
     *
     * @param factor representing the size of the population.
     * @return the interval upper bound.
     * @throws IllegalArgumentException if factor &lt; 0.0 or factor &gt; 1.0
     */
    public long getUpperBoundForFactor(final double factor)
    {
        if (0.0d >= factor || factor >= 1.0d)
        {
            throw new IllegalArgumentException("factor must be >= 0.0 and <= 1.0");
        }

        final long totalCount = getCount();
        final long tailTotal = totalCount - Math.round(totalCount * factor);
        long tailCount = 0L;

        // reverse search the intervals ('tailCount' from end)
        for (int i = counts.length() - 1; i >= 0; i--)
        {
            if (0L != counts.get(i))
            {
                tailCount += counts.get(i);
                if (tailCount >= tailTotal)
                {
                    return upperBounds[i];
                }
            }
        }

        return 0L;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Histogram{");

        sb.append("min=").append(getMin()).append(", ");
        sb.append("max=").append(getMax()).append(", ");
        sb.append("mean=").append(getMean()).append(", ");

        sb.append('[');
        for (int i = 0, size = counts.length(); i < size; i++)
        {
            sb.append(upperBounds[i]).append('=').append(counts.get(i)).append(", ");
        }

        if (counts.length() > 0)
        {
            sb.setLength(sb.length() - 2);
        }
        sb.append(']');

        sb.append('}');

        return sb.toString();
    }
}
