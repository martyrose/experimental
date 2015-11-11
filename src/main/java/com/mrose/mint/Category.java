package com.mrose.mint;

/**
 * TODO(martinrose) : Add Documentation
 */
public enum Category {
  CHARITY(100), TRAVEL(1000), MISC(250), MORTGAGE(3500), SALLYSP(200), GROCERY(1200), KIDS(
      500), BIGBOX(1000), OTHER(0), CAR(300), MARTYSP(150), HOMEOP(700), CHILDCARE(1000), SAVING(
      200), ENTERTAIN(600), GIFT(400), MEDICAL(400), INSURANCE(125), HAIRCUT(60), TUITION(
      300), CASH(160), CTA(20);
  private long amount;

  Category(long amount) {
    this.amount = amount;
  }

  public long getAmount() {
    return amount;
  }
}
