package com.mrose.dumb;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

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
  // http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
  private static final DateTimeZone zone = DateTimeZone.getDefault();

  private static final DateTime START_TIME = new DateTime(2014, DateTimeConstants.JANUARY, 1, 0, 0,
      zone);
  private static final DateTime END_TIME = new DateTime(2014, DateTimeConstants.SEPTEMBER, 1, 0, 0,
      zone);
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("MMM yyyy");

  private static Connection c;

  public static void main(String[] args) {
    int lineNumber = 1;
    try {
      c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
      c.setAutoCommit(false);

      Map<Category, CategoryEntries> results = getData(c);
      List<CategoryEntries> entries = new ArrayList<>(results.values());


      // sort them by amount
      Collections.sort(entries, new Comparator<CategoryEntries>() {
        @Override
        public int compare(CategoryEntries ce1, CategoryEntries ce2) {
          return ce1.total().compareTo(ce2.total());
        }
      });

      List<DateTime> months = buildMonths();

      System.out.println(formatHeaderRow(months));
      for(CategoryEntries ce: entries) {
        System.out.println(formatDataRow(ce, months));
      }


//      System.out.println(formatHeaderRow());
//      for(CategoryEntries ce: entries) {
//        System.out.println(formatDataRow(ce));
//      }
//      c.commit();
//    } catch (SQLException e) {
//      try {
//        c.rollback();
//      } catch (SQLException e1) {
//        ;
//      }
//      if (e.getNextException() != null) {
//        log.warn(e.getNextException().getMessage(), e);
//      }
//      log.warn("Line: " + lineNumber + " : " + e.getMessage(), e);
    } catch (Throwable e) {
      log.warn("Line: " + lineNumber + " : " + e.getMessage(), e);
    } finally {
      ;
    }
  }

  private static List<DateTime> buildMonths() {
    List<DateTime> months = Lists.newArrayList();
    DateTime point = END_TIME;
    while(!point.isBefore(START_TIME)) {
      months.add(point);
      point = point.minusMonths(1);
    }
    return months;
  }

  private static String formatDataRow(CategoryEntries ce, List<DateTime> months) {
    List<String> cells = new LinkedList<>();
    cells.add(ce.category.name);
    cells.add(ce.category.budget.toBigInteger().toString());

    for(DateTime month: months) {
      if(ce.values.containsKey(month)) {
        cells.add(ce.values.get(month).toBigInteger().toString());
      } else {
        cells.add(BigInteger.ZERO.toString());
      }
    }
    return Joiner.on(SEP).join(cells);
  }

  private static String formatHeaderRow(List<DateTime> months) {
    // Print out header rows
    List<String> cells = new LinkedList<>();
    cells.add("category");
    cells.add("budget");

    for(DateTime month: months) {
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

  private static Map<Category, CategoryEntries> getData(Connection c) throws SQLException {
    Map<String, Category> categories = getCategories(c);
    Map<Category, CategoryEntries> results = Maps.newHashMap();
    for (Category cc : categories.values()) {
      results.put(cc, new CategoryEntries(cc));
    }
    String SQL = "select date_trunc('month', ts), category, sum(amount) "
        + " from journals "
        + " where ts between ? and ? "
        + " group by date_trunc('month', ts), category";
    try (PreparedStatement ps = c.prepareStatement(SQL)) {
      ps.setTimestamp(1, new Timestamp(START_TIME.getMillis()));
      ps.setTimestamp(2, new Timestamp(END_TIME.plusMonths(1).getMillis()));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          DateTime dt = new DateTime(rs.getTimestamp(1).getTime());
          Category category = categories.get(rs.getString(2));
          BigDecimal bd = rs.getBigDecimal(3);

          CategoryEntries ce = results.get(category);
          ce.addValue(dt, bd);
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

  static class CategoryEntries {
    private final Category category;
    private final Map<DateTime, BigDecimal> values = new HashMap<>();

    CategoryEntries(Category category) {
      this.category = category;
    }

    public void addValue(DateTime dt, BigDecimal bd) {
      values.put(dt, bd);
    }

    @Override
    public String toString() {
      SortedMap<DateTime, BigDecimal> z = new TreeMap<>(values);
      return "[category=" + category.name + "; budget=" + category.budget.toString() + "; total=" + total().toString() + "; values=" + z.toString()
          + "]";
    }

    public BigDecimal total() {
      BigDecimal total = BigDecimal.ZERO;
      for( BigDecimal bd: values.values() ) {
        total = total.add(bd);
      }
      return total;
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

  static class Category {
    private final String name;
    private final BigDecimal budget;

    Category(String name,BigDecimal budget) {
      this.name = name;
      this.budget = budget;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null)
      {
        return false;
      }
      if (getClass() != obj.getClass())
      {
        return false;
      }
      final Category other = (Category) obj;
      return Objects.equals(this.name, other.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }
}
