package com.accertify.dumb;

import com.google.common.base.Objects;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import org.apache.commons.lang3.CharSetUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
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
 mrose@zeta:~/Documents/finance$ cat convertMint.py
 #!/usr/bin/python
 import csv
 import sys

 reader = csv.reader(sys.stdin, delimiter=',', quotechar='"')
 writer = csv.writer(sys.stdout,delimiter='|', lineterminator='\n')

 for row in reader:
 writer.writerow(row)
 */


/**
 drop table journals;
 drop table categories;
 drop table events;

 create table journals (
 ts date,
 category text,
 event text,
 desc1 text,
 desc2 text,
 amount numeric(7,2),
 acct text,
 hash text,
 id text primary key
 );

 create table categories (
 key text primary key,
 value text,
 budget numeric(7,2)
 );

 create table events (
 key text primary key,
 value text,
 ts date
 );

 */

/**
 // Find and evaluate duplicates

 select * from journals where hash in (
     select hash from journals
     group by hash
     having count(1) > 1
 ) order by hash

 // categorize
 select * from journals where category is null order by ts, desc1


 -- categories not used
 select key from categories
 except
 select category from journals

 -- categories not defined
 select category from journals
 except
 select key from categories

 -- review categorization
 select *
 from journals
 order by category, ts

 to_date('2014.02.07', 'YYYY.MM.DD')
 */
public class LoadFinancialData {
    private static final Logger log = LoggerFactory.getLogger(LoadFinancialData.class);

    private static final String JDBC_URL = "jdbc:postgresql://10.216.30.64:5432/mrose";
    private static final String JDBC_USER = "mrose";
    private static final String JDBC_PASS = "mrose";

    private static final String FILE_PATH = "/home/mrose/Documents/finance/data.csv";
    private static final int YEAR = 2014;
    private static final int MONTH = DateTimeConstants.JANUARY;

    private static Connection c;
    private static PreparedStatement ps;
    // 1/04/2012
    // http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
    private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");

    public static void main(String[] args) {
        try {
            c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
            c.setAutoCommit(false);
            ps = c.prepareStatement("insert into journals(id, hash, ts, desc1, desc2, amount, acct) values (?,?,?,?,?,?,?)");

            List<String> linez = Files.readAllLines(new File(FILE_PATH).toPath(), Charset.defaultCharset());

            for(String line: linez) {
                String[] parts = StringUtils.split(line, '|');
                if(StringUtils.equals(parts[0], "Date")) {
                    // HEADER ROW DISCARD
                    continue;
                }
                Row r = new Row();
                r.id = UUID.randomUUID().toString();
                r.ts = dtf.parseDateTime(parts[0]);
                r.desc1 = cleanup(parts[1]);
                r.desc2 = cleanup(parts[2]);
                r.amount = new BigDecimal(parts[3]);
                // Make debits negative
                if( StringUtils.equals(parts[4], "debit")) {
                    r.amount = r.amount.negate();
                }
                r.acct = cleanup(parts[6]);

                if( r.ts.getYear() != YEAR  ) {
                    continue;
                }
                if( r.ts.getMonthOfYear() != MONTH ) {
                    continue;
                }
                if( r.amount.compareTo(BigDecimal.ZERO) == 0 ) {
                    continue;
                }
                addRow(r);
            }
            ps.executeBatch();
            c.commit();
        } catch (Throwable e) {
            log.warn(e.getMessage(), e);
        } finally {
            ;
        }
    }

    private static String cleanup(String s) {
        return StringUtils.upperCase(StringUtils.trimToEmpty(s)).replaceAll("\\s+", " ");
    }


    private static void addRow(Row r) throws SQLException {
        HashFunction hf = Hashing.murmur3_128();
        HashCode hc = hf.newHasher().putLong(r.ts.getMillis()).putString(r.amount.toPlainString(), Charset.defaultCharset()).hash();

        ps.setString(1, r.id);
        ps.setString(2, hc.toString());
        ps.setTimestamp(3, new Timestamp(r.ts.getMillis()));
        ps.setString(4, r.desc1);
        ps.setString(5, r.desc2);
        ps.setBigDecimal(6, r.amount);
        ps.setString(7, r.acct);

        ps.addBatch();
    }

    static class Row {
        String id;
        DateTime ts;
        String desc1;
        String desc2;
        BigDecimal amount;
        String acct;

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("ts", ts)
                    .add("desc1", desc1)
                    .add("desc2", desc2)
                    .add("amount", amount)
                    .toString();
        }
    }

}