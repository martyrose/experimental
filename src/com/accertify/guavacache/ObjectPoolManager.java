package com.accertify.guavacache;

import com.google.common.cache.*;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Testing guava Loading Cache
 */
public class ObjectPoolManager {
    private final Logger log = LoggerFactory.getLogger(ObjectPoolManager.class);
    private final AtomicInteger created = new AtomicInteger(0);
    private final AtomicInteger destroyed = new AtomicInteger(0);

    private final int MAX_CACHE_SIZE = 2000;
    private final int EXPIRE_AFTER_MINUTES = 2;
    private final int DEDICATED_SIZE = 8;
    private final int THREADS = 24;
    private final int ENTRIES_PER_THREAD = 120;

    @Test
    public void runTest() {
        LoadingCache<String, ObjectPool> objectPool = null;
        CountDownLatch cdl = new CountDownLatch(THREADS);

        log.warn("Dedicated Size: {}", DEDICATED_SIZE);
        log.warn("Max Cache Size: {}", MAX_CACHE_SIZE);
        log.warn("Expire After Minutes: {}", EXPIRE_AFTER_MINUTES);

        objectPool = CacheBuilder.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterAccess(EXPIRE_AFTER_MINUTES, TimeUnit.MINUTES)
                .removalListener(new ObjectPoolEvictionListener())
                .build(new ObjectPoolFactory());

        spinThread(new PeridicEvictor(objectPool), true);
        spinThread(new DedicatedEntries(objectPool), true);
        spinThread(new StatusPrint(objectPool), true);
        for( int i=0; i<THREADS; i++) {
            spinThread(new RandomEntries(objectPool, cdl, (i+1)), false);
        }

        try {
            cdl.await();
        } catch (InterruptedException e) {
            ;
        }
        log.info("All Random Threads Done; waiting long time to let things die out");
        try {
            Thread.sleep(EXPIRE_AFTER_MINUTES * 2 * DateUtils.MILLIS_PER_MINUTE);
        } catch (InterruptedException e) {
            ;
        }
        log.info("All DONE Random Threads Done; waiting long time to let things die out");
    }

    private void spinThread(Runnable r, boolean daemon) {
        Thread t = new Thread(r);
        t.setDaemon(daemon);
        t.start();
    }


    class ObjectPool {
        final String id;

        ObjectPool(String id) {
            log.debug("Creating with id {}", id);
            created.incrementAndGet();
            this.id = id;
        }
    }

    class ObjectPoolEvictionListener implements RemovalListener<String, ObjectPool> {
        @Override
        public void onRemoval(final RemovalNotification<String, ObjectPool> notification) {
            log.debug("Destroying with id {}", notification.getKey());
            destroyed.incrementAndGet();
        }
    }

    class ObjectPoolFactory extends CacheLoader<String, ObjectPool> {
        @Override
        public ObjectPool load(String id) {
            return new ObjectPool(id);
        }
    }

    class PeridicEvictor implements Runnable {
        private final LoadingCache<String, ObjectPool> objectPool;
        private final MersenneTwisterFast twister;

        PeridicEvictor(LoadingCache<String, ObjectPool> objectPool) {
            this.objectPool = objectPool;
            twister = new MersenneTwisterFast();
        }

        @Override
        public void run() {
            int sleepTime = (twister.nextInt(15) + 30) * 1000;
            log.warn("Evictor every {} ms", sleepTime);
            while (true) {
                long prePoolSize = objectPool.size();
                long preCalcSize = created.intValue() - destroyed.intValue();
                long s1 = System.currentTimeMillis();
                objectPool.cleanUp();
                long s2 = System.currentTimeMillis();
                long postPoolSize = objectPool.size();
                long postCalcSize = created.intValue() - destroyed.intValue();

                log.debug("Evictor Took: {}", DurationFormatUtils.formatDurationHMS(s2 - s1));
                log.debug("Evictor PreSizes: {} {}", prePoolSize, preCalcSize);
                log.debug("Evictor PostSizes: {} {}", postPoolSize, postCalcSize);

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    ;
                }
            }
        }
    }

    class DedicatedEntries implements Runnable {
        private final LoadingCache<String, ObjectPool> objectPool;
        private final Set<String> dedicated = new HashSet<String>();
        private final MersenneTwisterFast twister;

        DedicatedEntries(LoadingCache<String, ObjectPool> objectPool) {
            this.objectPool = objectPool;
            for( int i=0; i<DEDICATED_SIZE; i++ ) {
                dedicated.add(UUID.randomUUID().toString());
            }
            twister = new MersenneTwisterFast();
        }

        @Override
        public void run() {
            int sleepTime = (twister.nextInt(15) + 45) * 1000;
            log.warn("Dedicated every {} ms", sleepTime);

            while( true ) {
                long s1 = System.currentTimeMillis();
                for(String uuid: dedicated) {
                    try {
                        objectPool.get(uuid);
                    } catch (ExecutionException e) {
                        log.warn(e.getMessage());
                    }
                }
                long s2 = System.currentTimeMillis();

                log.debug("Pushed {} Dedicated Entries: {}", DEDICATED_SIZE, DurationFormatUtils.formatDurationHMS(s2 - s1));

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    ;
                }
            }
        }
    }

    class RandomEntries implements Runnable {
        private final LoadingCache<String, ObjectPool> objectPool;
        private final CountDownLatch cdl;
        private final int threadNumber;
        private final MersenneTwisterFast twister;

        RandomEntries(LoadingCache<String, ObjectPool> objectPool, CountDownLatch cdl, int threadNumber) {
            this.objectPool = objectPool;
            twister = new MersenneTwisterFast();
            this.cdl = cdl;
            this.threadNumber = threadNumber;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < ENTRIES_PER_THREAD; i++) {
                    String uuid = UUID.randomUUID().toString();
                    try {
                        log.debug("Adding {}", uuid);
                        objectPool.get(uuid);
                    } catch (ExecutionException e) {
                        ;
                    }

                    try {
                        int sleepTime = twister.nextInt(1000) + 1;
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        ;
                    }
                }
                log.info("Random thread done {}", threadNumber);
            } finally {
                cdl.countDown();
            }
        }
    }

    class StatusPrint implements Runnable {
        private final LoadingCache<String, ObjectPool> objectPool;
        private final MersenneTwisterFast twister;

        StatusPrint(LoadingCache<String, ObjectPool> objectPool) {
            this.objectPool = objectPool;
            twister = new MersenneTwisterFast();
        }


        @Override
        public void run() {
            int sleepTime = 4000;
            log.warn("Status every {} ms", sleepTime);

            while( true ) {
                long calcSize = created.intValue() - destroyed.intValue();
                long size = objectPool.size();

                log.info("Status {} {} : {} {}", calcSize, size, created.intValue(), destroyed.intValue());

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    ;
                }
            }
        }

    }

}
