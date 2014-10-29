package com.mrose.financial;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Created by martinrose on 10/29/14.
 */
class Category {
  private final String name;
  private final BigDecimal budget;

  Category(String name, BigDecimal budget) {
    this.name = name;
    this.budget = budget;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Category other = (Category) obj;
    return Objects.equals(this.name, other.name);
  }

  public String name() {
    return name;
  }

  public BigDecimal budget() {
    return budget;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
