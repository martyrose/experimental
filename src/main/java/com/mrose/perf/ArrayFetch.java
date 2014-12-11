package com.mrose.perf;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class ArrayFetch {

  private final long NANO_PER_SECOND = (int) Math.pow(10, 9);
  private final long FETCHES = (int) Math.pow(10, 10);
  private final int RUNS = 50;
  private List<String> list_1 = new ArrayList<String>();
  private ArrayList<String> list_2 = new ArrayList<String>();
  private final ArrayList<String> list_3 = new ArrayList<String>();

  public static void main(String[] args) {
    new ArrayFetch().run();
  }

  private void run() {
    list_1.add("a");
    list_1.add("b");
    list_1.add("c");
    list_2.add("a");
    list_2.add("b");
    list_2.add("c");
    list_3.add("a");
    list_3.add("b");
    list_3.add("c");

    // JIT System.nanoTime() and ArrayList.get()
    long t1 = 0;
    long t2 = 0;
    for (int i = 0; i < 5000; i++) {
      t1 += System.nanoTime();
      t2 += list_1.get(0).length();
      t2 += list_2.get(0).length();
      t2 += list_3.get(0).length();
    }

    // Store the timing results
    List<Long> results1 = new ArrayList<>(RUNS);
    List<Long> results2 = new ArrayList<>(RUNS);
    List<Long> results3 = new ArrayList<>(RUNS);

    System.out.println("JVM Release: " + System.getProperty("java.version"));
    // Do the EXACT same test RUNS times; 3 different ways
    for (int run = 0; run < RUNS; run++) {
      long start_1 = System.nanoTime();
      long result1 = version1();
      long end_1 = System.nanoTime();

      long start_2 = System.nanoTime();
      long result2 = version2();
      long end_2 = System.nanoTime();

      long start_3 = System.nanoTime();
      long result3 = version3();
      long end_3 = System.nanoTime();

      long nano1 = end_1 - start_1;
      long nano2 = end_2 - start_2;
      long nano3 = end_3 - start_3;

      // 1B / time_elapsed == Y / nanos_per_sec
      // Y == ( 1B / time_elapsed ) * nanos_per_sec
      // Y == 1B * 1B / time_elapsed
      long numPerSecond_1 = (NANO_PER_SECOND * FETCHES) / nano1;
      long numPerSecond_2 = (NANO_PER_SECOND * FETCHES) / nano2;
      long numPerSecond_3 = (NANO_PER_SECOND * FETCHES) / nano3;

      results1.add(numPerSecond_1);
      results2.add(numPerSecond_2);
      results3.add(numPerSecond_3);
      // System.out.println(String.valueOf(run));
    }

    long total1 = 0;
    long total2 = 0;
    long total3 = 0;
    for (int i = 0; i < RUNS; i++) {
      total1 += results1.get(i);
      total2 += results2.get(i);
      total3 += results3.get(i);
    }
    NumberFormat nf = NumberFormat.getIntegerInstance();
    System.out.println("Version 1 # per second == " + nf.format(total1 / (long) RUNS));
    System.out.println("Version 2 # per second == " + nf.format(total2 / (long) RUNS));
    System.out.println("Version 3 # per second == " + nf.format(total3 / (long) RUNS));

  }

  public final long version1() {
    long returnVal = 0;
    for (int i = 0; i < FETCHES; i++) {
      returnVal += list_1.get(1).length();
    }
    return returnVal;
  }

  public final long version2() {
    long returnVal = 0;
    for (int i = 0; i < FETCHES; i++) {
      returnVal += list_2.get(1).length();
    }
    return returnVal;
  }

  public final long version3() {
    long returnVal = 0;
    for (int i = 0; i < FETCHES; i++) {
      returnVal += list_3.get(1).length();
    }
    return returnVal;
  }
}
