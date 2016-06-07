package com.mrose.dice;

/**
 * Created by mrose on 5/20/16.
 */
public class Dice {
  private final Iterable<Die> dice;

  public Dice(Iterable<Die> dice) {
    this.dice = dice;
  }

  public int roll() {
    int total = 0;
    for(Die d: dice) {
      total += d.roll();
    }
    return total;
  }
}
