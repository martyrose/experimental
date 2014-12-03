package com.mrose.financial;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Months;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by martinrose on 12/3/14.
 */
public class OverageDetail {
  private static final Logger log = LoggerFactory.getLogger(OverageDetail.class);

  private static final String DB_IP = "192.168.56.101";
  private static final String JDBC_URL = "jdbc:postgresql://" + DB_IP + ":5432/mrose";
  private static final String JDBC_USER = "mrose";
  private static final String JDBC_PASS = "mrose";

  private static final Integer MONTHS_BACK = 10;
  private static final Integer MIN_CATEGORY_OVERAGE = -10;
  private static final Integer OVERALL_BIG_EXPENSE = -100;
  private static final YearMonth END_TIME = new YearMonth(2014, DateTimeConstants.NOVEMBER);

  private static NumberFormat currencyFormat;
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");

  private static Connection c;

  public static void main(String[] args) {
    int lineNumber = 1;
    try {
      currencyFormat = NumberFormat.getCurrencyInstance();
      currencyFormat.setMaximumFractionDigits(0);

      c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
      c.setAutoCommit(false);
      System.out.println("Last Month");
      System.out.println("Category Overage");
      categoryOverage(END_TIME, END_TIME);
      System.out.println("Expenses over $" + OVERALL_BIG_EXPENSE);
      expensesOverThreshold(END_TIME, END_TIME, OVERALL_BIG_EXPENSE);

      System.out.println("==========================================");

      YearMonth yearAgo = END_TIME.minusMonths(MONTHS_BACK);
      System.out.println("Year Ago");
      System.out.println("Category Overage");
      categoryOverage(yearAgo, END_TIME);
      System.out.println("Expenses over $" + -500);
      expensesOverThreshold(yearAgo, END_TIME, -500);
    } catch (Throwable e) {
      log.warn("Line: " + lineNumber + " : " + e.getMessage(), e);
    }
  }

  private static void expensesOverThreshold(YearMonth start, YearMonth end, int threshold) throws SQLException {
    for(Journal j: getJournalsOverAmount(c, start, end, threshold)) {
      if( "MORTGAGE".equals(j.category()) || "TUITION".equals(j.category())) {
         continue;
      }
      String amount = Strings.padStart(String.valueOf(currencyFormat.format(j.amount())), 7, ' ');
      System.out.println("\t" + amount + "   ==>  " + j.desc1() + " on " + dtf.print(j.dttm()));
    }
  }

