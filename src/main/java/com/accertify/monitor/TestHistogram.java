package com.accertify.monitor;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.Assert;
import org.junit.Test;
import java.io.InputStream;
import java.net.URL;
import java.util.TreeSet;

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

    public long[] generateEndPointList(long resolution, long max) {
        TreeSet<Long> ts = new TreeSet<>();

        // .1 ms resolution from .1 ms => 45 ms
        for (long i = resolution; i <= max; i += resolution) {
            ts.add(i);
        }
        if (log.isDebugEnabled()) {
            log.debug("Size: " + ts.size());
            log.debug(StringUtils.join(ts, ", "));
        }
        long[] values = new long[ts.size()];
        int idx = 0;
        for (Long x : ts) {
            values[idx++] = x;
        }
        return values;
    }

    @Test
    public void testConcurrentEndpoint() {
        long[] segments = generateEndPointList(10000, 45*1000000);
        ConcurrentHistogram ch = new ConcurrentHistogram(segments);
        Histogram h = new Histogram(segments);
        for (int i = 0; i < 100000; i++) {
            long s1 = System.nanoTime();
            ping();
            long s2 = System.nanoTime();
            ch.addObservation(s2 - s1);
            h.addObservation(s2 - s1);
            if (log.isTraceEnabled()) {
                log.trace("" + (s2 - s1));
            }
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                ;
//            }
        }

        log.warn("Mean: " + ch.getMean() + " : " + h.getMean());
        log.warn("50%: " + ch.getUpperBoundForFactor(.50) + " : " + h.getUpperBoundForFactor(.50));
        log.warn("75%: " + ch.getUpperBoundForFactor(.75) + " : " + h.getUpperBoundForFactor(.75));
        log.warn("90%: " + ch.getUpperBoundForFactor(.90) + " : " + h.getUpperBoundForFactor(.90));
        log.warn("95%: " + ch.getUpperBoundForFactor(.95) + " : " + h.getUpperBoundForFactor(.95));
        log.warn("99%: " + ch.getUpperBoundForFactor(.99) + " : " + h.getUpperBoundForFactor(.99));
        log.warn("99.9%: " + ch.getUpperBoundForFactor(.999) + " : " + h.getUpperBoundForFactor(.999));
    }

    private static boolean ping() {
        try {
            URL u = new URL("http://localhost:8081/service/echo");
            try(InputStream is = u.openStream()) {
                String response = StringUtils.strip(IOUtils.toString(is));
                if (log.isTraceEnabled()) {
                    log.trace(response);
                }
            }
            return true;
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return false;
    }
}
