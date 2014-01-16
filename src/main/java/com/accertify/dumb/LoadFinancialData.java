package com.accertify.dumb;

import com.google.common.base.Objects;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.*;
import java.util.List;
import java.util.UUID;

/**
 CREATE TABLE
     tran_log
     (
         id varchar2(64),
         tran_date timestamp(6),
         category varchar2(32),
         detail varchar2(512),
         amount number(10,2)
 )

 */
public class LoadFinancialData {
    private static final Logger log = LoggerFactory.getLogger(LoadFinancialData.class);

    private static Connection c;
    private static PreparedStatement ps;
    private static DateTimeFormatter dtf;

    public static void main(String[] args) {
        try {
            c = DriverManager.getConnection("jdbc:oracle:thin:@10.12.17.123:1521:XE", "core", "core");
            c.setAutoCommit(false);
            truncateTable();
            ps = c.prepareStatement("insert into tran_log(id, tran_date, category, detail, amount) values(?,?,?,?,?)");

            // 1/04/2012
            // http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
            dtf = DateTimeFormat.forPattern("MM/dd/yyyy");
            List<String> linez = Files.readAllLines(new File("/home/mrose/Documents/finances/all.dat").toPath(), Charset.defaultCharset());

            for(String line: linez) {
                String[] parts = StringUtils.split(line, '|');
                Row r = new Row();
                r.id = UUID.randomUUID().toString();
                r.tranDate = dtf.parseDateTime(parts[0]);
                r.detail = StringUtils.upperCase(parts[1]) + " : " + StringUtils.upperCase(parts[2]);
                r.amount = new BigDecimal(parts[3]);
                if( r.tranDate.getYear() != 2013 ) {
                    continue;
                }

                pickCategory(r);
                addRow(r);
            }
            ps.executeBatch();
            c.commit();
            markMulti();

        } catch (Throwable e) {
            log.warn(e.getMessage(), e);
        } finally {
            ;
        }
    }


    private static void addRow(Row r) throws SQLException {
        if (r.amount.compareTo(BigDecimal.ZERO) != 0) {
            ps.setString(1, r.id);
            ps.setTimestamp(2, new Timestamp(r.tranDate.getMillis()));
            ps.setString(3, r.category == null ? null : r.category.name());
            ps.setString(4, r.detail);
            ps.setBigDecimal(5, r.amount);
            ps.addBatch();
        }
    }

    private static void truncateTable() throws SQLException {
        Statement st = c.createStatement();
        st.executeUpdate("truncate table tran_log");
        c.commit();
        st.close();
    }

