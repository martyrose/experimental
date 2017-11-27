package com.mrose.oneoff;

import com.google.common.primitives.UnsignedLong;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joda.time.DateTimeZone;
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
    DateTimeZone dtz = DateTimeZone.forID("America/Los_Angeles");
    log.warn(dtz.getID());

  }
}
