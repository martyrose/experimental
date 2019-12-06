package com.mrose.mastermind;

import com.google.common.collect.AbstractIterator;
import java.util.Iterator;

/**
 * TODO(martinrose) : Add Documentation
 */
public class Guess {
  Solution solution;
  GuessResult result;

  public Guess(Solution solution) {
    this.solution = solution;
  }
}
