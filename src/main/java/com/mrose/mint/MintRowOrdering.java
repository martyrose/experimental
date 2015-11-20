package com.mrose.mint;

import com.google.common.collect.Ordering;

/**
 * TODO(martinrose) : Add Documentation
 */
public class MintRowOrdering extends Ordering<MintRow> {
  @Override
  public int compare(MintRow left, MintRow right) {
    return left.getDate().compareTo(right.getDate());
  }
}
