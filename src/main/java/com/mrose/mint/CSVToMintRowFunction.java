package com.mrose.mint;

import com.google.common.base.Function;

import javax.annotation.Nullable;

/**
 * TODO(martinrose) : Add Documentation
 */
public class CSVToMintRowFunction implements Function<String[], MintRow> {
  @Override
  public MintRow apply(@Nullable String[] input) {
    return new MintRow(input);
  }
}
