package com.accertify.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * User: mrose
 * Date: 7/25/13
 * Time: 1:02 PM
 * <p/>
 * Comments
 */
public class TestBigDecimalFormat {
    private static final Logger log = LoggerFactory.getLogger(TestBigDecimalFormat.class);

    public TestBigDecimalFormat() {
        ;
    }
    private static final BigDecimal MIN_VALUE = new BigDecimal(Long.MIN_VALUE);
    private static final BigDecimal MAX_VALUE = new BigDecimal(Long.MAX_VALUE);

    @Test
    public void testSee1() {
        BigDecimal bd = new BigDecimal("836.00");
        log.warn(formatNumber(bd));
    }

    public String formatNumber(BigDecimal v) {
        if (v != null) {
            // Do some bounds checking so we don't write out something
            // too negative or too positive
            if( v.compareTo(MIN_VALUE) < 0 ) {
                v = MIN_VALUE;
            }
            if( v.compareTo(MAX_VALUE) > 0) {
                v = MAX_VALUE;
            }
            if( v.compareTo(BigDecimal.ZERO) == 0 ) {
                return "0";
            }
            // So at this point we know that it is a sane value and it is NOT ZERO

            // Set the scale that we are interested in
            v = v.setScale(MathContext.DECIMAL128.getPrecision(), MathContext.DECIMAL128.getRoundingMode());
            // Now convert it to a string an wax off the 0's from the left and right
            String s = v.toPlainString();

            // If the value has a decimal point; remove any zero's hanging off the right hand side
            if (StringUtils.contains(s, ".")) {
                s = StringUtils.stripEnd(s, "0");
            }
            // If the last character is a period, it is insignificant and can be chopped off
            if( StringUtils.endsWith(s, ".")) {
                s = StringUtils.stripEnd(s, ".");
            }
            return s;
        }
        return "0";
    }
}
