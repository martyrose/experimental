package com.accertify.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;

/**
 * Created with IntelliJ IDEA.
 * User: mrose
 * Date: 3/14/13
 * Time: 2:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestShards {
    protected static transient Log log = LogFactory.getLog(TestShards.class);

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
