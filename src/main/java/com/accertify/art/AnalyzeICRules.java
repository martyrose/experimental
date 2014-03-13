package com.accertify.art;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.*;

/**
 ic_rulez
 */
public class AnalyzeICRules {
    private static final Logger log = LoggerFactory.getLogger(LoadRuleData.class);

    private static final String SQL = "select tripped2 from art_model where r_fraud = false";  // and import < to_date('2014-01-01', 'YYYY-MM-DD')
//    private static final String SQL = "select tripped2 from art_model where r_fraud = false";  // and import < to_date('2014-01-01', 'YYYY-MM-DD')

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            Map<Long, String> ruleData = getRuleData(conn);
            // left => count
            // middle => score
            // right => lift
            final Map<Long, Triple<MutableInt, MutableInt, MutableLong>> tripData = getICTrippedOnFraud(conn);

            List<Long> rulez = new ArrayList<>(tripData.keySet());
            Collections.sort(rulez, new Comparator<Long>() {
                @Override
                public int compare(Long o1, Long o2) {
                    return tripData.get(o1).getRight().toLong().compareTo(tripData.get(o2).getRight().toLong());
                }
            });

            for(Long rid: rulez) {
                log.info("Rule=" + rid +
                        " count=" + tripData.get(rid).getLeft().toInteger() +
                        " score=" + tripData.get(rid).getMiddle().toInteger() +
                        " liftOrDrag=" + tripData.get(rid).getRight().toLong() +
                        " name=" + ruleData.get(rid));
            }
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private static Map<Long, String> getRuleData(Connection conn) throws SQLException {
        Map<Long, String> results = new HashMap<>();
        try(Statement st = conn.createStatement()) {
            st.setFetchSize(100);
            try(ResultSet rs = st.executeQuery("select id, rule_name from ic_rulez")) {
                while(rs.next()) {
                    Long id = rs.getLong(1);
                    String n = rs.getString(2);

                    results.put(id, n);
                }
            }
        }
        return results;
    }

    private static Map<Long, Triple<MutableInt, MutableInt, MutableLong>> getICTrippedOnFraud(Connection conn) throws SQLException {
        Map<Long, Triple<MutableInt, MutableInt, MutableLong>> results = new HashMap<>();
        try(Statement st = conn.createStatement()) {
            st.setFetchSize(100);
            try(ResultSet rs = st.executeQuery(SQL)) {
                while(rs.next()) {
                    String n = rs.getString(1);
                    String[] pairs = StringUtils.split(n, ';');
                    for(String pair:pairs) {
                        String[] sequence = StringUtils.split(pair, ':');
                        Long id = Long.parseLong(sequence[0]);
                        Integer score = Integer.parseInt(sequence[1]);
                        if(!results.containsKey(id)) {
                            Triple<MutableInt, MutableInt, MutableLong> p = new ImmutableTriple<>(new MutableInt(0), new MutableInt(0), new MutableLong(0));
                            results.put(id, p);
                        }
                        results.get(id).getLeft().increment();
                        results.get(id).getMiddle().setValue(score);
                        results.get(id).getRight().add(score);
                    }
                }
            }
        }
        return results;
    }
    /*
select (count(nullif(fraud, false))::real/count(1)::real)*100
from art_model
where tripped2 like '%5237260000000099621%'

     */
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
