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
    Solution guessSolution = new Solution(Colors.BLACK, Colors.BLACK, Colors.PINK, Colors.PINK);
    GuessResult gr = SolutionUtils.calculate(guessSolution, guessSolution);

    assertThat(gr.red).isEqualTo(4);
    assertThat(gr.white).isEqualTo(0);
  }


  @Test
  public void testExact_2() {
    Solution guessSolution = new Solution(Colors.BLACK, Colors.BLACK, Colors. BLACK, Colors.BLACK);

    GuessResult gr = SolutionUtils.calculate(guessSolution, guessSolution);

    assertThat(gr.red).isEqualTo(4);
    assertThat(gr.white).isEqualTo(0);
  }

  @Test
  public void test3red() {
    Solution guessSolution = new Solution(Colors.BLACK, Colors.BLACK, Colors. BLACK, Colors.BLACK);
    Solution s = new Solution(Colors.BLACK, Colors.BLACK, Colors.PINK, Colors.BLACK);

    GuessResult gr = SolutionUtils.calculate(s, guessSolution);

    assertThat(gr.red).isEqualTo(3);
    assertThat(gr.white).isEqualTo(0);
  }

  @Test
  public void test3red_onewhite() {
    Solution guessSolution = new Solution(Colors.BLACK, Colors.BLACK, Colors. BLACK, Colors.PINK);
    Solution s = new Solution(Colors.BLACK, Colors.BLACK, Colors.PINK, Colors.RED);

    GuessResult gr = SolutionUtils.calculate(s, guessSolution);

    assertThat(gr.red).isEqualTo(2);
    assertThat(gr.white).isEqualTo(1);
  }


  @Test
  public void test5() {
    Solution guessSolution = new Solution(Colors.BLACK, Colors.RED, Colors. YELLOW, Colors.PINK);
    Solution s = new Solution(Colors.GREEN, Colors.BLACK, Colors.PINK, Colors.RED);

    GuessResult gr = SolutionUtils.calculate(s, guessSolution);

    assertThat(gr.red).isEqualTo(0);
    assertThat(gr.white).isEqualTo(3);
  }

}