    private static void pickCategory(Row r) {
        if( StringUtils.contains(r.detail, "PAYROLLPPD")) {
            r.category = Category.ACC_INCOME;
        } else if(StringUtils.contains(r.detail, "HEALTHEQUITY INC")) {
            r.category = Category.ACC_INCOME;
        } else if(StringUtils.contains(r.detail, "ATM CHECK DEPOSIT") && r.amount.intValue() > 1200) {
            r.category = Category.NVI_INCOME;
        } else if(StringUtils.contains(r.detail, "ATM CHECK DEPOSIT") && r.amount.intValue() <= 1200 ) {
            r.category = Category.MISC_INCOME;
        } else if(r.amount.intValue() >= 300 ) {
            r.category = Category.MISC_INCOME;
        } else if(r.amount.intValue() >= 0 ) {
            r.category = Category.REFUND;
        } else if( StringUtils.contains(r.detail, "CITIMORTGAGE")) {
            r.category = Category.MORTGAGE;
        } else if( StringUtils.contains(r.detail, "PARKING")) {
            r.category = Category.PARKING;
        } else if (StringUtils.contains(r.detail, "ITUNES")) {
            r.category = Category.ITUNES;
        } else if (StringUtils.contains(r.detail, "LINDA")) {
            r.category = Category.MEDICAL;
        } else if (StringUtils.contains(r.detail, "STARBUCKS")) {
            r.category = Category.MARTY;
        } else if (StringUtils.contains(r.detail, "ZAGAT")) {
            r.category = Category.MARTY;
        } else if (StringUtils.contains(r.detail, "GIFT")) {
            r.category = Category.GIFT;
        } else if (StringUtils.contains(r.detail, "HNC")) {
            r.category = Category.MEDICAL;
        } else if (StringUtils.contains(r.detail, "SWEDISH")) {
            r.category = Category.MEDICAL;
        } else if (StringUtils.contains(r.detail, "MATTHIAS") && r.amount.intValue() < -200) {
            r.category = Category.TUITION;
        } else if (StringUtils.contains(r.detail, "WICKSTROM")) {
            r.category = Category.CAR;
        } else if (StringUtils.contains(r.detail, "FORD")) {
            r.category = Category.CAR;
        } else if (StringUtils.contains(r.detail, "NICK MATILLA")) {
            r.category = Category.CAR;
        } else if (StringUtils.contains(r.detail, "INSURANCE")) {
            r.category = Category.INSURANCE;
        } else if( StringUtils.contains(r.detail, "ATM WITHDRAWAL") && r.amount.intValue() < -120) {
            r.category = Category.CHILDCARE;
        } else if( StringUtils.contains(r.detail, "ATM WITHDRAWAL")) {
            r.category = Category.CASH;
        } else if( StringUtils.contains(r.detail, "CHILDCARE")) {
            r.category = Category.CHILDCARE;
        } else if( StringUtils.contains(r.detail, "JACKIE TV REIMBURSE")) {
            r.category = Category.NET;
        } else if (StringUtils.contains(r.detail, "FID BKG SVC")) {
            r.category = Category.NET;
         } else if( StringUtils.contains(r.detail, "SHELL")) {
            r.category = Category.GASOLINE;
        } else if( StringUtils.contains(r.detail, "EXXON")) {
            r.category = Category.GASOLINE;
        } else if( StringUtils.contains(r.detail, "SOUTHWEST AIRLINES")) {
            r.category = Category.TRAVEL;
        } else if( StringUtils.contains(r.detail, "AA AIR TICKET")) {
            r.category = Category.TRAVEL;
        } else if( StringUtils.contains(r.detail, "ORBITZ")) {
            r.category = Category.TRAVEL;
        } else if( StringUtils.contains(r.detail, "AMERICAN AIRLINES")) {
            r.category = Category.TRAVEL;
        } else if(StringUtils.contains(r.detail, "HARVESTIME")) {
            r.category = Category.GROCERY;
        } else if(StringUtils.contains(r.detail, "TRADER JOE")) {
            r.category = Category.GROCERY;
        } else if(StringUtils.contains(r.detail, "WHOLE FOODS")) {
            r.category = Category.GROCERY;
        } else if(StringUtils.contains(r.detail, "JEWEL") && r.amount.intValue() > -150) { // Also Matches Jewlery (without amount)
            r.category = Category.GROCERY;
        } else if(StringUtils.contains(r.detail, "DOMINICKS")) {
            r.category = Category.GROCERY;
        } else if(StringUtils.contains(r.detail, "MARIANOS")) {
            r.category = Category.GROCERY;
        } else if(StringUtils.contains(r.detail, "GENES SAUSAGE")) {
            r.category = Category.GROCERY;
        } else if(StringUtils.contains(r.detail, "SUNSET FOODS")) {
            r.category = Category.GROCERY;
        } else if(StringUtils.contains(r.detail, "WALGREENS") && r.amount.intValue() < -75 ) {
            r.category = Category.MEDICAL;
        } else if(StringUtils.contains(r.detail, "WALGREENS") && r.amount.intValue() >= -75 ) {
            r.category = Category.OTHER;
        } else if(StringUtils.contains(r.detail, "DRUG STORE/PHARMACY") && r.amount.intValue() >= -75 ) {
            r.category = Category.OTHER;
        } else if (StringUtils.contains(r.detail, "MEDICAL")) {
            r.category = Category.MEDICAL;
        } else if (StringUtils.contains(r.detail, "PEOPLES GAS LIGHT")) {
            r.category = Category.HOME_OPERATION;
        } else if (StringUtils.contains(r.detail, "COMMONWEALTH EDISON")) {
            r.category = Category.HOME_OPERATION;
        } else if (StringUtils.contains(r.detail, "DIRECTV")) {
            r.category = Category.HOME_OPERATION;
        } else if (StringUtils.contains(r.detail, "AT&T")) {
            r.category = Category.HOME_OPERATION;
        } else if (StringUtils.contains(r.detail, "LUCY TOMAL")) {
            r.category = Category.HOME_OPERATION;
        } else if (StringUtils.contains(r.detail, "TROY AND SONS")) {
            r.category = Category.HOME_OPERATION;
        } else if (StringUtils.contains(r.detail, "RAVENSWOOD")) {
            r.category = Category.HOME_OPERATION;
        } else if (StringUtils.contains(r.detail, "CONDO")) {
            r.category = Category.CONDO;
        } else if (StringUtils.contains(r.detail, "KIDS")) {
            r.category = Category.KIDS;
        } else if (StringUtils.contains(r.detail, "CHICAGO PARK DISTRICT")) {
            r.category = Category.KIDS;
        } else if (StringUtils.contains(r.detail, "TERESA")) {
            r.category = Category.CHARITY;
        } else if (StringUtils.contains(r.detail, "MATTHIAS")) {
            r.category = Category.CHARITY;
        } else if( StringUtils.contains(r.detail, "CENTRAL CHECKOUT")) {
            r.category = Category.TARGET;
        } else if( StringUtils.contains(r.detail, "COSTCO")) {
            r.category = Category.TARGET;
        } else if( StringUtils.contains(r.detail, "AMAZON")) {
            r.category = Category.TARGET;
        } else if( StringUtils.contains(r.detail, "HOTEL")) {
            r.category = Category.TRAVEL;
        } else if( StringUtils.contains(r.detail, "VACATION")) {
            r.category = Category.TRAVEL;
        } else if( StringUtils.contains(r.detail, "WYNN")) {
            r.category = Category.TRAVEL;
        } else if( StringUtils.contains(r.detail, "DIANES FLIGHT AND ANN REIMBURSE")) {
            r.category = Category.TRAVEL;
        } else if( StringUtils.contains(r.detail, "WYNN")) {
            r.category = Category.TRAVEL;
        } else if( StringUtils.contains(r.detail, "DENISE")) {
            r.category = Category.TRAVEL;
        } else if( StringUtils.contains(r.detail, "HARDING")) {
            r.category = Category.TRAVEL;
        } else if( StringUtils.contains(r.detail, "BANANA REPUBLIC")) {
            r.category = Category.CLOTHES;
        } else if( StringUtils.contains(r.detail, "LORD & TAYLOR")) {
            r.category = Category.CLOTHES;
        } else if( StringUtils.startsWith(r.detail, "MACY")) {
            r.category = Category.CLOTHES;
        } else if( StringUtils.contains(r.detail, "KOHL")) {
            r.category = Category.CLOTHES;
        } else if( StringUtils.contains(r.detail, "RESTAURANT")) {
            r.category = Category.RESTAURANT;
        } else if( StringUtils.contains(r.detail, "TICKET")) {
            r.category = Category.ENTERTAINMENT;
        } else if( StringUtils.contains(r.detail, "PENINSULA")) {
            r.category = Category.ENTERTAINMENT;
        } else if( StringUtils.contains(r.detail, "IL 529")) {
            r.category = Category.NET;
        } else if( StringUtils.contains(r.detail, "SALON")) {
            r.category = Category.HAIR;
        } else if( StringUtils.contains(r.detail, "LATHER")) {
            r.category = Category.HAIR;
        } else if( StringUtils.contains(r.detail, "SUPER NAILS")) {
            r.category = Category.SALLY;
        } else if( StringUtils.contains(r.detail, "SAWYER")) {
            r.category = Category.TRAVEL;
        } else if( StringUtils.contains(r.detail, "HERTZ")) {
            r.category = Category.TRAVEL;
        } else if( r.amount.intValue() < -350 ) {
            r.category = Category.ONE_OFF;
        }


        if( StringUtils.contains(r.detail, "LAS VEGAS") && r.tranDate.getMonthOfYear() == 7) {
            r.category = Category.TRAVEL;
        } else if( StringUtils.contains(r.detail, "LAS VEGAS") && r.tranDate.getMonthOfYear() == 10) {
            r.category = Category.NET;
        } else if( StringUtils.contains(r.detail, "LAS VEGAS") && r.tranDate.getMonthOfYear() == 11) {
            r.category = Category.NET;
        }
    }



