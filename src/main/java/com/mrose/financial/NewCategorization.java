package com.mrose.financial;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import com.mrose.dumb.FullCategorization;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.YearMonth;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by martinrose on 9/5/14.
 */
public class NewCategorization {
  private static final Logger log = LoggerFactory.getLogger(FullCategorization.class);

  private static final String DB_IP = "192.168.56.101";
  private static final String JDBC_URL = "jdbc:postgresql://" + DB_IP + ":5432/mrose";
  private static final String JDBC_USER = "mrose";
  private static final String JDBC_PASS = "mrose";

  private static final YearMonth START_TIME = new YearMonth(2014, DateTimeConstants.JANUARY);
  private static final YearMonth END_TIME = new YearMonth(2014, DateTimeConstants.SEPTEMBER);

  private static Connection c;

  public static void main(String[] args) {
    int lineNumber = 1;
    try {
      c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
      c.setAutoCommit(false);

      List<YearMonth> months = buildMonths();

      Map<Category, Map<YearMonth, BigInteger>> categoryData = getSummaryData(c, months);
      CategoryPrinter printer = new CategoryPrinter(System.out);
      printer.print(categoryData, months);

      Function<Entry<Category, Map<YearMonth, BigInteger>>, Pair<Category, BigInteger>> flatten =
          new MonthFlatten(END_TIME);
      Map<Category, BigInteger> lastMonth = new PairToMap<Category, BigInteger>()
          .apply(Iterables.transform(categoryData.entrySet(), flatten));

      List<Pair<Category, BigInteger>> overCategories = new ArrayList<>();

      for (Map.Entry<Category, BigInteger> e : lastMonth.entrySet()) {
        Category cat = e.getKey();
        int monies = e.getValue().intValue();
        int budget = cat.budget().intValue();

        // ignore where monies > 0
        if( monies > 0 ) {
          continue;
        }
        // Only interested where we spent more than we budgeted
        // so if monies > budget continue
        if( monies > budget ) {
          continue;
        }
        long overSpend = budget - monies;
        // If absolute value is small just ignore it
        if (overSpend < 100) {
          continue;
        }
        float overSpendPercent = ((float)monies / (float)budget);
        // If less than 10% over just ignore it
        if( overSpendPercent < 1.1 ) {
          continue;
        }
        if( "SAVING".equals(cat.name())) {
          continue;
        }
        overCategories.add(Pair.of(e.getKey(), e.getValue()));
      }


//      log.warn("Category " + cat.name() + " had a budget of " + Math.abs(budget) + " against expenditures of " + Math.abs(monies));
//      log.warn("\tThat is " + overSpend + " over which is " + ((int)(overSpendPercent*100)) + "%");

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

  private static Map<Category, Map<YearMonth, BigInteger>> getSummaryData(Connection c,
      List<YearMonth> months) throws SQLException {
    Map<String, Category> categories = getCategories(c);
    Map<Category, Map<YearMonth, BigInteger>> results = new HashMap<>();

    for (Category cc : categories.values()) {
      Map<YearMonth, BigInteger> entries = Maps.newHashMap();
      for (YearMonth month : months) {
        entries.put(month, BigInteger.ZERO);
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
          BigInteger bi = rs.getBigDecimal(3).toBigInteger();
          results.get(category).put(dt, bi);
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
    private final BigInteger amount;

    Journal(DateTime ts, String desc1, String desc2, BigInteger amount) {
      this.ts = ts;
      this.desc1 = desc1;
      this.desc2 = desc2;
      this.amount = amount;
    }
  }
}
