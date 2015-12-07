package com.mrose.mint;


import com.google.common.base.Function;

/**
 * TODO(martinrose) : Add Documentation
 */
public class PartialMonthFunction implements Function<Long, Long> {
  private final double percentComplete;

  public PartialMonthFunction(double percentComplete) {
    this.percentComplete = percentComplete;
  }

  @Override
  public Long apply(Long input) {
    return ((long) (input.doubleValue() * percentComplete));
  }
}
