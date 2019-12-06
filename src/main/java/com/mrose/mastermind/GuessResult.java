package com.mrose.mastermind;

import com.google.common.base.Objects;

/**
 * TODO(martinrose) : Add Documentation
 */
public class GuessResult {
  int red;
  int white;

  public GuessResult(int red, int white) {
    this.red = red;
    this.white = white;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuessResult that = (GuessResult) o;
    return red == that.red &&
        white == that.white;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(red, white);
  }
}
