package com.mrose.mastermind;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;


@RunWith(JUnit4.class)
public class TestSolutionUtils {

  @Test
  public void testExact_1() {
    Solution guessSolution = new Solution(Colors.BLACK, Colors.BLACK, Colors. BLUE, Colors.BLUE);
    Guess g = new Guess(guessSolution);

    GuessResult gr = SolutionUtils.calculate(guessSolution, g);

    assertThat(gr.red).isEqualTo(4);
    assertThat(gr.white).isEqualTo(0);
  }


  @Test
  public void testExact_2() {
    Solution guessSolution = new Solution(Colors.BLACK, Colors.BLACK, Colors. BLACK, Colors.BLACK);
    Guess g = new Guess(guessSolution);

    GuessResult gr = SolutionUtils.calculate(guessSolution, g);

    assertThat(gr.red).isEqualTo(4);
    assertThat(gr.white).isEqualTo(0);
  }

  @Test
  public void test3red() {
    Solution guessSolution = new Solution(Colors.BLACK, Colors.BLACK, Colors. BLACK, Colors.BLACK);
    Guess g = new Guess(guessSolution);
    Solution s = new Solution(Colors.BLACK, Colors.BLACK, Colors.BLUE, Colors.BLACK);

    GuessResult gr = SolutionUtils.calculate(s, g);

    assertThat(gr.red).isEqualTo(3);
    assertThat(gr.white).isEqualTo(0);
  }

  @Test
  public void test3red_onewhite() {
    Solution guessSolution = new Solution(Colors.BLACK, Colors.BLACK, Colors. BLACK, Colors.BLUE);
    Guess g = new Guess(guessSolution);
    Solution s = new Solution(Colors.BLACK, Colors.BLACK, Colors.BLUE, Colors.RED);

    GuessResult gr = SolutionUtils.calculate(s, g);

    assertThat(gr.red).isEqualTo(2);
    assertThat(gr.white).isEqualTo(1);
  }

}
