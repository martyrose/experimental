package com.mrose.financial;

import com.google.common.base.Function;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by martinrose on 10/29/14.
 */
class PairToMap <K,V> implements Function<Iterable<Pair<K, V>>, Map<K,V>> {
  @Override
  public Map<K, V> apply(Iterable<Pair<K, V>> input) {
    Map<K, V> m = new HashMap<>();
    for(Pair<K,V> p : input) {
      m.put(p.getKey(), p.getValue());
    }
    return m;
  }
}
