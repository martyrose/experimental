package com.mrose.mastermind;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;

/**
 * TODO(martinrose) : Add Documentation
 */
public class StartGame {

  public static void main(String[] args) throws IOException {
    String line = null;
    BufferedReader br = new BufferedReader(
        new InputStreamReader(System.in, StandardCharsets.UTF_8));

    boolean exit = false;
    System.out.printf("command please: ");
    while (!exit && (line = br.readLine()) != null) {
      line = StringUtils.lowerCase(line);
      line = StringUtils.trimToEmpty(line);
      switch (line) {
        case "exit":
          System.out.printf("Later%n");
          exit = true;
          break;
      }
      System.out.printf("command please: ");
    }
  }
}
