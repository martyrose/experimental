package com.accertify.socket;

import com.accertify.crypt.BCrypt;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 10/20/11
 * Time: 7:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class MonitorCPU {
    public static final long init = System.currentTimeMillis();
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss.SSS");

    public static final Long INTERVAL = 1000l;
    public static final Long MILLIS_PER_SECOND = 1000l;
    public static final Long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60l;
    public static final Long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60l;

    public static void main(String[] args) {
        for(int i=0;i<10000;i++) {
            BCrypt.hashpw("abcdefg", BCrypt.gensalt(4));
            if( i % 100 == 0) {
                System.out.print(".");
            }
        }
        System.out.println();
        while(true) {
            long start = System.currentTimeMillis();
            doWork();
            long end = System.currentTimeMillis();

            System.out.println(sdf.format(new Date()) + "|" + (end-start));

            long sleep = INTERVAL - (end % INTERVAL);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static void doWork() {

        try {
            for (int i = 0; i < 5; i++) {
                BCrypt.hashpw("abcdefg", BCrypt.gensalt(10));
            }
        } catch(Throwable t) {
            log(t);
        }
    }

    public static final void log(String s) {
        System.out.println(sdf.format(new java.util.Date()) + " (" + formatDurationHMS(System.currentTimeMillis()-init) + ") --- " + s);
    }
    public static final void log(Throwable e) {
        System.out.print(sdf.format(new java.util.Date()) + ": ex : ");
        e.printStackTrace(System.out);
    }

    public static final String lpad(long l, int pad, char c) {
        String s = "" + l;
        while(s.length() < pad) {
            s = c + s;
        }
        return s;
    }

    public static final String formatDurationHMS(long l) {
        long balance = l;
        long hours = 0;
        long minutes = 0;
        long seconds = 0;
        long millis = 0;
        if( balance / MILLIS_PER_HOUR > 0) {
            hours = balance / MILLIS_PER_HOUR;
            balance = balance - hours*MILLIS_PER_HOUR;
        }

        if( balance / MILLIS_PER_MINUTE > 0) {
            minutes = balance / MILLIS_PER_MINUTE;
            balance = balance - minutes*MILLIS_PER_MINUTE;
        }

        if( balance / MILLIS_PER_SECOND > 0) {
            seconds = balance / MILLIS_PER_SECOND;
            balance = balance - seconds*MILLIS_PER_SECOND;
        }

        millis = balance;

        return lpad(hours, 2, '0') + ':' + lpad(minutes, 2, '0') + ':' + lpad(seconds, 2, '0') + '.' + lpad(millis, 3, '0');
    }

    static class MutableLong {
        Long value = null;

        MutableLong(Long l) {
            value = l;
        }
    }

}
