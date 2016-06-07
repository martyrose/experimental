package com.mrose.dice;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * Created by mrose on 5/20/16.
 */
public class PlayGame {

  public static void main(String[] args) {
    Die d = new Die(0, 9);
    Dice dice = new Dice(ImmutableList.of(d, d));

    Predicate[] gameRules = new Predicate[]{
        new GameSlot(0, 1, 17, 18),
        new GameSlot(2, 3, 4, 5, 6),
        new GameSlot(12, 13, 14, 15, 16),

        new GameSlot(7, 8),
        new GameSlot(9),
        new GameSlot(10, 11)
    };

    int[] completesIn = new int[1000];
    for (int i = 0; i < 100000; i++) {
      Game g = new Game(gameRules);
      int rolls = 0;
      while (!g.complete()) {
        rolls++;
        g.newRoll(dice.roll());
      }
      completesIn[rolls] = completesIn[rolls] + 1;
    }

    System.out.println("50% of games done in " + findNthGameSlot(completesIn, 50000));
    System.out.println("90% of games done in " + findNthGameSlot(completesIn, 90000));
    System.out.println("95% of games done in " + findNthGameSlot(completesIn, 95000));

  }

  private static int findNthGameSlot(int[] slots, int numGames) {
    int total = 0;
    for (int i = 0; i < slots.length; i++) {
      total += slots[i];
      if (total >= numGames) {
        return i;
      }
    }
    return slots.length;
  }

  private static class GameSlot implements Predicate<Integer> {

    private Iterable<Integer> okayValues;

    public GameSlot(Integer... okayValues) {
      this.okayValues = Arrays.asList(okayValues);
    }

    @Override
    public boolean apply(@Nullable Integer input) {
      return Iterables.contains(okayValues, input);
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this).append("values", okayValues).toString();
    }
  }
}
