package com.accertify.dumb;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * User: mrose
 * Date: 10/2/13
 * Time: 12:14 PM
 * <p/>
 * Comments
 */

public class TestRandom {
    private static final Logger log = LoggerFactory.getLogger(TestRandom.class);

    public TestRandom() {
        ;
    }

    @Test
    public void rand() {
        for (int i = 0; i < 1000; i++) {
            int j = ThreadLocalRandom.current().nextInt(0, 1);
            if (j != 0) {
                log.warn("" + j);
            }
        }
    }
}
