package com.accertify.monitor;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;

/**
 * Update the Histogram
 */
public class TestHistogram {
    protected static transient Log log = LogFactory.getLog(Histogram.class);

    @Test
    public void testHistogramsSingleThread() {
        long[] trackPoints = new long[] {1,10, 250, 750, 1000, 5000};
        long[] datum = new long[100000000];

        RandomDataGenerator generator = new RandomDataGenerator();
        for( int i=0; i<datum.length; i++) {
            datum[i] = generator.nextLong(0, 900);
        }

        DisruptorHistogram h1 = new DisruptorHistogram(trackPoints);
        Histogram h2 = new Histogram(trackPoints);
        ConcurrentHistogram h3 = new ConcurrentHistogram(trackPoints);

        long s1 = System.currentTimeMillis();
        for( int i=0; i<datum.length; i++) {
            h1.addObservation(datum[i]);
            h2.addObservation(datum[i]);
            h3.addObservation(datum[i]);
        }
        long s2 = System.currentTimeMillis();

        log.warn("Took: " + DurationFormatUtils.formatDurationHMS(s2-s1));

        Assert.assertTrue(h1.getMean().equals(h2.getMean()));
        Assert.assertTrue(h2.getMean().equals(h3.getMean()));

        Assert.assertTrue(h1.getMin() == h2.getMin());
        Assert.assertTrue(h2.getMin() == h3.getMin());

        Assert.assertTrue(h1.getMax() == h2.getMax());
        Assert.assertTrue(h2.getMax() == h3.getMax());

        double[] points = new double[] {.6, .75, .8, .9, .95, .99};
        for( int i=0; i<points.length; i++ ) {
            Assert.assertTrue(h1.getUpperBoundForFactor(points[i]) == h2.getUpperBoundForFactor(points[i]));
            Assert.assertTrue(h2.getUpperBoundForFactor(points[i]) == h3.getUpperBoundForFactor(points[i]));
        }
    }



}
