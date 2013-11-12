package com.accertify.dumb;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 ResultSet rs = null;
 try {
 int i=1;
 this.binRangeSelectPs.setString(i++, cardnumber);
 this.binRangeSelectPs.setString(i++, cardnumber);
 for(Long l:hChain) {
 this.binRangeSelectPs.setLong(i++, l);
 }
 rs = this.binRangeSelectPs.executeQuery();

 private static final String BIN_RANGE_SELECT_SQL_TEMPLATE
 = "select * from {0} where bin_nbr_low <= ? and bin_nbr_high >= ?"
 + " and hierarchy_id in (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";





 ResultSet rs = null;
 try {
 int i=1;
 this.shortBinRangeSelectPs.setString(i++, cardnumber + "%");
 for(Long l:hChain) {
 this.shortBinRangeSelectPs.setLong(i++, l);
 }
 rs = this.shortBinRangeSelectPs.executeQuery();

 private static final String SHORT_BIN_RANGE_SELECT_SQL_TEMPLATE
 = "select * from {0} where bin_nbr_low like ?"
 + " and hierarchy_id in (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

 */

/**
 select * from bin_ranges where bin_nbr_low <= '47820010003648825' and bin_nbr_high >= '47820010003648825' and hierarchy_id = 5237260000000000001
 IO Cost: 222
 CPU Cost: 1.9M


 CREATE INDEX BIN_RANGES_IDX_2 ON BIN_RANGES (to_number(BIN_NBR_LOW), to_number(BIN_NBR_HIGH));

 select HINT *
 from bin_ranges br where bin_nbr_low <= to_number('5466322638930986') and bin_nbr_high >= to_number('5466322638930986') and hierarchy_id = 5237260000000000001

 IO COST: 22
 CPU COST: .5M


 CREATE INDEX BIN_RANGES_IDX_3 ON BIN_RANGES (to_number(BIN_NBR_LOW), to_number(BIN_NBR_HIGH), hierarchy_id);


 select HINT *
 from bin_ranges br where bin_nbr_low <= to_number('5466322638930986') and bin_nbr_high >= to_number('5466322638930986') and hierarchy_id = 5237260000000000001

 IO COST: 15
 CPU COST: .5M

 */
public class TestBinRange {
    private static final Logger log = LoggerFactory.getLogger(TestBinRange.class);

    public TestBinRange() {
        ;
    }

    @Test
    public void findBadRows() {
        try(Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement("select id, bin_nbr_low, bin_nbr_high from bin_ranges");
            ps.setFetchSize(50);
            ResultSet rs = ps.executeQuery();
            int i=0;
            while(rs.next()) {
                Long id = rs.getLong(1);
                String lo = rs.getString(2);
                String hi = rs.getString(3);

                if(!StringUtils.containsOnly(lo, "0123456789")) {
                    log.warn("Bad Lo: " + id + " => " + lo);
                }
                if(!StringUtils.containsOnly(hi, "0123456789")) {
                    log.warn("Bad Hi: " + id + " => " + hi);
                }
            }
            log.warn("Found: " + i);
        } catch (SQLException ex) {
            throw new RuntimeException("BAD");
        }
    }

    private Connection getConnection() {
        try {
            return DriverManager.getConnection("jdbc:oracle:thin:@10.216.30.40:1521:XE", "core", "core");
        } catch (SQLException e) {
            throw new RuntimeException("EVIL");
        }
    }

}
// delete from bin_ranges where id in (5237260000000213402,5237260000000298319);