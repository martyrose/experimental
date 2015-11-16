package com.mrose.mint;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import au.com.bytecode.opencsv.CSVReader;

import com.mrose.financial.LoadFinancialData;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * TODO(martinrose) : Add Documentation
 */
public class MonthToDate {

  private static final Logger log = LoggerFactory.getLogger(LoadFinancialData.class);

  // readlink -f file
  private static final String FILE_PATH = "/tmp/transactions.csv";
  private static final YearMonth LOAD_MONTH = new YearMonth(2015, DateTimeConstants.NOVEMBER);

  // 1/04/2012
  // http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");
  private static final NumberFormat percentFormat = NumberFormat.getPercentInstance();
  private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

  static {
    percentFormat.setMaximumFractionDigits(0);
    currencyFormat.setMaximumFractionDigits(0);

    String symbol = currencyFormat.getCurrency().getSymbol();
    ((DecimalFormat)currencyFormat).setNegativePrefix(symbol+"-"); // or "-"+symbol if that's what you need
    ((DecimalFormat)currencyFormat).setNegativeSuffix("");
  }

  public static void main(String[] args) throws Exception {
    CSVReader reader = new CSVReader(new FileReader(FILE_PATH));
    Iterable<String[]> allRows = reader.readAll();
    double percentInMonth = ((double) DateTime.now().getDayOfMonth()) / ((double)30);

    System.out.println("Percent of Month Complete: " + percentFormat.format(percentInMonth));
    // Skip the header/prefix row
    allRows = Iterables.filter(allRows, new Predicate<String[]>() {
      @Override
      public boolean apply(@Nullable String[] input) {
        return !input[0].equals("Date");
      }
    });

    Iterable<MintRow> mintRows = Iterables.transform(allRows, new Function<String[], MintRow>() {
      @Nullable
      @Override
      public MintRow apply(@Nullable String[] input) {
        return new MintRow(input);
      }
    });

    mintRows = Iterables.filter(mintRows, new Predicate<MintRow>() {
      @Override
      public boolean apply(@Nullable MintRow input) {
        return LOAD_MONTH.toInterval().contains(input.getDate());
      }
    });

    Ordering<MintRow> myOrdering = new Ordering<MintRow>() {
      @Override
      public int compare(MintRow left, MintRow right) {
        return left.getDate().compareTo(right.getDate());
      }
    };

    mintRows = myOrdering.immutableSortedCopy(mintRows);

    mintRows = Iterables.filter(mintRows, new Predicate<MintRow>() {
      @Override
      public boolean apply(@Nullable MintRow input) {
        String account = input.getAccountName();
        switch (account) {
          case "Amex":
          case "Marty Checking":
          case "BOFA":
          case "BankOne CC Acct":
          case "Sally Checking":
          case "Target Credit Card":
            return true;
          case "IGNORE":
            return false;
          case "GOOGLE INC. 401(K) SAVINGS PLAN":
            return false;
        }

        throw new IllegalArgumentException("Unknown Account: " + account);
      }
    });

    Set<String> categoriesToIgnore = new HashSet<>();
    // Ignore things that are just shuffling cash around between my accounts
    categoriesToIgnore.add("NET");
    categoriesToIgnore.add("PAYCC");

    // Ignore Income
    categoriesToIgnore.add("MRINCOME");
    categoriesToIgnore.add("SRINCOME");

    mintRows = Iterables.filter(mintRows, new Predicate<MintRow>() {
      @Override
      public boolean apply(@Nullable MintRow input) {
        String category = input.getCategory();
        return !categoriesToIgnore.contains(category);
      }
    });

    Set<String> allCategories = new HashSet<>();
    for (Category x : Category.values()) {
      allCategories.add(x.name());
    }
    Map<Category, Collection<MintRow>> categorize = new HashMap<>();

    for (MintRow mr : mintRows) {
      String category = mr.getCategory();
      if (StringUtils.isBlank(category)) {
        // Don't worry about tiny stuff
        if (mr.isDebit() && mr.getFinancialAmount().doubleValue() > -5.00) {
          continue;
        } else if (mr.isCredit()) {
          continue;
        } else {
          log.warn("Unable to categorize: " + mr.toString());
          category = "OTHER";
        }
      }
      if (!allCategories.contains(category)) {
        log.warn("UNKNOWN CATEGORY: " + category + " Description: " + mr.getDescription());
        continue;
      }
      Category c = Category.valueOf(category);

      if (!categorize.containsKey(c)) {
        categorize.put(c, new ArrayList<>());
      }
      categorize.get(c).add(mr);
    }

    {
      long totalBudget = 0;
      for (Category c : Category.values()) {
        totalBudget = totalBudget + c.getAmount();
      }
      long expectedBudget = (long) (((double) totalBudget) * percentInMonth);
      long totalExpenses = 0;
      for (Collection<MintRow> x : categorize.values()) {
        totalExpenses = totalExpenses + sum(x).longValue();
      }
      totalExpenses = dontGoPositive(totalExpenses);
      // TODO(martinrose) : how to exclude mortgage from both sides of this
      long leftOver = expectedBudget + totalExpenses;

      if (leftOver > 0) {
        System.out.println(
            "ON TRACK OVERALL: Spent: " + currencyFormat
                .format(expensesExpressedPositive(totalExpenses)) + " of " + currencyFormat
                .format(totalBudget));
      } else {
        System.out.println(
            "NOT ON TRACK OVERALL: Spent: " + currencyFormat
                .format(expensesExpressedPositive(totalExpenses)) + " of " + currencyFormat
                .format(totalBudget));
      }
    }

    System.out.println("\n\n");

    StringBuilder onTrack = new StringBuilder();
    StringBuilder offTrack = new StringBuilder();
    StringBuilder overTrack = new StringBuilder();

    for (Category category : Category.values()) {
      Collection<MintRow> mints =
          categorize.containsKey(category) ? categorize.get(category) : Collections.emptyList();
      // This will be negative
      long categoryExpenses = dontGoPositive(sum(mints).longValue());
      // This will be positive
      long budgetExpected = (long) (((double) category.getAmount()) * percentInMonth);

      // Negative if overspent, positive if money left
      long remainingMoney = category.getAmount() + categoryExpenses;
      if (remainingMoney < 0) {
        overTrack.append("OVER BUDGET: ");
        describeCategoryState(category, categoryExpenses, overTrack);
        overTrack.append("\n");
        for (MintRow mr : categorize.get(category)) {
          overTrack.append("\t");
          describeMintRow(mr, overTrack);
          overTrack.append("\n");
        }
      } else {
        long remainingAgainstBudget = budgetExpected + categoryExpenses;
        if (remainingAgainstBudget < 0) {
          offTrack.append("NOT ON TRACK: ");
          describeCategoryState(category, categoryExpenses, offTrack);
          offTrack.append("\n");
          for (MintRow mr : categorize.get(category)) {
            offTrack.append("\t");
            describeMintRow(mr, offTrack);
            offTrack.append("\n");
          }
        } else {
          onTrack.append("ONTRACK: ");
          describeCategoryState(category, categoryExpenses, onTrack);
          onTrack.append("\n");
        }
      }
    }
    System.out.println(onTrack.toString());
    System.out.println(offTrack.toString());
    System.out.println(overTrack.toString());
  }

