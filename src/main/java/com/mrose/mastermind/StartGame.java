package com.mrose.mastermind;

import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.StringUtils;

/**
 * TODO(martinrose) : Add Documentation
 */
public class StartGame {

  public static void main(String[] args) throws IOException {
    String line = null;
    BufferedReader br = new BufferedReader(
        new InputStreamReader(System.in, StandardCharsets.UTF_8));

    List<PossibleSolution> allSolutions = new ArrayList<>();
    for (Colors c1 : Colors.values()) {
      for (Colors c2 : Colors.values()) {
        for (Colors c3 : Colors.values()) {
          for (Colors c4 : Colors.values()) {
            Solution s = new Solution(c1, c2, c3, c4);
            PossibleSolution ps = new PossibleSolution(s);
            allSolutions.add(ps);
          }
        }
      }
    }

    boolean exit = false;
    System.out.printf("command please: ");
    while (!exit && (line = br.readLine()) != null) {
      line = StringUtils.lowerCase(line);
      line = StringUtils.trimToEmpty(line);
      String[] command = StringUtils.split(line);
      switch (command[0]) {
        case "possible":

        case "makeguess":
          Solution s = makeGuess(allSolutions);
          System.out
              .printf("%s%s%s%s%n", s.first.toString().charAt(0), s.second.toString().charAt(0), s.third.toString().charAt(0),
                  s.fourth.toString().charAt(0));
          break;
        case "processguess":
          int excluded = processguess(allSolutions, command[1], command[2], command[3]);
          System.out.printf("Excluded %,d%n", excluded);
          break;
        case "exit":
          System.out.printf("Later%n");
          exit = true;
          break;
      }
      System.out.printf("command please: ");
    }
  }

  private static final Random RAND = new Random();

  private static Solution makeGuess(List<PossibleSolution> possibleSolutions) {
    ImmutableList<PossibleSolution> availableSolutions = possibleSolutions.stream()
        .filter(possibleSolution -> !possibleSolution.excluded).collect(
            ImmutableList.toImmutableList());
    return availableSolutions.get(RAND.nextInt(availableSolutions.size())).solution;
  }

  private static int processguess(List<PossibleSolution> possibleSolutions, String guess, String red, String white) {
    if(guess.length() != 4) {
      System.out.printf("Invalid color length%n");
      return 0;
    }
    Colors one = toColor(guess.charAt(0));
    Colors two = toColor(guess.charAt(1));
    Colors three = toColor(guess.charAt(2));
    Colors four = toColor(guess.charAt(3));

    if(one == null || two == null|| three == null||four==null) {
      System.out.printf("Invalid color identifier%n");
      return 0;
    }

    Solution guessedSolution = new Solution(one,two,three,four);
    GuessResult guessResult = new GuessResult(Integer.parseInt(red), Integer.parseInt(white));

    int excludedCount = 0;
    for(PossibleSolution ps: possibleSolutions) {
      if(!SolutionUtils.calculate(ps.solution, guessedSolution).equals(guessResult)) {
        ps.exclude();
        excludedCount++;
      }
    }
    return excludedCount;
  }

  private static Colors toColor(char color) {
    for(Colors c:Colors.values()) {
      if( c.name().toLowerCase().charAt(0) == color) {
        return c;
      }
    }
    System.out.printf("Invalid Color %s%n", color);
    return null;
  }
}