  private static void categoryOverage(YearMonth start, YearMonth end) throws SQLException {
    Map<Category, Integer> lastMonth = getSummaryData(c, start, end);
    List<Pair<Category, Integer>> overCategories = new ArrayList<>();

    int months = Months.monthsBetween(start, end.plusMonths(1)).getMonths();

    for (Map.Entry<Category, Integer> e : lastMonth.entrySet()) {
      Category cat = e.getKey();
      int monies = e.getValue();
      int budget = cat.budget() * months;

      // ignore where monies > 0
      if( monies > 0 ) {
        continue;
      }
      // Only interested where we spent more than we budgeted
      // so if monies > budget continue
      if( monies > budget ) {
        continue;
      }
      int overSpend = budget - monies;
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

    // Now sort by those we overspent the most
    overCategories.sort(Collections.reverseOrder(new Comparator<Pair<Category, Integer>>() {
      @Override
      public int compare(Pair<Category, Integer> o1, Pair<Category, Integer> o2) {
        int budget1 = o1.getKey().budget() * months;
        int monies1 = o1.getValue();
        int overSpend1 = budget1 - monies1;

        int budget2 = o2.getKey().budget() * months;
        int monies2 = o2.getValue();
        int overSpend2 = budget2 - monies2;

        return Integer.valueOf(overSpend1).compareTo(Integer.valueOf(overSpend2));
      }
    }));

    for (Pair<Category, Integer> p : overCategories) {
      Category cat = p.getKey();
      final int budget = (Math.abs(cat.budget()) == 1 ? 0 : cat.budget()) * months;;
      int monies = p.getValue();
      int overSpend = budget - monies;

      System.out.println("Category " + cat.name() + " had a budget of " + currencyFormat.format(Math.abs(budget))
          + " against expenditures of " + currencyFormat.format(Math.abs(monies)));
      if( budget != 0 ) {
        float overSpendPercent = ((float) monies / (float) budget);
        System.out.println(
            "\tThat is " + currencyFormat.format(overSpend) + " over which is " + ((int) (overSpendPercent * 100)) + "%");
      }

      List<Journal> journals = getJournals(c, p.getLeft(), start, end);
      for(Journal j: journals) {
        if (j.amount() < MIN_CATEGORY_OVERAGE) {
          // TODO Add in date time and commonize this with  expensesOverThreshold
          String amount = Strings.padStart(String.valueOf(currencyFormat.format(j.amount())), 7, ' ');
          System.out.println("\t" + amount + "   ==>  " + j.desc1() + " on " + dtf.print(j.dttm()));
        }
      }
      System.out.println("");
    }
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

  private static Map<Category, Integer> getSummaryData(Connection c, YearMonth start, YearMonth end) throws SQLException {
    Map<String, Category> categories = getCategories(c);
    Map<Category, Integer> results = new HashMap<>();

    for (Category cc : categories.values()) {
      results.put(cc, 0);
    }

    String SQL = "select category, sum(amount) "
        + " from journals "
        + " where ts between ? and ? "
        + " group by category";
    try (PreparedStatement ps = c.prepareStatement(SQL)) {
      ps.setTimestamp(1, new Timestamp(start.toLocalDate(1).toDateTimeAtStartOfDay().getMillis()));
      ps.setTimestamp(2, new Timestamp(end.plusMonths(1).toLocalDate(1).toDateTimeAtStartOfDay().getMillis()));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Category category = categories.get(rs.getString(1));
          int bi = rs.getInt(2);
          results.put(category, bi);
        }
      }
    }

    return results;
  }

  private static List<Journal> getJournals(Connection c, Category cat, YearMonth start, YearMonth end) throws SQLException {
    String SQL = "select desc1, amount, category, ts "
        + " from journals "
        + " where ts between ? and ? "
        + " and category = ?"
        + " order by amount";

    List<Journal> journals = new ArrayList<>();

    try(PreparedStatement ps = c.prepareStatement(SQL)) {
      ps.setTimestamp(1, new Timestamp(start.toLocalDate(1).toDateTimeAtStartOfDay().getMillis()));
      ps.setTimestamp(2, new Timestamp(end.plusMonths(1).toLocalDate(1).toDateTimeAtStartOfDay().getMillis()));
      ps.setString(3, cat.name());
      try(ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          journals.add(new Journal(rs.getString(1), rs.getInt(2), rs.getString(3), new DateTime(rs.getTimestamp(4).getTime())));
        }
      }
    }
    return journals;
  }

  private static List<Journal> getJournalsOverAmount(Connection c, YearMonth start, YearMonth end, int threshold) throws SQLException {
    String SQL =
        "select desc1, amount, category, ts "
        + " from journals "
        + " where ts between ? and ? "
        + " and amount <= ?"
        + " order by amount";

    List<Journal> journals = new ArrayList<>();

    try(PreparedStatement ps = c.prepareStatement(SQL)) {
      ps.setTimestamp(1, new Timestamp(start.toLocalDate(1).toDateTimeAtStartOfDay().getMillis()));
      ps.setTimestamp(2, new Timestamp(end.plusMonths(1).toLocalDate(1).toDateTimeAtStartOfDay().getMillis()));
      ps.setInt(3, threshold);
      try(ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          journals.add(new Journal(rs.getString(1), rs.getInt(2), rs.getString(3), new DateTime(rs.getTimestamp(4).getTime())));
        }
      }
    }
    return journals;
  }

}