  private static void describeCategoryState(Category c, long actualSpend, StringBuilder sb) {
    long leftOver = (c.getAmount() + actualSpend);
    if (leftOver >= 0) {
      sb.append(
          "In " + c.name() + " spent " + currencyFormat
              .format(expensesExpressedPositive(dontGoPositive(actualSpend))) + " of "
              + currencyFormat.format(c.getAmount())
              + " with " +
              currencyFormat.format(leftOver) + " left.");
    } else {
      sb.append(
          "In " + c.name() + " spent " + currencyFormat
              .format(expensesExpressedPositive(dontGoPositive(actualSpend))) + " budgeted "
              + currencyFormat.format(c.getAmount()) + " over by " +
              currencyFormat.format(expensesExpressedPositive(leftOver)));
    }
  }

  private static void describeMintRow(MintRow mr, StringBuilder sb) {
//    if (mr.getFinancialAmount().longValue() > 0) {
//      sb.append("RETURN ");
//    }
    sb.append(
        currencyFormat.format(mr.getFinancialAmount().longValue()) + " : " + dtf.print(mr.getDate()) + " : " + mr
            .getDescription());

  }

  private static BigDecimal sum(Iterable<MintRow> mintRows) {
    BigDecimal sum = BigDecimal.ZERO;

    for (MintRow mr : mintRows) {
      sum = sum.add(mr.getFinancialAmount());
    }
    return sum;
  }

  private static long dontGoPositive(long value) {
    if (value > 0) {
      return 0;
    }
    return value;
  }

  private static long expensesExpressedPositive(long value) {
    return value * -1;
  }
}
