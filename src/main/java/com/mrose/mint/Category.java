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
  // Income
  MRINCOME(27750, false, true, true),
  SRINCOME(0, false, true, true),
  // Expenses
  BIGBOX(2667, true, true, false),
  CAR(500, false, false, false),
  CASH(85, true, true, false),
  CHARITY(208, false, true, false),
  CHILDCARE(1930, true, true, false),
  CTA(0, false, true, false),
  ENTERTAIN(750, true, true, false),
  GIFT(0, false, false, false),
  GROCERY(1375, true, true, false),
  HAIRCUT(125, false, true, false),
  HOME(500, true, true, false),
  INSURANCE(450, false, false, false),
  KIDS(800, true, true, false),
  MARTYSP(200, true, true, false),
  MEDICAL(833, false, true, false),
  MISC(150, true, true, false),
  MORTGAGE(4300, false, true, false),
  ONETIME(1, false, false, false),
  OTHER(1, true, true, false),
  SALLYSP(167, true, true, false),
  TRAVEL(1, false, false, false),
  TUITION(850, false, true, false),
  UTILITY(1250, false, true, false);

  private static Supplier<Set<String>> allCategoryNames =
      Suppliers.memoize(
          () -> ImmutableSet.copyOf(
              Iterables.transform(
                  Arrays.asList(Category.values()),
                  Enum::name)));

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

  public static Set<Category> allExpensesExcludingOneTime() {
    return Sets.filter(
        allCategories(),
        new Predicate<Category>() {
          @Override
          public boolean apply(@Nullable Category input) {
            return !input.isIncome && input != Category.ONETIME && input != Category.TRAVEL;
          }
        });
  }

  public static Set<Category> allExpensesIncludingOneTime() {
    return Sets.filter(
        allCategories(),
        new Predicate<Category>() {
          @Override
          public boolean apply(@Nullable Category input) {
            return !input.isIncome;
          }
        });
  }

  public static Set<Category> allIncome() {
    return Sets.filter(
        allCategories(),
        new Predicate<Category>() {
          @Override
          public boolean apply(@Nullable Category input) {
            return input.isIncome;
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

  public static Set<Category> allMultiMonthExpenses() {
    return Sets.filter(
        allCategories(),
        new Predicate<Category>() {
          @Override
          public boolean apply(@Nullable Category input) {
            return !input.isIncome && !input.isMonthBased;
          }
        });
  }

  public static Set<Category> excludingWhat(Set<Category> c) {
    return Sets.filter(
        allExpensesIncludingOneTime(),
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
    return getAmount(1);
  }

  public long getAmount(int months) {
    return amount * months;
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
/**
 // Income
 MRINCOME(12165, false, true, true),
 SRINCOME(0, false, true, true),
 // Expenses
 BIGBOX(1000, true, true, false),
 CAR(300, false, false, false),
 CASH(160, true, true, false),
 CHARITY(100, false, true, false),
 CHILDCARE(1000, true, true, false),
 CTA(20, false, true, false),
 ENTERTAIN(600, true, true, false),
 GIFT(400, false, false, false),
 GROCERY(1200, true, true, false),
 HAIRCUT(60, false, true, false),
 HOME(50, true, true, false),
 INSURANCE(125, false, false, false),
 KIDS(500, true, true, false),
 MARTYSP(150, true, true, false),
 MEDICAL(400, false, true, false),
 MISC(250, true, true, false),
 MORTGAGE(3500, false, true, false),
 OTHER(0, true, true, false),
 SALLYSP(200, true, true, false),
 SAVING(200, false, true, false),
 TRAVEL(1000, false, false, false),
 TUITION(300, false, true, false),
 UTILITY(650, false, true, false);
*/
