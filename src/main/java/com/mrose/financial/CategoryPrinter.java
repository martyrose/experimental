package com.mrose.financial;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by martinrose on 10/29/14.
 */
class CategoryPrinter {
  private static final int ZERO = 0;
  private static final char SEP = ',';
  // http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("MMM yyyy");

  private final PrintStream stream;

  public CategoryPrinter(PrintStream stream) {
    this.stream = stream;
  }

  public void print(Map<Category, Map<YearMonth, Integer>> data, List<YearMonth> months) {
    List<Entry<Category, Map<YearMonth, Integer>>> entries = new ArrayList<>(data.entrySet());

    final Function<Map<YearMonth, Integer>, Integer> summer = new MapSummation<>();

    // sort them by amount
    Collections.sort(entries, new Comparator<Entry<Category, Map<YearMonth, Integer>>>() {
      @Override
      public int compare(Entry<Category, Map<YearMonth, Integer>> o1,
          Entry<Category, Map<YearMonth, Integer>> o2) {
        Integer total1 = summer.apply(o1.getValue());
        Integer total2 = summer.apply(o2.getValue());
        return total1.compareTo(total2);
      }
    });

    stream.println(formatHeaderRow(months));
    for (Entry<Category, Map<YearMonth, Integer>> e : entries) {
      stream.println(formatDataRow(e, months));
    }
  }

  private static String formatDataRow(Entry<Category, Map<YearMonth, Integer>> e,
      List<YearMonth> months) {
    List<String> cells = new LinkedList<>();
    cells.add(e.getKey().name());
    cells.add(String.valueOf(e.getKey().budget()));

    for (YearMonth month : months) {
      if (e.getValue().containsKey(month)) {
        cells.add(e.getValue().get(month).toString());
      } else {
        cells.add(String.valueOf(ZERO));
      }
    }
    return Joiner.on(SEP).join(cells);
  }

  private static String formatHeaderRow(List<YearMonth> months) {
    // Print out header rows
    List<String> cells = new LinkedList<>();
    cells.add("category");
    cells.add("budget");

    for (YearMonth month : months) {
      cells.add(dtf.print(month));
    }
    return Joiner.on(SEP).join(cells);
  }
}
