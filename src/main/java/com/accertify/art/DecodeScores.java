package com.accertify.art;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * User: mrose
 * Date: 3/25/14
 * Time: 1:50 PM
 * <p/>
 * Comments
 */
public class DecodeScores {
    private static final Logger log = LoggerFactory.getLogger(DecodeScores.class);


    private static final String INSERT_SQL = "insert into art_model(tid, import, escore, mscore, cost, agent, agent_ts, r, r_ts, r_fraud, r_missed, tripped1, tripped2) values(?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            decodeRules(conn);
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
        }

    }

    private static void decodeRules(Connection conn) throws SQLException, IOException {
        Map<Long, String> rulez=new HashMap<>();
        try(PreparedStatement ps = conn.prepareStatement("select id, rule_name from ic_rulez")) {
            try(ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    rulez.put(rs.getLong(1), rs.getString(2));
                }
            }
        }

        try (
                PreparedStatement ps1 = conn.prepareStatement("select tid, tripped2 from art_model");
                PreparedStatement ps2 = conn.prepareStatement("update art_model set tripped3=? where tid=?")

        ) {
            try(ResultSet rs = ps1.executeQuery()) {
                while (rs.next()) {
                    String tid = rs.getString(1);
                    String rtripped = rs.getString(2);
                    rtripped = translate(rulez, rtripped);
                    ps2.clearParameters();
                    ps2.setString(1, rtripped);
                    ps2.setString(2, tid);
                    ps2.executeUpdate();
                }
            }
            conn.commit();
        }
    }

    private static String translate(Map<Long, String> rulez, String rtripped) {
        String[] parts = StringUtils.split(rtripped, ';');
        List<Pair<String, Integer>> results = new ArrayList<>();
        for(String part:parts) {
            long id = Long.parseLong(StringUtils.substringBefore(part, ":"));
            int score = Integer.parseInt(StringUtils.substringAfter(part, ":"));
            results.add(new ImmutablePair<>(StringUtils.trimToEmpty(rulez.get(id)), score));
        }
        Collections.sort(results, new Comparator<Pair<String, Integer>>() {
            @Override
            public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
                return -1 * o1.getRight().compareTo(o2.getRight());
            }
        });

        StringBuilder sb = new StringBuilder();
        for(Pair<String, Integer> p : results ) {
            sb.append(p.getLeft() + "=>" + p.getRight() + "; ");
        }

        return sb.toString();
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
