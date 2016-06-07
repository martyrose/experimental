package com.mrose.dice;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by mrose on 5/20/16.
 */
public class Die {
  private final int min;
  private final int max;

  public Die(int min, int max) {
    this.min = min;
    this.max = max;
  }

  public int roll() {
    return ThreadLocalRandom.current().nextInt(min, max+1);
  }
}
