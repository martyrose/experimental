package com.mrose.mint;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import javax.annotation.Nullable;

/**
 * TODO(martinrose) : Add Documentation
 */
public class CSVPredicate implements Predicate<String[]> {

  private final Predicate<String[]> all;

  public CSVPredicate() {
    this.all = Predicates.and(new HeaderPredicate());
  }

  @Override
  public boolean apply(@Nullable String[] input) {
    return all.apply(input);
  }

  static class HeaderPredicate implements Predicate<String[]> {
    @Override
    public boolean apply(@Nullable String[] input) {
      // TODO(martinrose) : Fix
      return !input[0].equals("Date");
    }
  }
}
