package com.mrose.financial;

import com.google.common.collect.Maps;
import org.joda.time.DateTimeConstants;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by martinrose on 9/5/14.
 */
public class CategorizationDump {
  private static final Logger log = LoggerFactory.getLogger(CategorizationDump.class);

  private static final String JDBC_URL = "jdbc:postgresql://10.12.17.124:5432/mrose";
  private static final String JDBC_USER = "mrose";
  private static final String JDBC_PASS = "mrose";

  private static final YearMonth START_TIME = new YearMonth(2015, DateTimeConstants.JANUARY);
  private static final YearMonth END_TIME = new YearMonth(2015, DateTimeConstants.SEPTEMBER);

  private static Connection c;

  public static void main(String[] args) {
    int lineNumber = 1;
    try {
      c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
      c.setAutoCommit(false);

      List<YearMonth> months = buildMonths();

      Map<Category, Map<YearMonth, Integer>> categoryData = getSummaryData(c, months);
      CategoryPrinter printer = new CategoryPrinter(System.out);
      printer.print(categoryData, months);
    } catch (Throwable e) {
      log.warn("Line: " + lineNumber + " : " + e.getMessage(), e);
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

  private static Map<String, Category> getCategories(Connection c) throws SQLException {
    Map<String, Category> results = Maps.newHashMap();
    try (PreparedStatement ps = c.prepareStatement("select key, budget from categories")) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          results.put(rs.getString(1), new Category(rs.getString(1), rs.getInt(2)));
        }
      }
    }
    return results;
  }

  private static Map<Category, Map<YearMonth, Integer>> getSummaryData(Connection c,
      List<YearMonth> months) throws SQLException {
    Map<String, Category> categories = getCategories(c);
    Map<Category, Map<YearMonth, Integer>> results = new HashMap<>();

    for (Category cc : categories.values()) {
      Map<YearMonth, Integer> entries = Maps.newHashMap();
      for (YearMonth month : months) {
        entries.put(month, 0);
      }
      results.put(cc, entries);
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
          int bi = rs.getInt(3);
          results.get(category).put(dt, bi);
        }
      }
    }

    return results;
  }
}
