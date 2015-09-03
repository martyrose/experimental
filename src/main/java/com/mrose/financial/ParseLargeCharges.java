package com.mrose.financial;


import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * TODO(martinrose) : Add Documentation
 */
public class ParseLargeCharges {
  private static final Logger log = LoggerFactory.getLogger(LoadFinancialData.class);

  // readlink -f file
  private static final String FILE_PATH = "/usr/local/google/home/martinrose/Desktop/mint.dat";
  private static final YearMonth LOAD_MONTH = new YearMonth(2015, DateTimeConstants.JUNE);

  // http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");

  public static void main(String[] args) {
    int rowsAdded = 0;
    BigDecimal total = BigDecimal.ZERO;

    try {
      List<String> linez = Files
          .readAllLines(new File(FILE_PATH).toPath(), Charset.defaultCharset());

      List<Row> rowz = new ArrayList<>();
      for (String line : linez) {
        if(StringUtils.isEmpty(line)) {
          continue;
        }
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

        if (r.ts.getYear() != LOAD_MONTH.getYear()) {
          continue;
        }
        if (r.ts.getMonthOfYear() != LOAD_MONTH.getMonthOfYear()) {
          continue;
        }
        if (r.amount.compareTo(BigDecimal.ZERO) == 0) {
          continue;
        }
        rowz.add(r);
        rowsAdded++;
        total = total.add(r.amount);
      }
      log.warn("Rows Processed: " + rowsAdded);
      log.warn("Total Amount: " + total.toString());

      List<Row> filtered = new ArrayList<>(Collections2.filter(rowz, new Predicate<Row>() {
        @Override
        public boolean apply(Row input) {
          return input.amount.abs().intValue() > 300;
        }
      }));

      Collections.sort(filtered, Collections.reverseOrder(new Comparator<Row>() {
        @Override
        public int compare(Row o1, Row o2) {
          return o1.amount.compareTo(o2.amount);
        }
      }));
      for (Row r : filtered) {
        log.warn("Date: " + r.ts + " Amount: " + r.amount + " Desc: " + r.desc1);
      }
    } catch (Throwable e) {
      log.warn("Line: " + rowsAdded + " : " + e.getMessage(), e);
    } finally {
      ;
    }
  }

  private static String cleanup(String s) {
    return StringUtils.upperCase(StringUtils.trimToEmpty(s)).replaceAll("\\s+", " ");
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
      return MoreObjects.toStringHelper(this)
          .add("ts", ts)
          .add("desc1", desc1)
          .add("desc2", desc2)
          .add("amount", amount)
          .toString();
    }
  }
}
