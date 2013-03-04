package com.accertify.genetic.util;

import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 6/15/11
 * Time: 2:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class ParallelSort<T> {
    private ThreadPoolExecutor tpe = null;

    public ParallelSort() {
        int numProcessors = Runtime.getRuntime().availableProcessors();
        int threads = numProcessors - 1;
        tpe = new ThreadPoolExecutor(threads, threads, Long.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory());
    }

    public static <T> void sort(T[] a, Comparator<? super T> c) {

    }
}
