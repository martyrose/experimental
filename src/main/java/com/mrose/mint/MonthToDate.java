package com.mrose.mint;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

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
import java.util.Map;

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
    ((DecimalFormat) currencyFormat)
        .setNegativePrefix(symbol + "-"); // or "-"+symbol if that's what you need
    ((DecimalFormat) currencyFormat).setNegativeSuffix("");
  }

  public static void emitSummary(
      Map<Category, Collection<MintRow>> data,
      Iterable<Category> categories,
      Function<Long, Long> exepectedCalculator,
      StringBuilder sb) {
    long totalBudget = 0;
    for (Category c : categories) {
      totalBudget = totalBudget + c.getAmount();
    }
    long expectedBudget = exepectedCalculator.apply(totalBudget);
    long totalExpenses = 0;
    for (Category c : categories) {
      Collection<MintRow> rows = data.get(c);
      if (rows != null) {
        totalExpenses = totalExpenses + sum(rows).longValue();
      }
    }
    totalExpenses = dontGoPositive(totalExpenses);
    long leftOver = expectedBudget + totalExpenses;

    if (leftOver > 0) {
      sb.append(
          "ON TRACK: Spent: "
              + currencyFormat.format(expensesExpressedPositive(totalExpenses))
              + " of "
              + currencyFormat.format(totalBudget)
              + "\n");
    } else {
      System.out.println(
          "NOT ON TRACK: Spent: "
              + currencyFormat.format(expensesExpressedPositive(totalExpenses))
              + " of "
              + currencyFormat.format(totalBudget)
              + "\n");
    }
  }

  public static void main(String[] args) throws Exception {
    CSVReader reader = new CSVReader(new FileReader(FILE_PATH));
    Iterable<String[]> allRows = reader.readAll();
    double percentInMonth = ((double) DateTime.now().getDayOfMonth()) / ((double) 30);

    System.out.println("Percent of Month Complete: " + percentFormat.format(percentInMonth));

    // Skip the header/prefix row
    allRows = Iterables.filter(allRows, new CSVPredicate());

    // Convert to MintRows
    Iterable<MintRow> mintRows = Iterables.transform(allRows, new CSVToMintRowFunction());
    // Sort them
    mintRows = new MintRowOrdering().immutableSortedCopy(mintRows);

    Predicate<MintRow> filter = new MintRowPredicate(LOAD_MONTH.toInterval());
    // Filter rows we don't care about
    mintRows = Iterables.filter(mintRows, filter);

    Map<Category, Collection<MintRow>> categorize = getCategoryCollectionMap(mintRows);
    System.out.println("Period: " + LOAD_MONTH.toString());
    {
      StringBuilder summary = new StringBuilder();
      emitSummary(categorize, Category.allExpenses(), Functions.identity(), summary);
      System.out.println("All Categories");
      System.out.println("Includes: " + Category.sortByAmount(Category.allExpenses()).toString());
      System.out.println("Excludes: " + Category.sortByAmount(Category.excludingWhat(Category.allExpenses())).toString());
      System.out.println(summary.toString());
    }
    {
      StringBuilder summary = new StringBuilder();
      emitSummary(categorize, Category.allMonthlyExpenses(), Functions.identity(), summary);
      System.out.println("Only Categories that are consistent month to month");
      System.out.println("Includes: " + Category.sortByAmount(Category.allMonthlyExpenses()).toString());
      System.out.println("Excludes: " + Category.sortByAmount(Category.excludingWhat(Category.allMonthlyExpenses())).toString());
      System.out.println(summary.toString());
    }
    {
      StringBuilder summary = new StringBuilder();
      emitSummary(
          categorize,
          Category.allMonthlySmoothExpenses(),
          new Function<Long, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable Long input) {
              return ((long) (input.doubleValue() * percentInMonth));
            }
          },
          summary);
      System.out.println("Prorated Smooth Monthly Expenses.");
      System.out.println("Includes: " + Category.sortByAmount(Category.allMonthlySmoothExpenses()).toString());
      System.out.println("Excludes: " + Category.sortByAmount(Category.excludingWhat(Category.allMonthlySmoothExpenses())).toString());
      System.out.println(summary.toString());
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

  private static Map<Category, Collection<MintRow>> getCategoryCollectionMap(
      Iterable<MintRow> mintRows) {
    Map<Category, Collection<MintRow>> categorize = new HashMap<>();
    for (MintRow mr : mintRows) {
      String categoryName = mr.getCategory();
      if (StringUtils.isBlank(categoryName)) {
        // Don't worry about tiny stuff
        log.warn("Unable to categorize: " + mr.toString());
        categoryName = "OTHER";
      }
      // This catches if i spell something wrong
      if (!Category.contains(categoryName)) {
        log.warn("UNKNOWN CATEGORY: " + categoryName + " Description: " + mr.getDescription());
        continue;
      }
      // Turn it into a category object
      Category c = Category.valueOf(categoryName);

      if (!categorize.containsKey(c)) {
        categorize.put(c, new ArrayList<>());
      }
      categorize.get(c).add(mr);
    }
    categorize.remove(Category.OTHER);
    return categorize;
  }

  private static void describeCategoryState(Category c, long actualSpend, StringBuilder sb) {
    long leftOver = (c.getAmount() + actualSpend);
    if (leftOver >= 0) {
      sb.append(
          "In "
              + c.name()
              + " spent "
              + currencyFormat.format(expensesExpressedPositive(dontGoPositive(actualSpend)))
              + " of "
              + currencyFormat.format(c.getAmount())
              + " with "
              + currencyFormat.format(leftOver)
              + " left.");
    } else {
      sb.append(
          "In "
              + c.name()
              + " spent "
              + currencyFormat.format(expensesExpressedPositive(dontGoPositive(actualSpend)))
              + " budgeted "
              + currencyFormat.format(c.getAmount())
              + " over by "
              + currencyFormat.format(expensesExpressedPositive(leftOver)));
    }
  }

  private static void describeMintRow(MintRow mr, StringBuilder sb) {
    //    if (mr.getFinancialAmount().longValue() > 0) {
    //      sb.append("RETURN ");
    //    }
    sb.append(
        currencyFormat.format(mr.getFinancialAmount().longValue())
            + " : "
            + dtf.print(mr.getDate())
            + " : "
            + mr.getDescription());
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
