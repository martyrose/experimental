package com.mrose.dice;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.builder.ToStringBuilder;
import java.util.Arrays;

/**
 * Created by mrose on 5/20/16.
 */
public class Game {
  private final Iterable<GamePredicate<Integer>> conditions;

  public Game(Predicate<Integer>... conditions) {
    Iterable<GamePredicate<Integer>> lazyConditions = Iterables.transform(Arrays.asList(conditions),
        new Function<Predicate<Integer>, GamePredicate<Integer>>() {
          @Override
          public GamePredicate<Integer> apply(Predicate<Integer> input) {
            return new GamePredicate<>(input);
          }
        });
    this.conditions = Lists.newArrayList(lazyConditions);
  }

  public boolean newRoll(int i) {
    Optional<GamePredicate<Integer>> markable = Iterables
        .tryFind(conditions, Predicates.and(new UnMetPredicate<>(), new CanAccept<>(i)));
    if (markable.isPresent()) {
      markable.get().mark();
      return true;
    }
    return false;
  }

  public boolean complete() {
    return Iterables.isEmpty(Iterables.filter(conditions, new UnMetPredicate<>()));
  }

  private static class UnMetPredicate<T> implements Predicate<GamePredicate<T>> {

    @Override
    public boolean apply(GamePredicate<T> input) {
      return !input.hasBeenMet();
    }
  }

  private static class CanAccept<T> implements Predicate<GamePredicate<T>> {

    private final T value;

    public CanAccept(T value) {
      this.value = value;
    }

    @Override
    public boolean apply(GamePredicate<T> input) {
      return input.meets(value);
    }
  }

  private static class GamePredicate<T> implements Predicate<T> {

    private final Predicate<T> predicate;
    private boolean beenMet = false;

    public GamePredicate(Predicate<T> predicate) {
      this.predicate = predicate;
    }

    @Override
    public boolean apply(T input) {
      return predicate.apply(input);
    }

    public boolean meets(T input) {
      return apply(input);
    }

    public boolean hasBeenMet() {
      return beenMet;
    }

    public void mark() {
      beenMet = true;
    }

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this);
    }
  }
}
