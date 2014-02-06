package com.accertify.dumb;

import com.accertify.random.MersenneTwisterFast;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * User: mrose
 * Date: 2/3/14
 * Time: 5:04 PM
 * <p/>
 * Comments
 */
public class LoadupResolutionHistory {
    private static final Logger log = LoggerFactory.getLogger(LoadupResolutionHistory.class);

    private static final String JDBC_URL = "jdbc:oracle:thin:@10.12.17.123:1521:XE";
    private static final String JDBC_USER = "core";
    private static final String JDBC_PASS = "core";

    private static Connection c;

    public static void main(String[] args) {
        try {
            c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
            c.setAutoCommit(false);

            MersenneTwisterFast twister = new MersenneTwisterFast(System.currentTimeMillis() ^ System.identityHashCode(c));

            PreparedStatement ps = c.prepareStatement("insert into resolution_history(id, sys_id, virtual_table_id, data_header_id, established, established_by) values(?,?,?,?,?,?)");
            for( int i=0; i<500; i++ ) {
                ps.clearBatch();
                ps.clearParameters();
                for(int j=0; j<1000; j++ ) {
                    ps.setLong(1, twister.nextLong(Long.MAX_VALUE));
                    ps.setInt(2, twister.nextInt(Byte.MAX_VALUE));
                    ps.setLong(3, twister.nextLong(Byte.MAX_VALUE));
                    ps.setLong(4, twister.nextLong(Integer.MAX_VALUE));
                    ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                    ps.setString(6, RandomStringUtils.randomAlphabetic(6));
                    ps.addBatch();
                }
                ps.executeBatch();
                c.commit();
                log("" + i);
            }

        } catch (Throwable t) {
            log(t);
        }
    }
















    // ***************************************************************************************
    // Below the line
    // ***************************************************************************************
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss.SSS");

    public static final void log(Object o) {
        log(String.valueOf(o));
    }

    public static final void log(String s) {
        System.out.println(sdf.format(System.currentTimeMillis()) + " (" + formatDurationHMS(System.currentTimeMillis() - init) + ") --- " + s);
    }

    public static final void log(Throwable e) {
        System.out.print(sdf.format(System.currentTimeMillis()) + ": ex : ");
        e.printStackTrace(System.err);
    }

    public static final void flush() {
        System.out.flush();
    }

    public static final String lpad(long l, int pad, char c) {
        String s = "" + l;
        while (s.length() < pad) {
            s = c + s;
        }
        return s;
    }

    public static final long init = System.currentTimeMillis();
    public static final Long MILLIS_PER_SECOND = 1000l;
    public static final Long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60l;
    public static final Long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60l;

    public static final String formatDurationHMS(long l) {
        long balance = l;
        long hours = 0;
        long minutes = 0;
        long seconds = 0;
        long millis = 0;
        if (balance / MILLIS_PER_HOUR > 0) {
            hours = balance / MILLIS_PER_HOUR;
            balance = balance - hours * MILLIS_PER_HOUR;
        }

        if (balance / MILLIS_PER_MINUTE > 0) {
            minutes = balance / MILLIS_PER_MINUTE;
            balance = balance - minutes * MILLIS_PER_MINUTE;
        }

        if (balance / MILLIS_PER_SECOND > 0) {
            seconds = balance / MILLIS_PER_SECOND;
            balance = balance - seconds * MILLIS_PER_SECOND;
        }

        millis = balance;

        return lpad(hours, 2, '0') + ':' + lpad(minutes, 2, '0') + ':' + lpad(seconds, 2, '0') + '.' + lpad(millis, 3, '0');
    }



}
