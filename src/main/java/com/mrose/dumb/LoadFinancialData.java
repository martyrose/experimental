package com.mrose.dumb;

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
 drop table journals;
 drop table categories;
 drop table events;

 create table journals (
 ts date,
 category text,
 event text,
 desc1 text,
 desc2 text,
 amount numeric(12,2),
 acct text,
 hash text,
 id text primary key
 );

 create table categories (
 key text primary key,
 value text,
 budget numeric(12,2)
 );

 create table events (
 key text primary key,
 value text,
 ts date
 );

 */

/**
# Find and evaluate duplicates
select *
from journals
where hash in (
  select hash
  from journals
  where ts >= to_date('2014.02.01', 'YYYY.MM.DD')
  group by hash
  having count(1) > 1
)
order by hash


# Look for paying CC bills
select ts, category, event, amount, acct, desc1, desc2, id
from journals
where category is null and
ts between to_date('2014.04.01', 'YYYY.MM.DD') and to_date('2014.05.01', 'YYYY.MM.DD')
order by abs(amount) desc

# auto categorize
update journals
set category='CHILDCARE'
where category is null and
lower(desc1) like '%childcare%'

update journals set category='BIGBOX' where category is null and lower(desc1) like '%central checkout%'

update journals set category='BIGBOX' where category is null and lower(desc1) like '%amazon%'

update journals set category='MEDICAL' where category is null and lower(desc1) like '%medical%'

update journals set category='MEDICAL' where category is null and lower(desc1) like '%swedish%'

# categorize
select ts, category, event, amount, acct, desc1, desc2, id
from journals
where category is null
order by lower(desc1), ts

# categories not used
select key from categories except select category from journals where ts >= to_date('2014.04.01', 'YYYY.MM.DD')

# categories not defined
select category from journals where ts >= to_date('2014.04.01', 'YYYY.MM.DD') except select key from categories

# event definitions
select key from events order by ts

# review categorization
select ts, category, acct, amount, desc1, desc2, id
from journals where
ts >= to_date('2014.04.01', 'YYYY.MM.DD')
order by category, ts

# Review Category summary
select category, count(1), sum(amount)
from journals where ts between to_date('2014.04.01', 'YYYY.MM.DD') and to_date('2014.06.01', 'YYYY.MM.DD')
group by category
order by sum(amount) desc

# Review event summary
select event, count(1), sum(amount)
from journals where ts between to_date('2014.01.01', 'YYYY.MM.DD') and to_date('2014.06.01', 'YYYY.MM.DD') and
event is not null
group by event
order by sum(amount) desc

# Grand Total
select sum(amount)
from journals
where ts between to_date('2014.04.01', 'YYYY.MM.DD') and to_date('2014.06.01', 'YYYY.MM.DD')

# To Print
select ts, category, amount, acct, desc1 from journals where ts between
to_date('2014.04.01', 'YYYY.MM.DD') and to_date('2014.06.01', 'YYYY.MM.DD') and abs(amount) > 50
order by abs(amount) desc
 */
public class LoadFinancialData {

  private static final Logger log = LoggerFactory.getLogger(LoadFinancialData.class);

  private static final boolean doCommit = true;
  private static final String JDBC_URL = "jdbc:postgresql://192.168.56.101:5432/mrose";
  private static final String JDBC_USER = "mrose";
  private static final String JDBC_PASS = "mrose";

  // readlink -f file
  private static final String FILE_PATH = "/tmp/fin2.dat";
  private static final int YEAR = 2014;
  private static final int MONTH = DateTimeConstants.SEPTEMBER;

  private static Connection c;
  private static PreparedStatement ps;
  // 1/04/2012
  // http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");

  public static void main(String[] args) {
    int rowsAdded = 1;
    BigDecimal total = BigDecimal.ZERO;

    try {
      c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
      c.setAutoCommit(false);
      ps = c.prepareStatement(
          "insert into journals(id, hash, ts, desc1, desc2, amount, acct) values (?,?,?,?,?,?,?)");

      List<String> linez = Files
          .readAllLines(new File(FILE_PATH).toPath(), Charset.defaultCharset());

      for (String line : linez) {
        String[] parts = StringUtils.splitPreserveAllTokens(line, '|');
        if (StringUtils.equals(parts[0], "Date")) {
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
        if (StringUtils.equals(parts[4], "debit")) {
          r.amount = r.amount.negate();
        }
        r.acct = cleanup(parts[6]);

        if (r.ts.getYear() != YEAR) {
          continue;
        }
        if (r.ts.getMonthOfYear() != MONTH) {
          continue;
        }
        if (r.amount.compareTo(BigDecimal.ZERO) == 0) {
          continue;
        }
        addRow(r);
        rowsAdded++;
        total = total.add(r.amount);
      }
      ps.executeBatch();
      if (doCommit) {
        c.commit();
      }
      log.warn("Rows Processed: " + rowsAdded);
      log.warn("Total Amount: " + total.toString());
    } catch (SQLException e) {
      try {
        c.rollback();
      } catch (SQLException e1) {
        ;
      }
      if (e.getNextException() != null) {
        log.warn(e.getNextException().getMessage(), e);
      }
      log.warn("Line: " + rowsAdded + " : " + e.getMessage(), e);
    } catch (Throwable e) {
      log.warn("Line: " + rowsAdded + " : " + e.getMessage(), e);
    } finally {
      ;
    }
  }

  private static String cleanup(String s) {
    return StringUtils.upperCase(StringUtils.trimToEmpty(s)).replaceAll("\\s+", " ");
  }

  private static void addRow(Row r) throws SQLException {
    HashFunction hf = Hashing.murmur3_128();
    HashCode hc = hf.newHasher()
        .putLong(r.ts.getMillis())
        .putString(r.amount.toPlainString(), Charset.defaultCharset())
        .hash();

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
