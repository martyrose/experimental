package com.mrose.financial;

import com.google.common.base.Function;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by martinrose on 10/29/14.
 */
class MapSummation<K> implements Function<Map<K, BigDecimal>, BigDecimal>  {
  @Override
  public BigDecimal apply(Map<K, BigDecimal> input) {
    BigDecimal running = BigDecimal.ZERO;
    for( BigDecimal bd: input.values()) {
      running = running.add(bd);
    }
    return running;
  }
}
