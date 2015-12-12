package com.mrose.mint;

import org.joda.time.Chronology;
import org.joda.time.Interval;
import org.joda.time.base.AbstractInterval;
import org.joda.time.chrono.ISOChronology;

/**
 * TODO(martinrose) : Add Documentation
 */
public class FinancialPeriod extends AbstractInterval {
  private final Interval originalInterval;
  private final int months;

  public FinancialPeriod(Interval interval, int months) {
    this.originalInterval = interval;
    this.months = months;
  }
  public int getMonths() {
    return months;
  }

  @Override
  public Chronology getChronology() {
    return ISOChronology.getInstance();
  }

  @Override
  public long getStartMillis() {
    return originalInterval.getStartMillis();
  }

  @Override
  public long getEndMillis() {
    return originalInterval.getEndMillis();
  }
}
