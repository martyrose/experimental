package com.mrose.mint;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nullable;

/**
 * TODO(martinrose) : Add Documentation
 */
public enum Category {
  CHARITY(100, false, true, false),
  TRAVEL(1000, false, false, false),
  MISC(250, true, true, false),
  MORTGAGE(3500, false, true, false),
  SALLYSP(200, true, true, false),
  GROCERY(1200, true, true, false),
  KIDS(500, true, true, false),
  BIGBOX(1000, true, true, false),
  OTHER(0, true, true, false),
  CAR(300, false, true, false),
  MARTYSP(150, true, true, false),
  HOME(50, true, true, false),
  UTILITY(650, false, true, false),
  CHILDCARE(1000, true, true, false),
  SAVING(200, false, true, false),
  ENTERTAIN(600, true, true, false),
  GIFT(400, false, false, false),
  MEDICAL(400, false, true, false),
  INSURANCE(125, false, false, false),
  HAIRCUT(60, false, true, false),
  TUITION(300, false, true, false),
  CASH(160, true, true, false),
  CTA(20, false, true, false);

  private static Supplier<Set<String>> allCategoryNames =
      Suppliers.memoize(
          new Supplier<Set<String>>() {
            @Override
            public Set<String> get() {
              return ImmutableSet.copyOf(
                  Iterables.transform(
                      Arrays.asList(Category.values()),
                      new Function<Category, String>() {
                        @Override
                        public String apply(Category input) {
                          return input.name();
                        }
                      }));
            }
          });

  private boolean isMonthlySmooth;
  private boolean isMonthBased;
  private boolean isIncome;
  private long amount;

  Category(long monthlyAmount, boolean isMonthlySmooth, boolean isMonthBased, boolean isIncome) {
    this.isMonthlySmooth = isMonthlySmooth;
    this.isMonthBased = isMonthBased;
    this.isIncome = isIncome;
    this.amount = monthlyAmount;
  }

  public static boolean contains(String n) {
    return allCategoryNames.get().contains(n);
  }

  public static Set<Category> allCategories() {
    return EnumSet.allOf(Category.class);
  }

  public static Set<Category> allExpenses() {
    return Sets.filter(
        allCategories(),
        new Predicate<Category>() {
          @Override
          public boolean apply(@Nullable Category input) {
            return !input.isIncome;
          }
        });
  }

  public static Set<Category> allMonthlyExpenses() {
    return Sets.filter(
        allCategories(),
        new Predicate<Category>() {
          @Override
          public boolean apply(@Nullable Category input) {
            return !input.isIncome && input.isMonthBased;
          }
        });
  }

  public static Set<Category> allMonthlySmoothExpenses() {
    return Sets.filter(
        allCategories(),
        new Predicate<Category>() {
          @Override
          public boolean apply(@Nullable Category input) {
            return !input.isIncome && input.isMonthBased && input.isMonthlySmooth;
          }
        });
  }

  public static Set<Category> excludingWhat(Set<Category> c) {
    return Sets.filter(
        allExpenses(),
        new Predicate<Category>() {
          @Override
          public boolean apply(Category input) {
            return !c.contains(input);
          }
        });
  }

  public static SortedSet<Category> sortByAmount(Set<Category> c) {
    return ImmutableSortedSet.copyOf(new BudgetValueOrdering().reversed(), c);
  }

  public long getAmount() {
    return amount;
  }

  static class BudgetValueOrdering extends Ordering<Category> {
    @Override
    public int compare(@Nullable Category left, @Nullable Category right) {
      if (left.amount != right.amount) {
        return Long.compare(left.amount, right.amount);
      } else {
        return left.name().compareTo(right.name());
      }
    }
  }
}
