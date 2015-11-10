package com.mrose.mint;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import au.com.bytecode.opencsv.CSVReader;

import com.mrose.financial.LoadFinancialData;

import org.apache.commons.lang3.StringUtils;
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
import java.util.HashMap;
import java.util.Map;

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

    Map<String, Collection<MintRow>> categorize = new HashMap<>();

    for (MintRow mr : mintRows) {
      String category = cleanup(StringUtils.substringAfterLast(mr.getDescription(), "-"));
      if (StringUtils.isBlank(category)) {
        // Don't worry about tiny stuff
        if (mr.isDebit() && mr.getFinancialAmount().doubleValue() > -5.00) {
          continue;
        } else if (mr.isCredit()) {
          continue;
        } else {
          log.warn("Unable to categorize: " + mr.toString());
          category="OTHER";
        }
      }

      categorize.putIfAbsent(category, new ArrayList<MintRow>());
      categorize.get(category).add(mr);
    }
    categorize.remove("NET");
    categorize.remove("MRINCOME");
    categorize.remove("SRINCOME");
    categorize.remove("PAYCC");
    for(String key: categorize.keySet()) {
      log.warn("category=" + key + " amount=" + sum(categorize.get(key)));
    }
  }

  private static String cleanup(String s) {
    return StringUtils.upperCase(StringUtils.trimToEmpty(s)).replaceAll("\\s+", "");
  }

  private static BigDecimal sum(Iterable<MintRow> mintRows) {
    BigDecimal sum = BigDecimal.ZERO;

    for(MintRow mr: mintRows) {
      sum = sum.add(mr.getFinancialAmount());
    }
    return sum;
  }
}
