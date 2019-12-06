package com.mrose.mastermind;


/**
 * TODO(martinrose) : Add Documentation
 */
public class PossibleSolution {
  Solution solution;
  boolean excluded = false;

  public PossibleSolution(Solution s) {
    this.solution = s;
  }

  void exclude() {
    excluded = true;
  }
}
