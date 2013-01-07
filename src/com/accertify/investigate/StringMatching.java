package com.accertify.investigate;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: mrose
 * Date: 1/4/13
 * Time: 10:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class StringMatching {
    protected static transient Log log = LogFactory.getLog(StringMatching.class);

    @Test
    public void test1() {
        String s = "2012-08-30 23:00:00";

        boolean b1 = StringUtils.containsOnly(s, "0123456789-:. ");
        boolean b2 = StringUtils.containsAny(s, "0123456789");
        boolean b3 = countOccurrences(s, '-') == 2;
        boolean b4 = countOccurrences(s, ':') == 2;

        log.warn("hi");
    }


    // Borrowed from StringUtils
    public static int countOccurrences(String haystack, char needle)
    {
        int count = 0;
        for (int i=0; i < haystack.length(); i++)
        {
            if (haystack.charAt(i) == needle)
            {
                 count++;
            }
        }
        return count;
    }
}
