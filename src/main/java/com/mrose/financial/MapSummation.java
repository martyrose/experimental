package com.mrose.financial;

import com.google.common.base.Function;
import java.math.BigInteger;
import java.util.Map;

/**
 * Created by martinrose on 10/29/14.
 */
class MapSummation<K> implements Function<Map<K, BigInteger>, BigInteger>  {
  @Override
  public BigInteger apply(Map<K, BigInteger> input) {
    BigInteger running = BigInteger.ZERO;
    for( BigInteger bd: input.values()) {
      running = running.add(bd);
    }
    return running;
  }
}
