package com.mrose.mastermind;


import com.google.common.collect.ImmutableList;
import java.util.Iterator;

/**
 * TODO(martinrose) : Add Documentation
 */
public class Solution implements Iterable<Colors> {

  Colors first;
  Colors second;
  Colors third;
  Colors fourth;

  public Solution(Colors first, Colors second, Colors third, Colors fourth) {
    this.first = first;
    this.second = second;
    this.third = third;
    this.fourth = fourth;
  }

  @Override
  public Iterator<Colors> iterator() {
    return ImmutableList.of(first, second, third, fourth).iterator();
  }

  public ImmutableList<Colors> asList() {
    return ImmutableList.of(first, second, third, fourth);
  }

  public int countColors(Colors c) {
    return (first == c ? 1 : 0) + (second == c ? 1 : 0) + (third == c ? 1 : 0) + (fourth == c ? 1
        : 0);
  }
}
