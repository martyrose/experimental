package com.accertify.art;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.*;

/**
 create table ic_rulez (
   id numeric,
   rule_name text
 );
 */
public class LoadRuleData {
    private static final Logger log = LoggerFactory.getLogger(LoadRuleData.class);

    private static final String INSERT_SQL = "insert into ic_rulez(id, rule_name) values(?,?)";

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement(INSERT_SQL);

            FileInputStream fis = new FileInputStream(new File("/home/mrose/art/n30uryzmu_2014021115.rule.txt"));
            BufferedReader bufReader = new BufferedReader(new InputStreamReader(fis, Charset.defaultCharset()));

            String line;
            while ((line = bufReader.readLine()) != null) {
                String[] parts = StringUtils.splitPreserveAllTokens(line, (char) 27);

                if (parts.length != 3) {
                    log.warn("Invalid line: " + line);
                    continue;
                }

                ps.setLong(1, Long.parseLong(parts[0]));
                ps.setString(2, parts[1]);
                ps.executeUpdate();
            }
            conn.commit();
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
        }
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
