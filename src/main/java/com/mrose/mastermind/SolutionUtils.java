package com.mrose.mastermind;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO(martinrose) : Add Documentation
 */
public class SolutionUtils {

  static GuessResult calculate(Solution actualSolution, Guess playerGuess) {
    int red = Streams
        .zip(Streams.stream(actualSolution), Streams.stream(playerGuess.solution),
            (colors, colors2) -> colors == colors2 ? 1 : 0).mapToInt((i) -> i).sum();

    int white = 0;
    ImmutableSet<Colors> colors = ImmutableSet.copyOf(playerGuess.solution.iterator());
    for (Colors c : colors) {
      white += actualSolution.countColors(c);
    }
    white -= red;
    return new GuessResult(red, white);
  }
}
