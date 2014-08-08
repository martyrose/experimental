package com.mrose.dumb;

import com.google.common.base.Joiner;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by martinrose on 6/6/14.
 */
public class FullCategorization {

  private static final Logger log = LoggerFactory.getLogger(FullCategorization.class);

  private static final char SEP = ',';
  private static final String JDBC_URL = "jdbc:postgresql://10.12.17.112:5432/mrose";
  private static final String JDBC_USER = "mrose";
  private static final String JDBC_PASS = "mrose";
  // http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
  private static final DateTimeZone zone = DateTimeZone.getDefault();

  private static final DateTime START_TIME = new DateTime(2014, DateTimeConstants.JANUARY, 1, 0, 0,
      zone);
  private static final DateTime END_TIME = new DateTime(2014, DateTimeConstants.AUGUST, 1, 0, 0,
      zone);
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");

  private static Connection c;

  public static void main(String[] args) {
    int lineNumber = 1;
    try {
      c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
      c.setAutoCommit(false);

      Map<String, CategoryEntries> results = getData(c);
      List<CategoryEntries> entries = new ArrayList<>(results.values());
      Collections.sort(entries, new Comparator<CategoryEntries>() {
        @Override
        public int compare(CategoryEntries ce1, CategoryEntries ce2) {
          return ce1.total.compareTo(ce2.total);
        }
      });

      System.out.println(formatHeaderRow());
      for(CategoryEntries ce: entries) {
        System.out.println(formatDataRow(ce));
      }
      c.commit();
    } catch (SQLException e) {
      try {
        c.rollback();
      } catch (SQLException e1) {
        ;
      }
      if (e.getNextException() != null) {
        log.warn(e.getNextException().getMessage(), e);
      }
      log.warn("Line: " + lineNumber + " : " + e.getMessage(), e);
    } catch (Throwable e) {
      log.warn("Line: " + lineNumber + " : " + e.getMessage(), e);
    } finally {
      ;
    }
  }

  private static String formatDataRow(CategoryEntries ce) {
    List<String> cells = new LinkedList<>();
    cells.add(ce.category);
    DateTime current = START_TIME;
    while (current.isBefore(END_TIME)) {
      if(ce.values.containsKey(current)) {
        cells.add(ce.values.get(current).toBigInteger().toString());
      } else {
        cells.add(BigInteger.ZERO.toString());
      }
      current = current.plusMonths(1);
    }
    return Joiner.on(SEP).join(cells);
  }

  private static String formatHeaderRow() {
    // Print out header rows
    List<String> cells = new LinkedList<>();
    cells.add("");
    DateTime current = START_TIME;
    while (current.isBefore(END_TIME)) {
      cells.add(dtf.print(current));
      current = current.plusMonths(1);
    }
    return Joiner.on(SEP).join(cells);
  }

  private static Set<String> getCategories(Connection c) throws SQLException {
    Set<String> results = new HashSet<>();
    try (PreparedStatement ps = c.prepareStatement("select key from categories")) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          results.add(rs.getString(1));
        }
      }
    }
    return results;
  }

  private static Map<String, CategoryEntries> getData(Connection c) throws SQLException {
    Map<String, CategoryEntries> results = new HashMap<>();
    for (String cc : getCategories(c)) {
      results.put(cc, new CategoryEntries(cc));
    }
    String SQL = " select date_trunc('month', ts), category, sum(amount) "
        + " from journals "
        + " where ts between ? and ? "
        + " group by date_trunc('month', ts), category";
    try (PreparedStatement ps = c.prepareStatement(SQL)) {
      ps.setTimestamp(1, new Timestamp(START_TIME.getMillis()));
      ps.setTimestamp(2, new Timestamp(END_TIME.getMillis()));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          DateTime dt = new DateTime(rs.getTimestamp(1).getTime());
          String category = rs.getString(2);
          BigDecimal bd = rs.getBigDecimal(3);

          CategoryEntries ce = results.get(category);
          ce.addValue(dt, bd);
        }
      }
    }

    return results;
  }

  static class CategoryEntries {

    private final String category;
    private BigDecimal total = BigDecimal.ZERO;
    private final Map<DateTime, BigDecimal> values = new HashMap<>();

    CategoryEntries(String category) {
      this.category = category;
    }

    public void addValue(DateTime dt, BigDecimal bd) {
      values.put(dt, bd);
      total = total.add(bd);
    }

    @Override
    public String toString() {
      SortedMap<DateTime, BigDecimal> z = new TreeMap<>(values);
      return "[category=" + category + "; total=" + total.toString() + "; values=" + z.toString()
          + "]";
    }
  }
}
