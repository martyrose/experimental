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
      if(account.endsWith("IGNORE")) {
        return false;
      }
      switch (account) {
        case "CASH":
        case "AMEX":
        case "BLUECASHPREFERRED":
        case "BLUECASHPREFERRED-SUPPLEMENTARYACCOUNT":
        case "MARTYCHECKING":
        case "BOFA":
        case "BANKONECCACCT":
        case "SALLYCHECKING":
        case "CHASECC9478":
        case "AADVANTAGEAVIATOR":
        case "TARGETCREDITCARD":
        case "NORDSTROM":
        case "BANANA":
        case "ATHLETA":
        case "TJX_REWARDS":
          return true;
        case "BUSINESSADVANTAGECASHREWARDS":
        case "FAMILYSAVINGS":
        case "HSBCDIRECTSAVINGS":
        case "IGNORE":
        case "2425WWINONAST":
        case "XXXXXX7674":
        case "GOOGLE401K":
        case "AMERIPRISEONEACCT":
        case "CREDITCARDACCOUNT":
          return false;
      }

      return true;
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
