package com.mrose.financial;

import com.google.common.base.Function;
import java.util.Map;

/**
 * Created by martinrose on 10/29/14.
 */
class MapSummation<K> implements Function<Map<K, Integer>, Integer>  {
  @Override
  public Integer apply(Map<K, Integer> input) {
    Integer running = 0;
    for( Integer bd: input.values()) {
      running = running + bd;
    }
    return running;
  }
}
