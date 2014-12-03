package com.mrose.financial;

import com.google.common.base.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.YearMonth;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by martinrose on 10/29/14.
 */
class MonthFlatten implements
    Function<Entry<Category, Map<YearMonth, Integer>>, Pair<Category, Integer>> {
  private final YearMonth month;
  public MonthFlatten(YearMonth month) {
    this.month = month;
  }

  @Override
  public Pair<Category, Integer> apply(
      Entry<Category, Map<YearMonth, Integer>> input) {
    return Pair.of(input.getKey(), input.getValue().get(month));
  }
}
