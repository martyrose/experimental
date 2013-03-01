package com.accertify.monitor;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.junit.Test;

/**
 * Update the Histogram
 */
public class TestHistogram {
    protected static transient Log log = LogFactory.getLog(Histogram.class);

    @Test
    public void testSimple1() {
        Histogram h = new Histogram(new long[] {10,100,1000});
        h.addObservation(25);
        h.addObservation(50);
        h.addObservation(60);

        log.warn(h.toString());
    }
}
