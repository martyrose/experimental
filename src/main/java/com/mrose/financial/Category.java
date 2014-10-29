package com.mrose.financial;

import com.google.common.base.MoreObjects;
import java.util.Objects;

/**
 * Created by martinrose on 10/29/14.
 */
class Category {
  private final String name;
  private final int budget;

  Category(String name, int budget) {
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

  public int budget() {
    return budget;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", name).add("budget", budget).toString();
  }
}
