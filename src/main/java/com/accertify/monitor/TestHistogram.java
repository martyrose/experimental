package com.accertify.monitor;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
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
    public void testSimple1() {
        Histogram h = new Histogram(new long[] {10,100,1000});
        h.addObservation(25);
        h.addObservation(50);
        h.addObservation(60);

        log.warn(h.toString());
    }

    @Test
    public void emitShardsInfo() {
        NumberFormat nf5 = NumberFormat.getIntegerInstance();

        nf5.setMaximumIntegerDigits(5);
        nf5.setMinimumIntegerDigits(5);
        nf5.setGroupingUsed(false);
        try {
            File f1 = new File("/tmp/shards.txt");
            FileUtils.deleteQuietly(f1);

            String s = "insert into profile_tracking_shards(shard_hash, service_name, active, modified) values ({0}, ''{1}'', 1, current_timestamp);" + SystemUtils.LINE_SEPARATOR;
            for(int i=0; i<65536; i++) {
                FileUtils.write(f1, MessageFormat.format(s, String.valueOf(i), "profileslv" + nf5.format(i % 32)), true);
            }

            s = "update profile_tracking_svcs set service_name=''{0}'' where table_name=''{1}'';" + SystemUtils.LINE_SEPARATOR;
            for(int i=0; i<32; i++ ) {
                FileUtils.write(f1, MessageFormat.format(s, "profileslv" + nf5.format(i % 32), "profile_tracking_" + nf5.format(i % 32)), true);
            }
        } catch (IOException e) {
            log.warn(e, e.getMessage());
        }
    }
}
