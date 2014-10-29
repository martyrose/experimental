package com.mrose.financial;

import com.google.common.base.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.YearMonth;
import java.math.BigInteger;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by martinrose on 10/29/14.
 */
public class MonthFlatten implements
    Function<Entry<Category, Map<YearMonth, BigInteger>>, Pair<Category, BigInteger>> {
  private final YearMonth month;
  public MonthFlatten(YearMonth month) {
    this.month = month;
  }

  @Override
  public Pair<Category, BigInteger> apply(
      Entry<Category, Map<YearMonth, BigInteger>> input) {
    return Pair.of(input.getKey(), input.getValue().get(month));
  }
}
