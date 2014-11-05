package com.mrose.financial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

/**
 insert into events(key, value, ts) values('SALLY_CALI_GIRLS', null, to_date('2014.02.07', 'YYYY.MM.DD'))
 insert into events(key, value, ts) values('MARTY_VEGAS_ACC', null, to_date('2014.03.14', 'YYYY.MM.DD'))
 insert into events(key, value, ts) values('SPRING_WATER_2014', null, to_date('2014.03.21', 'YYYY.MM.DD'))

 insert into categories(key, value, budget) values('Cash', null, null)
 */
public class LoadCategoryData {
    private static final Logger log = LoggerFactory.getLogger(LoadCategoryData.class);

    private static final String JDBC_URL = "jdbc:postgresql://10.216.30.64:5432/mrose";
    private static final String JDBC_USER = "mrose";
    private static final String JDBC_PASS = "mrose";

    public LoadCategoryData() {
        ;
    }

    public static void main(String[] args) {
        try {
            Connection c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
            c.setAutoCommit(false);
            PreparedStatement ps = c.prepareStatement("insert into categories(key) values(?)");

            for(String v : VALUES) {
                ps.setString(1, v.toUpperCase(Locale.getDefault()));
                ps.addBatch();
            }
            ps.executeBatch();
            c.commit();
        } catch(SQLException ex) {
            log.warn(ex.getMessage(), ex);
        }
    }

    private static String[] VALUES = new String[] {
            "Insurance",
            "Mortgage",
            "Condo",
            "HomeOp",
            "CTA",
            "DIRECTV",
            "Tuition",
            "Storage",
            "CAR",
            "MEDICAL",
            "CHILDCARE",
            "EMERGENCY",
            "KIDS",
            "TRAVEL",
            "GIFT",
            "HAIRCUT",
            "TAXES",
            "GROCERY",
            "BIGBOX",
            "SAVING",
            "ENTERTAIN",
            "MISC",
            "NET",
            "NVI",
            "CHARITY",
            "MARTYSP",
            "SALLYSP",
            "ACC"
    };
}
