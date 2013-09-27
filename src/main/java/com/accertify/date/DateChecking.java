package com.accertify.date;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: mrose
 * Date: 9/20/13
 * Time: 11:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class DateChecking {
    protected static transient Log log = LogFactory.getLog(DateChecking.class);

    @Test
    public void dateStuff() {
        final DateTimeFormatter dtf =  DateTimeFormat.forPattern("yyyyMMdd");
        {
            DateTime dt = dtf.parseDateTime("12341122");
            long ts = dt.getMillis();
            log.warn(dt + " => " + ts);
        }
        {
            DateTime dt = dtf.parseDateTime("67891122");
            long ts = dt.getMillis();
            log.warn(dt + " => " + ts);
        }
    }

    @Test
    public void math() {
        double DEFAULT_MAX_RATE = Runtime.getRuntime().availableProcessors() + Runtime.getRuntime().availableProcessors()*.5;

        log.warn("1: " + Runtime.getRuntime().availableProcessors());
        log.warn("2: " + DEFAULT_MAX_RATE);
    }
}
