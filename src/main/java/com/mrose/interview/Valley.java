package com.mrose.interview;

import com.mrose.financial.LoadFinancialData;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO(martinrose) : Add Documentation
 */
public class Valley {

  private static final Logger log = LoggerFactory.getLogger(LoadFinancialData.class);

  public static void main(String[] args) {
    int[] hills = new int[] {2, 4, 5, 3, 6, 4, 7, 3};
    Pair<Integer, Integer> largestHill = findLargestHill(hills);
  }


  private static Pair<Integer, Integer> findLargestHill(int[] hills) {
    return Pair.of(0,0);
  }
}
