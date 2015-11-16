package com.mrose.mint;

/**
 * TODO(martinrose) : Add Documentation
 */
public enum Category {
  CHARITY(100, true, true),
  TRAVEL(1000, false, false),
  MISC(250, true, true),
  MORTGAGE(3500, false, true),
  SALLYSP(200, true, true),
  GROCERY(1200, true, true),
  KIDS(500, true, true),
  BIGBOX(1000, true, true),
  OTHER(0, true, true),
  CAR(300, true, true),
  MARTYSP(150, true, true),
  HOME(50, true, true),
  UTILITY(650, true, true),
  CHILDCARE(1000, true, true),
  SAVING(200, false, true),
  ENTERTAIN(600, true, true),
  GIFT(400, false, false),
  MEDICAL(400, true, true),
  INSURANCE(125, false, false),
  HAIRCUT(60, true, true),
  TUITION(300, false, true),
  CASH(160, true, true),
  CTA(20, false, true);

  private boolean isMonthlySmooth;
  private boolean isMonthBased;
  private long amount;

  Category(long amount, boolean isMonthlySmooth, boolean isMonthBased) {
    this.isMonthlySmooth = isMonthlySmooth;
    this.isMonthBased = isMonthBased;
    this.amount = amount;
  }

  public long getAmount() {
    return amount;
  }
}
