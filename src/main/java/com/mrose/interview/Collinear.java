package com.mrose.interview;

import static com.google.common.truth.Truth.assertThat;

import com.mrose.financial.LoadFinancialData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO(martinrose) : Add Documentation
 */
public class Collinear {

  private static final Logger log = LoggerFactory.getLogger(LoadFinancialData.class);

  public static void main(String[] args) {
    // Straight lines
    // diagonal
    assertThat(isCollinear(0, 0, 1, 1, 2, 2)).isTrue();
    // vertical
    assertThat(isCollinear(0, 0, 0, 1, 0, 2)).isTrue();
    // horizontal
    assertThat(isCollinear(0, 0, 1, 0, 2, 0)).isTrue();

    // Disjoint lines
    // diagonal
    assertThat(isCollinear(0, 0, 1, 1, 3, 2)).isFalse();
    // vertical
    assertThat(isCollinear(0, 0, 0, 1, 1, 2)).isFalse();
    // horizontal
    assertThat(isCollinear(0, 0, 1, 0, 2, 1)).isFalse();

    // Scatter
    assertThat(isCollinear(0, 0, 3, 4, 49, 50)).isFalse();
  }

  private static boolean isCollinear(int x1, int y1, int x2, int y2, int x3, int y3) {
    return (y2 - y1) * (x3 - x1) == (y3 - y1) * (x2 - x1);
  }
}