    private static void markMulti() throws SQLException {
        String[] SQLs = new String[] {
                "update tran_log set category='NET' where id in ( " +
                        "        select tl2.id " +
                        "        from tran_log tl1, tran_log tl2 " +
                        "        where tl1.id != tl2.id and " +
                        "        tl1.tran_date between tl2.tran_date-4 and tl2.tran_date+4 and " +
                        "        tl1.amount = -1*tl2.amount and " +
                        "        (tl1.detail like '%PAYMENT%' or tl1.detail like '%TRANSFER%' or tl1.detail like '%PMT FROM BILL PAYER%') AND " +
                        "        (tl2.detail like '%PAYMENT%' or tl2.detail like '%TRANSFER%' or tl2.detail like '%PMT FROM BILL PAYER%')" +
                        ")",
                "delete from tran_log where detail like '%IGNORE%'",
                "delete from tran_log where detail like '%ONLINE PAYMENT FROM CHK 0%'",
                "delete from tran_log where detail like '%INSURANCE CAR WRECK CHECK%'"
        };
        for(String SQL: SQLs) {
            Statement st = c.createStatement();
            st.executeUpdate(SQL);
            st.close();
        }

        c.commit();
    }

    static class Row {
        String id;
        DateTime tranDate;
        Category category;
        String detail;
        BigDecimal amount;

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("tranDate", tranDate)
                    .add("category", category)
                    .add("detail", detail)
                    .add("amount", amount)
                    .toString();
        }
    }

    static enum Category {
        NET,

        ACC_INCOME,
        NVI_INCOME,
        MISC_INCOME,
        REFUND,

        PARKING,
        ITUNES,
        MEDICAL,
        TUITION,
        CAR,
        INSURANCE,
        CHILDCARE,
        CASH,
        GASOLINE,
        TRAVEL,
        TARGET,
        COSTCO,
        GROCERY,
        HOME_OPERATION,
        CONDO,
        GIFT,
        MARTY,
        SALLY,
        KIDS,
        CHARITY,
        ENTERTAINMENT,
        HAIR,

        RESTAURANT,
        CLOTHES,
        ONE_OFF,

        MORTGAGE,
        OTHER;
    }
}
