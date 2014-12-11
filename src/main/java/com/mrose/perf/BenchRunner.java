package com.mrose.perf;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Paul Tyma - 12/8/14
 */

public class BenchRunner {

  private final CyclicBarrier startBarrier;
  private final CyclicBarrier stopBarrier;
  private volatile boolean keepRunning = true;
  private final AtomicLong ops = new AtomicLong();

  public static int interval;

  public static void runThis(int numInterval, int numThreads, Benchable r) {
    int[] threadnums;
    if (numThreads < 0) {
      threadnums = new int[]{1, 2, 4, 10, 500};
    } else {
      threadnums = new int[]{numThreads};
    }
    runThis(numInterval, threadnums, r);
  }

  public static void runThis(int numInterval, int[] threadnums, Benchable r) {
    interval = numInterval;
    for (int g = 0; g < threadnums.length; ++g) {
      started.set(0);
      System.out.println();
      BenchRunner tr = new BenchRunner(5, threadnums[g], r);
      tr.startRunning();
      System.out.println("Threads that actually started: " + started.get());
    }
  }

  final int numThreads;
  final Benchable benchable;

  public BenchRunner(int numInterval, int numThreads, Benchable r) {
    this.numThreads = numThreads;
    benchable = r;
    CyclicBarrier cb1 = null;
    CyclicBarrier cb2 = null;
    try {
      cb1 = new CyclicBarrier(numThreads + 1);
      cb2 = new CyclicBarrier(numThreads + 1);
    } catch (Exception e) {
      //huge terrible waste of things could happen!
    }
    startBarrier = cb1;
    stopBarrier = cb2;
  }

  private static final AtomicInteger started = new AtomicInteger();

  public void startRunning() {
    long time = 0;
    for (int xxx = 0; xxx < numThreads; ++xxx) {
      (new Thread(new Runner())).start();
    }
    System.out.println("total threads = " + numThreads);

    ops.set(0);
    for (int g = 0; g < interval; ++g) {
      keepRunning = true;
      waitToGo();
      time = System.currentTimeMillis();
      startBarrier.reset();
      try {
        Thread.sleep(5000);
      } catch (Exception ee) {
      }
      keepRunning = false;
      done();
      // Grabbing time should be done *after* the done(); so all the workers have finished
      time = System.currentTimeMillis() - time;
      long o = ops.getAndSet(0);
      stopBarrier.reset();
      if (g == 0) {
        System.out.print("[1st run-ignore] ");
      }
      long timing = (o * 1000) / time;
      String formatted = format(timing);
      System.out.println(formatted + " ops/sec");
    }
  }

  private String format(long l) {
    String x = l + "";
    int len = x.length();
    if (len < 3) {
      return x;
    }

    int count = 0;
    char[] chars = new char[16];
    int ptr = 15;
    for (int g = len - 1; g >= 0; --g) {
      chars[ptr--] = x.charAt(g);

      if ((++count == 3) && (g != 0)) {
        count = 0;
        chars[ptr--] = ',';
      }
    }
    return (new String(chars)).trim();
  }

  public void waitToGo() {
    try {
      startBarrier.await();
    } catch (Exception e) {
    }
  }

  public void done() {
    try {
      stopBarrier.await();
    } catch (Exception ee) {
    }
  }

  public abstract static class Benchable {

    public abstract void bench();
  }

  class Runner implements Runnable {
    public void run() {
      started.getAndIncrement();
      long accum = 0;
      int x = interval;
      while (x-- > 0) {
        waitToGo();
        while (keepRunning) {
          benchable.bench();
          accum++;
        }
        ops.addAndGet(accum);
        accum = 0;
        done();
      }
    }
  }
}
