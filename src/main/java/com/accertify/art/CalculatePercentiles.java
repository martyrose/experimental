package com.accertify.art;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 -- Finds 99% of fraud
 select * from art_model where mscore>26
 -- Finds 95% of fraud
 select * from art_model where mscore>95
 -- Why do we look at so few over 600?
 select * from art_model where escore>600
 */
public class CalculatePercentiles {
    private static final Logger log = LoggerFactory.getLogger(CalculatePercentiles.class);

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            List<Integer> scores = getScores(conn, "select mscore from art_model");
            log.warn(String.valueOf(scores));

            log.warn("99P=" + getPercentile(scores, .99));
            log.warn("1P=" + getPercentile(scores, .01));
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private static Integer getPercentile(List<Integer> values, double percentile) {
        if( percentile <= 0.0d || percentile >= 1.0d ) {
            throw new IllegalArgumentException("Must be between 0.0 and 1.0");
        }
        double elementAsDbl = (double)values.size() * percentile;
        int element = (int)Math.round(elementAsDbl);
        element = element == 0 ? 1 : element;
        return values.get(element-1);
    }

    private static List<Integer> getScores(Connection conn, String sql) throws SQLException {
        List<Integer> scores = new ArrayList<>(10000);
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while(rs.next()) {
            scores.add(rs.getInt(1));
        }
        rs.close();
        st.close();
        Collections.sort(scores);
        return scores;
    }

    private static Connection getConnection() {
        try {
            Connection c = DriverManager.getConnection("jdbc:postgresql://autod.pg01/art", "art", "art");
            c.setAutoCommit(false);
            return c;
        } catch (SQLException e) {
            throw new RuntimeException("EVIL-1", e);
        }
    }
}
