package com.mrose.perf;

import java.util.ArrayList;
import java.util.List;

public class TestVirtuality {

  public static void main(String[] args) {
    TestVirtuality t = new TestVirtuality();
    t.go();
  }

  public static final ArrayList<String> cc = new ArrayList<String>();
  public static List<String> aa = new ArrayList<String>();
  public static ArrayList<String> bb = new ArrayList<String>();

  static {
    aa.add("a");
    aa.add("b");
    aa.add("c");
    bb.add("a");
    bb.add("b");
    bb.add("c");
    cc.add("a");
    cc.add("b");
    cc.add("c");
  }


  public void go() {
    BenchRunner.runThis(5, 1, new BenchRunner.Benchable() {
      @Override
      public void bench() {
        aa.get(1);
      }
    });

    BenchRunner.runThis(5, 1, new BenchRunner.Benchable() {
      @Override
      public void bench() {
        bb.get(1);
      }
    });

    BenchRunner.runThis(5, 1, new BenchRunner.Benchable() {
      @Override
      public void bench() {
        cc.get(1);
      }
    });
  }

}
