package com.accertify.oracle;

import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * User: mrose
 * Date: 4/29/13
 * Time: 2:19 PM
 * <p/>
 * Comments
 */
public class TestDateBounds {
    private static final Logger log = LoggerFactory.getLogger(TestDateBounds.class);

    public TestDateBounds() {
        ;
    }

    @Test
    public void findMin() {
        try( Connection c = DriverManager.getConnection("jdbc:oracle:thin:@accdbd01.accertify.com:1521:dev01", "core", "dev01") ) {
            DateTime dt = DateTime.now();

            try( PreparedStatement ps = c.prepareStatement("select count(1) from properties where established > ?") ) {
                ps.clearParameters();
                ps.setTimestamp(1, new Timestamp(dt.getMillis()));

                while (dt.getYear() < 10000) {
                    try (ResultSet rs = ps.executeQuery()) {
                        log.warn("Success @ " + dt.toString() + " : " + dt.getMillis());
                    } catch (SQLException e) {
                        log.warn("Failed to run query for: " + dt.toString() + " : " + e.getMessage());
                    }
                    dt = dt.plusYears(1);
                }

            } catch(SQLException e ) {
                log.warn("Failed to prepare : " + e.getMessage());
            }
        } catch(SQLException e ) {
            log.warn("Failed to connect : " + e.getMessage());
        }
    }

    protected final DateTimeFormatter df = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss");
    private static int NUM_TABLES = 5;
    private static int MINS_ON_TABLE = 3;

    @Test
    public void logggers() {
        while(true) {
            int secondsIntoToday = ((int)(System.currentTimeMillis() % DateUtils.MILLIS_PER_DAY)) / 1000;
            int minutesIntoToday = secondsIntoToday / 60;
            int tableCounter = minutesIntoToday / MINS_ON_TABLE;

            int queryTable = tableCounter % NUM_TABLES;
            int writeAheadTable = (tableCounter+1) % NUM_TABLES;
            int truncateTable = (tableCounter+NUM_TABLES-2) % NUM_TABLES; // When tableCounter is 0,1; we don't want to go negative; so add something that won't affect the result

            log.warn("now = " + df.print(System.currentTimeMillis()) + "  secondsIntoToday = " + secondsIntoToday + "  minutesIntoToday = " + minutesIntoToday + "  tableCounter = " + tableCounter);
            log.warn("\tqueryTable = " + queryTable + "  writeTables = " + queryTable + "," + writeAheadTable + " truncateTable=" + truncateTable);

            try {
                Thread.sleep(990);
            } catch (InterruptedException e) {
                ;
            }
        }
    }
}
