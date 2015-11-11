package com.mrose.mint;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
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
  private static final String FILE_PATH = "/tmp/mint.dat";
  private static final YearMonth LOAD_MONTH = new YearMonth(2015, DateTimeConstants.NOVEMBER);

  // 1/04/2012
  // http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");

  public static void main(String[] args) throws Exception {
    CSVReader reader = new CSVReader(new FileReader(FILE_PATH));
    Iterable<String[]> allRows = reader.readAll();
    double percentInMonth = ((double) DateTime.now().getDayOfMonth()) / 30.0d;

    System.out.println("Percent of Month Complete: " + percentInMonth);
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

    // Ignore Categories that don't make sense on a month to month basis
    categoriesToIgnore.add("TRAVEL");
    categoriesToIgnore.add("MORTGAGE");

    mintRows = Iterables.filter(mintRows, new Predicate<MintRow>() {
      @Override
      public boolean apply(@Nullable MintRow input) {
        String category = input.getCategory();
        return !categoriesToIgnore.contains(category);
      }
    });

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
      Category c = Category.valueOf(category);
      if (c == null) {
        log.warn("UNKNOWN CATEGORY: " + category);
        continue;
      }

      if (!categorize.containsKey(c)) {
        categorize.put(c, new ArrayList<>());
      }
      categorize.get(c).add(mr);
    }

    StringBuilder onTrack = new StringBuilder();
    StringBuilder offTrack = new StringBuilder();
    StringBuilder overTrack = new StringBuilder();

    for (Category category : Category.values()) {
      Collection<MintRow> mints =
          categorize.containsKey(category) ? categorize.get(category) : Collections.emptyList();
      // This will be negative
      BigDecimal monthToDate = sum(mints);
      // This will be positive
      long budgetExpected = (long) (((double) category.getAmount()) * percentInMonth);

      // Negative if overspent, positive if money left
      long remainingMoney = category.getAmount() + monthToDate.longValue();
      if (remainingMoney < 0) {
        overTrack.append(
            "OVEROVER: In " + category + " over by " + remainingMoney + " Spent " + monthToDate
                + " budget " + category.getAmount() + "\n");
      } else {
        long remainingAgainstBudget = budgetExpected + monthToDate.longValue();
        if (remainingAgainstBudget < 0) {
          offTrack.append(
              "NOT ON TRACK: In " + category + " there is " + remainingMoney + " left. Spent "
                  + monthToDate + " budget " + category.getAmount() + "\n");
          Collection<MintRow> entries = categorize.get(category);
          for (MintRow mr : entries) {
            offTrack.append(
                "\t" + mr.getFinancialAmount() + " : " + dtf.print(mr.getDate()) + " : " + mr
                    .getDescription() + "\n");
          }
        } else {
          onTrack.append("ONTRACK: In " + category + " there is " + remainingMoney + " left.\n");
        }
      }
    }
    System.out.println(onTrack.toString());
    System.out.println(offTrack.toString());
    System.out.println(overTrack.toString());
  }


  private static BigDecimal sum(Iterable<MintRow> mintRows) {
    BigDecimal sum = BigDecimal.ZERO;

    for (MintRow mr : mintRows) {
      sum = sum.add(mr.getFinancialAmount());
    }
    return sum;
  }
}
