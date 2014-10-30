package com.mrose.financial;

/**
 * Created by martinrose on 10/30/14.
 */
class Journal {
  private final String desc1;
  private final Integer amount;

  Journal(String desc1, Integer amount) {
    this.desc1 = desc1;
    this.amount = amount;
  }

  public String desc1() {
    return desc1;
  }

  public Integer amount() {
    return amount;
  }
}
