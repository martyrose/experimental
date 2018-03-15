package com.mrose.oneoff;

import com.google.common.primitives.UnsignedLong;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
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
    org.joda.time.format.DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("YYYYMMddHHmmss");
    String dateTimeStamp = DATE_FORMAT.print(org.joda.time.Instant.now());
    log.info(dateTimeStamp);
  }
}
