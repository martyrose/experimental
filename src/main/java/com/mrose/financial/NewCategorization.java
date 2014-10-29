package com.mrose.financial;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import com.mrose.dumb.FullCategorization;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

      Map<Category, Map<YearMonth, Integer>> categoryData = getSummaryData(c, months);
      CategoryPrinter printer = new CategoryPrinter(System.out);
      printer.print(categoryData, months);

      Function<Entry<Category, Map<YearMonth, Integer>>, Pair<Category, Integer>> flatten =
          new MonthFlatten(END_TIME);
      Map<Category, Integer> lastMonth = new PairToMap<Category, Integer>()
          .apply(Iterables.transform(categoryData.entrySet(), flatten));

      List<Pair<Category, Integer>> overCategories = new ArrayList<>();

      for (Map.Entry<Category, Integer> e : lastMonth.entrySet()) {
        Category cat = e.getKey();
        int monies = e.getValue();
        int budget = cat.budget();

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
          int budget1 = o1.getKey().budget();
          int monies1 = o1.getValue();
          float overSpendPercent1 = ((float)monies1 / (float)budget1);

          int budget2 = o2.getKey().budget();
          int monies2 = o2.getValue();
          float overSpendPercent2 = ((float)monies2 / (float)budget2);

          return Float.valueOf(overSpendPercent1).compareTo(Float.valueOf(overSpendPercent2));
        }
      }));

      for (Pair<Category, Integer> p : overCategories) {
        Category cat = p.getKey();
        int budget = cat.budget();
        int monies = p.getValue();
        int overSpend = budget - monies;
        float overSpendPercent = ((float) monies / (float) budget);
        log.warn("Category " + cat.name() + " had a budget of " + Math.abs(budget)
            + " against expenditures of " + Math.abs(monies));
        log.warn(
            "\tThat is " + overSpend + " over which is " + ((int) (overSpendPercent * 100)) + "%");

        List<Journal> journals = new ArrayList<>(getJournals(c, p.getLeft(), END_TIME));
        Collections.sort(journals, new Comparator<Journal>() {
          @Override
          public int compare(Journal o1, Journal o2) {
            return o1.amount.compareTo(o2.amount);
          }
        });
        for(Journal j: journals) {
          log.warn("\t" + j.amount + " " + j.desc1 + " " + j.desc2);
        }
        log.warn("");
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

  private static Collection<Journal> getJournals(Connection c, Category cat, YearMonth month) throws SQLException {
    String SQL = "select ts, desc1, desc2, amount "
        + " from journals "
        + " where ts between ? and ? "
        + " and category = ?";

    Collection<Journal> journals = new ArrayList<>();

    try(PreparedStatement ps = c.prepareStatement(SQL)) {
      ps.setTimestamp(1, new Timestamp(month.toLocalDate(1).toDateTimeAtStartOfDay().getMillis()));
      ps.setTimestamp(2, new Timestamp(month.plusMonths(1).toLocalDate(1).toDateTimeAtStartOfDay().getMillis()));
      ps.setString(3, cat.name());
      try(ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          journals.add(new Journal(new LocalDate(rs.getDate(1)), rs.getString(2), rs.getString(3),
              rs.getInt(4)));
        }
      }
    }
    return journals;
  }

  static class Journal {
    private final LocalDate ts;
    private final String desc1;
    private final String desc2;
    private final Integer amount;

    Journal(LocalDate ts, String desc1, String desc2, Integer amount) {
      this.ts = ts;
      this.desc1 = desc1;
      this.desc2 = desc2;
      this.amount = amount;
    }
  }
}
