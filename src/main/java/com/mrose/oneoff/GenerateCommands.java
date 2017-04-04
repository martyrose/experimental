package com.mrose.oneoff;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * TODO(martinrose) : Add Documentation
 */
public class GenerateCommands {

  private static final Logger log = LoggerFactory.getLogger(GenerateCommands.class);

  public static void main(String[] args) throws IOException {
    Pattern p = Pattern.compile("^.+@google.com$");
    Matcher m = p.matcher("@google.com");
    log.warn(""+ m.matches());
  }
}
