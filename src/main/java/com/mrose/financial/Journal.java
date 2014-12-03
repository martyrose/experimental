package com.mrose.financial;

import org.joda.time.DateTime;

/**
 * Created by martinrose on 10/30/14.
 */
class Journal {
  private final String desc1;
  private final Integer amount;
  private final String category;
  private final DateTime dateTime;

  Journal(String desc1, Integer amount, String category, DateTime dateTime) {
    this.desc1 = desc1;
    this.amount = amount;
    this.category = category;
    this.dateTime = dateTime;
  }

  public String desc1() {
    return desc1;
  }

  public Integer amount() {
    return amount;
  }

  public String category() { return category; }

  public DateTime dttm() { return dateTime; }
}
