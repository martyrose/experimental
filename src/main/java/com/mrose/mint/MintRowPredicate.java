package com.mrose.mint;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;

import org.joda.time.Interval;
import org.joda.time.ReadableInterval;

import javax.annotation.Nullable;

/**
 * TODO(martinrose) : Add Documentation
 */
public class MintRowPredicate implements Predicate<MintRow> {
  private final ImmutableSet<String> categoriesToIngore =
      ImmutableSet.of("NET", "PAYCC");
  private final Predicate<MintRow> predicates;

  public MintRowPredicate(ReadableInterval dateInterval) {
    this.predicates =
        Predicates.and(
            new DateRangePredicate(dateInterval),
            new AccountPredicate(),
            Predicates.not(new IgnoreCategoryPredicate(categoriesToIngore)));
  }

  @Override
  public boolean apply(@Nullable MintRow input) {
    return predicates.apply(input);
  }

  static class DateRangePredicate implements Predicate<MintRow> {

    private final ReadableInterval relevantInterval;

    public DateRangePredicate(ReadableInterval relevantInterval) {
      this.relevantInterval = relevantInterval;
    }

    @Override
    public boolean apply(@Nullable MintRow input) {
      return relevantInterval.contains(input.getDate());
    }
  }

  static class AccountPredicate implements Predicate<MintRow> {

    @Override
    public boolean apply(@Nullable MintRow input) {
      String account = input.getAccountName();
      switch (account) {
        case "AMEX":
        case "MARTYCHECKING":
        case "BOFA":
        case "BANKONECCACCT":
        case "SALLYCHECKING":
        case "CHASECC9478":
        case "TARGETCREDITCARD":
          return true;
        case "FAMILYSAVINGS":
        case "IGNORE":
          return false;
        case "GOOGLE401K":
          return false;
      }

      throw new IllegalArgumentException("Unknown Account: " + account);
    }
  }

  static class IgnoreCategoryPredicate implements Predicate<MintRow> {

    private final ImmutableSet<String> categoriesToIgnore;

    public IgnoreCategoryPredicate(ImmutableSet<String> categoriesToIgnore) {
      this.categoriesToIgnore = categoriesToIgnore;
    }

    @Override
    public boolean apply(@Nullable MintRow input) {
      return categoriesToIgnore.contains(input.getCategory());
    }
  }
}
