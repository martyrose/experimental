package com.mrose.google;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ScheduledTesting {
  public static void main(String[] args) {
    ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(0);
    scheduler.setKeepAliveTime(10, TimeUnit.MILLISECONDS);

    Future<?> future = scheduler.schedule(new DoNothing(), 10, TimeUnit.SECONDS);
    System.out.println("Sleeping");
    try {
      Thread.sleep(60 * 1000);
    } catch (InterruptedException e) {
      ;
    }
    future.cancel(false);
    System.out.println("Done Sleeping");
    scheduler.shutdownNow();
  }

  static class DoNothing implements Runnable {
    @Override
    public void run() {
      System.out.println("asdfasdfasdf");
    }
  }

  static class MyThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r);
    }
  }
}
