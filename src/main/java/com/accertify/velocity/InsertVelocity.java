package com.accertify.velocity;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.random.BitsStreamGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.sql.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 create table velocity_tracking_1 (
 digest1 text,
 ts integer
 );
 create index velocity_tracking_1_idx_1 on velocity_tracking_1(digest1, ts);
 */
public class InsertVelocity {
    private static final Logger log = LoggerFactory.getLogger(InsertVelocity.class);

    private static final int SECONDS_PER_YEAR = 365 * 24 *60 *60;

    private static final int MIN_ROWS_TO_CREATE = 1000000;
    private static final int BATCH_SIZE = 250;
    private static final int PARALLELISM = 1;
    private static final int TEXT_SIZE = 4;

    private static final AtomicInteger COMPLETED = new AtomicInteger(0);
    private static final CountDownLatch LATCH = new CountDownLatch(PARALLELISM);

    public InsertVelocity() {
        ;
    }

    public static void main(String[] args) throws Exception {
        log.info("Truncating");
        truncateTable();
        log.info("Inserting");
        for(int i=0; i<PARALLELISM; i++) {
            Thread t = new Thread(new InsertThread());
            t.start();
        }
        while(!LATCH.await(5, TimeUnit.SECONDS)) {
            log.info("DONE: " + COMPLETED.get());
        }
        log.info("Clustering");
        clusterTable();
        log.info("ReIndexing");
        reIndexTable();
    }

    private static Connection getConnection() {
        try {
            Connection c = DriverManager.getConnection("jdbc:postgresql://10.216.30.60:5432/mrose", "mrose", "mrose");
            c.setAutoCommit(false);
            return c;
        } catch (SQLException e) {
            throw new RuntimeException("EVIL-1", e);
        }
    }

    private static void truncateTable() throws Exception {
        try(Connection c = getConnection()) {
            try(Statement s = c.createStatement()) {
                s.executeUpdate("truncate velocity_tracking_1");
            }
        }
    }

    private static void clusterTable() throws Exception {
        try(Connection c = getConnection()) {
            try(Statement s = c.createStatement()) {
                s.executeUpdate("cluster velocity_tracking_1 using velocity_tracking_1_idx_1");
            }
        }
    }

    private static void reIndexTable() throws Exception {
        try(Connection c = getConnection()) {
            try(Statement s = c.createStatement()) {
                s.executeUpdate("reindex table velocity_tracking_1");
            }
        }
    }

    private static class InsertThread implements Runnable {
        private final long now = System.currentTimeMillis();
        private final int nowAsSeconds = (int)(now / 1000);
        private final BitsStreamGenerator rand = new Well19937c(now ^ System.identityHashCode(this));

        @Override
        public void run() {
            try(Connection c = getConnection()) {
                try(PreparedStatement ps = c.prepareStatement("insert into velocity_tracking_1(digest1, ts) values(?,?)") ) {
                    while(COMPLETED.get() < MIN_ROWS_TO_CREATE) {
                        ps.clearBatch();
                        for(int i=0; i<BATCH_SIZE; i++) {
                            String s = Base64.encodeBase64String(DigestUtils.sha1(RandomStringUtils.randomAlphanumeric(TEXT_SIZE).getBytes(Charset.defaultCharset())));
                            ps.setString(1, s);
                            ps.setInt(2, pickRandomDateSeconds());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                        c.commit();
                        COMPLETED.addAndGet(BATCH_SIZE);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("EVIL-2", e);
            } finally {
                LATCH.countDown();
            }
        }

        private int pickRandomDateSeconds() {
            return nowAsSeconds - rand.nextInt(SECONDS_PER_YEAR);
        }
    }
}
