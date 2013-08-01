package com.accertify.monitor;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.Assert;
import org.junit.Test;

import javax.net.SocketFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.text.MessageFormat;
import java.text.NumberFormat;
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

    @Test
    public void generateEndPointList() {
        TreeSet<Long> ts = new TreeSet<>();
        for(int i=0;i<30;i++) {
            long l = 1 << i;
            ts.add(l);
        }

        log.warn(StringUtils.join(ts, ", "));
    }

        @Test
    public void testConcurrentEndpoint() {
        long[] segments = new long[30];
        for(int i=0;i<30;i++) {
            segments[i] = 1 << i;
        }
        ConcurrentHistogram ch = new ConcurrentHistogram(segments);
        Histogram h = new Histogram(segments);
        for (int i = 0; i < 1000; i++) {
            long s1 = System.nanoTime();
            ping();
            long s2 = System.nanoTime();
            ch.addObservation(s2-s1);
            h.addObservation(s2-s1);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                ;
            }
        }

        log.warn("Mean: " + ch.getMean() + " : " + h.getMean());
        log.warn("75%: " + ch.getUpperBoundForFactor(.75) + " : " + h.getUpperBoundForFactor(.75));
        log.warn("90%: " + ch.getUpperBoundForFactor(.90) + " : " + h.getUpperBoundForFactor(.90));
        log.warn("95%: " + ch.getUpperBoundForFactor(.95) + " : " + h.getUpperBoundForFactor(.95));
        log.warn("99%: " + ch.getUpperBoundForFactor(.99) + " : " + h.getUpperBoundForFactor(.99));

    }

    private static boolean ping() {
        try {
            URL u = new URL("http://localhost:8081/service/echo");
            try(InputStream is = u.openStream()) {
                log.warn(StringUtils.strip(IOUtils.toString(is)));
            }
            return true;
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return false;
    }
}
