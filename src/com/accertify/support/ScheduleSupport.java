package com.accertify.support;

import com.accertify.prime.MersenneTwisterFast;
import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 */
public class ScheduleSupport {
    protected static transient Log log = LogFactory.getLog(ScheduleSupport.class);



    public static void main(String[] args) {
        List<String> PEEPS = new ArrayList<>();
        PEEPS.add("Stephan");
        PEEPS.add("Eric");
        PEEPS.add("Marty");
        PEEPS.add("Nabeel");
        PEEPS.add("Andy");
        String LAST_PRIMARY = "Eric";
        String LAST_SECONDARY = "Stephan";

        MersenneTwisterFast twister = new MersenneTwisterFast(System.currentTimeMillis());

        DateTime now = DateTime.now();
        while( now.getDayOfWeek() != DateTimeConstants.MONDAY ) {
            now = now.plusDays(1);
        }

        StringBuilder sb = new StringBuilder();
        DateTimeFormatter f = DateTimeFormat.forPattern("MM/dd/YYYY");
        while (now.getYear() == 2013) {
            now = now.plusDays(7);

            String primary = LAST_PRIMARY;
            while(StringUtils.equals(primary, LAST_PRIMARY) || StringUtils.equals(primary, LAST_SECONDARY)) {
                primary = PEEPS.get(twister.nextInt(PEEPS.size()));
            }

            String secondary = LAST_SECONDARY;
            while( StringUtils.equals(primary, secondary) || StringUtils.equals(primary, LAST_PRIMARY) || StringUtils.equals(secondary, LAST_SECONDARY)) {
                secondary = PEEPS.get(twister.nextInt(PEEPS.size()));
            }
            LAST_PRIMARY = primary;
            LAST_SECONDARY = secondary;


            sb.append(f.print(now) + "^" + primary + "^" + secondary + "|");
        }
        log.warn(sb.toString());
    }
}
