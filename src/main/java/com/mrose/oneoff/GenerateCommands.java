package com.mrose.oneoff;

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
    long smearMicros = TimeUnit.HOURS.toMicros(4);
    long UPDATE_DELAY_TIME_MICROS = TimeUnit.HOURS.toMicros(1);
    long timeMicros = System.currentTimeMillis() * 1000;
    Random random = new Random();

    int hours[] = new int[36];
    for( int i =0; i<10000000; i++) {
      double d = random.nextDouble();
      long smearOverMicros = (long)(d * (double)smearMicros);
      long scheduleMicros = smearOverMicros + timeMicros + UPDATE_DELAY_TIME_MICROS;

      long microsOut = scheduleMicros - timeMicros;
      int hoursOut = (int)TimeUnit.MICROSECONDS.toHours(microsOut);
      hours[hoursOut]++;
    }
    for(int i=0; i<hours.length; i++) {
      System.out.println(i + " " + hours[i]);
    }

  }
}
