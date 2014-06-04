package com.mrose.socket;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class MonitorPersistentJDBCEndpoint {
    public static String URL = null;
    public static String USER = null;
    public static String PW = null;

    public static final String QUERY = "select 1 from dual";

    public static final Long MILLIS_PER_SECOND = 1000l;
    public static final Long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60l;
    public static final Long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60l;

    public static final long init = System.currentTimeMillis();
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss.SSS");
    public static final Map<Long, ConnectionWrapper> checks = new HashMap<Long, ConnectionWrapper>();
    public static final SortedSet<Long> intervals = new TreeSet<Long>();

    public static void main(String[] args) {
        if( args.length != 3 ) {
            System.err.println("PROGGIE URL USER PASS");
            return;
        }
        URL = args[0];
        USER = args[1];
        PW = args[2];

        try {
            log("Registering Driver");
            Class clazz = MonitorPersistentJDBCEndpoint.class.getClassLoader().loadClass("oracle.jdbc.OracleDriver");
            DriverManager.registerDriver((Driver)clazz.newInstance());

            intervals.add(1* MILLIS_PER_SECOND);
            intervals.add(5* MILLIS_PER_SECOND);
            intervals.add(30*MILLIS_PER_SECOND);
            intervals.add(MILLIS_PER_MINUTE);
            intervals.add(5*MILLIS_PER_MINUTE);
            intervals.add(10*MILLIS_PER_MINUTE);
            intervals.add(15*MILLIS_PER_MINUTE);
            intervals.add(30*MILLIS_PER_MINUTE);
            intervals.add(45*MILLIS_PER_MINUTE);
            intervals.add(MILLIS_PER_HOUR);
            intervals.add(2*MILLIS_PER_HOUR);
            intervals.add(3*MILLIS_PER_HOUR);
            intervals.add(4*MILLIS_PER_HOUR);
            log("Intervals: " + intervals);

            log("Creating Connections");
            for(Long interval:intervals) {
                checks.put(interval, new ConnectionWrapper(System.currentTimeMillis(), interval));
            }
            log("Startup Complete");
        } catch (Throwable t) {
            log(t.getMessage());
            t.printStackTrace();
        }

        try {
            while(true) {
                Thread.sleep(100);

                for(Long interval:intervals) {
                    testPointInTime(interval);
                }
            }
        } catch (InterruptedException e) {
            log("Caught Interrupted");
        }

    }

    public static final Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PW);
    }

    public static final void testPointInTime(Long interval) {
        ConnectionWrapper cw = checks.get(interval);
        long lastChecked = cw.lastValidChecked;
        long now = System.currentTimeMillis();
        long mustBeenCheckedSince = now - interval;

        if( lastChecked < mustBeenCheckedSince) {
            long start = System.currentTimeMillis();

            boolean valid = checkConnection(cw.conn);
            long end = System.currentTimeMillis();
            if( !valid ) {
                System.out.println("F|" + sdf.format(new Date()) + "|" + formatDurationHMS(interval) + "|" + sdf.format(cw.created) + "|" + sdf.format(cw.lastValidChecked) + "|" + formatDurationHMS(end-start));
                close(cw.conn);
                checks.put(interval, new ConnectionWrapper(System.currentTimeMillis(), interval));
            } else {
                System.out.println("S|" + sdf.format(new Date()) + "|" + formatDurationHMS(interval) + "|" + sdf.format(cw.created) + "|" + sdf.format(cw.lastValidChecked) + "|" + formatDurationHMS(end-start));
                cw.lastValidChecked = System.currentTimeMillis();
            }
        } else {
            // log("Connection for interval " + interval + " had not been checked since " + sdf.format(cw.lastValidChecked) + " is not needing to be checked");
        }
    }

    public static final void log(String s) {
        System.out.println(sdf.format(new java.util.Date()) + " (" + formatDurationHMS(System.currentTimeMillis()-init) + ") --- " + s);
    }
    public static final void log(Exception e) {
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

    public static final boolean checkConnection(Connection c) {
        Statement st = null;
        ResultSet rs = null;

        long start = System.currentTimeMillis();
        try {
            if (c != null && !c.isClosed()) {
                st = c.createStatement();
                st.setQueryTimeout(5);
                rs = st.executeQuery(QUERY);
                if (rs.next()) {
                    return true;
                } else {
                    log("Returned no rows");
                }
            } else {
                log("Connection was closed");
            }
        } catch (Throwable e) {
            log(e.getMessage());
            e.printStackTrace();
        } finally {
            close(st);
            close(rs);
        }
        long end = System.currentTimeMillis();
        log("Check Connection Took: " + formatDurationHMS(end-start));

        return false;
    }

    public static final void close(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException e) {
                ;
            }
        }
    }

    public static final void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                ;
            }
        }
    }

    public static final void close(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (SQLException e) {
                ;
            }
        }
    }
    static class ConnectionWrapper {
        Long interval = -1l;
        Long lastValidChecked = -1l;
        Long created = null;
        Connection conn = null;

        ConnectionWrapper(Long lastChecked, Long interval) {
            try {
                this.conn = getConnection();
                this.lastValidChecked = lastChecked;
                this.interval = interval;
                this.created = System.currentTimeMillis();
                log("Connection for interval " + formatDurationHMS(interval) + " created @ [" + sdf.format(created) + "]");
            } catch (SQLException e) {
                log("Unable to retrieve connection : " + e.getMessage());
            }
        }
    }
}

