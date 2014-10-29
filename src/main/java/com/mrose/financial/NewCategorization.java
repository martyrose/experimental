package com.mrose.financial;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import com.mrose.dumb.FullCategorization;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.YearMonth;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by martinrose on 9/5/14.
 */
public class NewCategorization {
  private static final Logger log = LoggerFactory.getLogger(FullCategorization.class);

  private static final char SEP = ',';
  private static final String DB_IP = "192.168.56.101";
  private static final String JDBC_URL = "jdbc:postgresql://" + DB_IP + ":5432/mrose";
  private static final String JDBC_USER = "mrose";
  private static final String JDBC_PASS = "mrose";

  private static final YearMonth START_TIME = new YearMonth(2014, DateTimeConstants.JANUARY);
  private static final YearMonth END_TIME = new YearMonth(2014, DateTimeConstants.SEPTEMBER);

  // http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("MMM yyyy");

  private static Connection c;

  public static void main(String[] args) {
    int lineNumber = 1;
    try {
      c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
      c.setAutoCommit(false);

      List<YearMonth> months = buildMonths();

      Map<Category, Map<YearMonth, BigDecimal>> results = getSummaryData(c, months);
      List<Entry<Category, Map<YearMonth, BigDecimal>>> entries = new ArrayList<>(results.entrySet());

      final Function<Map<YearMonth, BigDecimal>, BigDecimal> summer = new MapSummation<>();

      // sort them by amount
      Collections.sort(entries, new Comparator<Entry<Category, Map<YearMonth, BigDecimal>>>() {
        @Override
        public int compare(Entry<Category, Map<YearMonth, BigDecimal>> o1,
            Entry<Category, Map<YearMonth, BigDecimal>> o2) {
          BigDecimal total1 = summer.apply(o1.getValue());
          BigDecimal total2 = summer.apply(o2.getValue());
          return total1.compareTo(total2);
        }
      });

      System.out.println(formatHeaderRow(months));
      for(Entry<Category, Map<YearMonth, BigDecimal>> e: entries) {
        System.out.println(formatDataRow(e, months));
      }
    } catch (Throwable e) {
      log.warn("Line: " + lineNumber + " : " + e.getMessage(), e);
    } finally {
      ;
    }
  }

  private static List<YearMonth> buildMonths() {
    List<YearMonth> months = new ArrayList<>();
    YearMonth point = END_TIME;
    while(!point.isBefore(START_TIME)) {
      months.add(point);
      point = point.minusMonths(1);
    }
    return months;
  }

  private static String formatDataRow(Entry<Category, Map<YearMonth, BigDecimal>> e, List<YearMonth> months) {
    List<String> cells = new LinkedList<>();
    cells.add(e.getKey().name());
    cells.add(e.getKey().budget().toBigInteger().toString());

    for(YearMonth month: months) {
      if(e.getValue().containsKey(month)) {
        cells.add(e.getValue().get(month).toBigInteger().toString());
      } else {
        cells.add(BigInteger.ZERO.toString());
      }
    }
    return Joiner.on(SEP).join(cells);
  }

  private static String formatHeaderRow(List<YearMonth> months) {
    // Print out header rows
    List<String> cells = new LinkedList<>();
    cells.add("category");
    cells.add("budget");

    for(YearMonth month: months) {
      cells.add(dtf.print(month));
    }
    return Joiner.on(SEP).join(cells);
  }

  private static Map<String, Category> getCategories(Connection c) throws SQLException {
    Map<String, Category> results = Maps.newHashMap();
    try (PreparedStatement ps = c.prepareStatement("select key, budget from categories")) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          results.put(rs.getString(1), new Category(rs.getString(1), rs.getBigDecimal(2)));
        }
      }
    }
    return results;
  }

  private static Map<Category, Map<YearMonth, BigDecimal>> getSummaryData(Connection c,
      List<YearMonth> months) throws SQLException {
    Map<String, Category> categories = getCategories(c);
    Map<Category, Map<YearMonth, BigDecimal>> results = new HashMap<>();

    for (Category cc : categories.values()) {
      Map<YearMonth, BigDecimal> entries = Maps.newHashMap();
      for (YearMonth month : months) {
        entries.put(month, BigDecimal.ZERO);
      }
      results.put(cc, new HashMap<>());
    }

    String SQL = "select date_trunc('month', ts), category, sum(amount) "
        + " from journals "
        + " where ts between ? and ? "
        + " group by date_trunc('month', ts), category";
    try (PreparedStatement ps = c.prepareStatement(SQL)) {
      ps.setTimestamp(1, new Timestamp(START_TIME.toLocalDate(1).toDateTimeAtStartOfDay().getMillis()));
      ps.setTimestamp(2, new Timestamp(END_TIME.plusMonths(1).toLocalDate(1).toDateTimeAtStartOfDay().getMillis()));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          YearMonth dt = new YearMonth(rs.getTimestamp(1).getTime());
          Category category = categories.get(rs.getString(2));
          BigDecimal bd = rs.getBigDecimal(3);
          results.get(category).put(dt, bd);
        }
      }
    }

    return results;
  }

  static class JournalEntries {
    private final Category category;
    private final DateTime month;
    private final List<Journal> entries;

    JournalEntries(Category category, DateTime month, List<Journal> entries) {
      this.category = category;
      this.month = month;
      this.entries = entries;
    }
  }

  static class Journal {
    private final DateTime ts;
    private final String desc1;
    private final String desc2;
    private final BigDecimal amount;

    Journal(DateTime ts, String desc1, String desc2, BigDecimal amount) {
      this.ts = ts;
      this.desc1 = desc1;
      this.desc2 = desc2;
      this.amount = amount;
    }
  }
}
