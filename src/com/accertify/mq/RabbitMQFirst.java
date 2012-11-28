package com.accertify.mq;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;


import com.rabbitmq.client.QueueingConsumer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 9/6/11
 * Time: 2:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class RabbitMQFirst {
    protected static transient Log log = LogFactory.getLog(RabbitMQFirst.class);

    private static final String BROKER_URL = "10.216.30.28";
    private static final String QUEUE_NAME = "TEST.FOO";


    private static final char SEP = ':';
    private static final String SEP_STR = String.valueOf(SEP);

    private static final int NUM_CONSUMERS = 5;
    private static final int TO_SEND = 1000;
    private static final ConcurrentMap<String, Boolean> UUID_TRACK = new ConcurrentHashMap<String, Boolean>();
    private static final AtomicInteger SENT = new AtomicInteger(0);
    private static final AtomicInteger RECV = new AtomicInteger(0);
    private static final AtomicLong TOTAL_DELAY = new AtomicLong(0);

    private static final CountDownLatch CONSUMER_DONE = new CountDownLatch(NUM_CONSUMERS);

    public static void main(String[] args) throws Exception {
        for(int i=0; i<NUM_CONSUMERS; i++) {
            thread(new HelloWorldConsumer());
        }
       thread(new HelloWorldProducer());

        CONSUMER_DONE.await();

        log.warn("ALL DONE");

        log.warn("Expected: " + TO_SEND);
        log.warn("Sent: " + SENT);
        log.warn("Recv: " + RECV);
        log.warn("UUID: " + UUID_TRACK);
        log.warn("Delay: " + TOTAL_DELAY.get());
        log.warn("Q Delay: " + ((double)TOTAL_DELAY.get())/((double)TO_SEND));
    }

    public static void thread(Runnable runnable) {
        Thread brokerThread = new Thread(runnable);
        brokerThread.setDaemon(false);
        brokerThread.start();
    }

    private static final void close(Channel c) {
        if( c != null ) {
            try {
                c.close();
            } catch(Throwable t) {
                ;
            }
        }
    }
    private static final void close(Connection c) {
        if( c != null ) {
            try {
                c.close();
            } catch(Throwable t) {
                ;
            }
        }
    }

    public static class HelloWorldProducer implements Runnable {
        public void run() {
            long s1 = System.currentTimeMillis();

            Connection connection = null;
            Channel channel = null;
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(BROKER_URL);
                connection = factory.newConnection();
                channel = connection.createChannel();
                log.warn(channel);
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);

                for (int i = 0; i < TO_SEND; i++) {
                    String uuid = UUID.randomUUID().toString();
                    String time = String.valueOf(System.currentTimeMillis());
                    String message = uuid + SEP + time;

                    channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
                    SENT.incrementAndGet();
                    UUID_TRACK.put(uuid, Boolean.TRUE);

                    if( i % 50 == 0 ) {
                        log.warn("@ " + i);
                    }
                    Thread.sleep(50);
                }
            } catch (Throwable t) {
                log.error(t, t.getMessage());
            } finally {
                close(channel);
                close(connection);
            }
            long e1 = System.currentTimeMillis();
            log.warn("Full Send " + DurationFormatUtils.formatDurationHMS(e1 - s1));
        }
    }

    public static class HelloWorldConsumer implements Runnable {
        public void run() {
            long s1 = System.currentTimeMillis();
            Connection connection = null;
            Channel channel = null;
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(BROKER_URL);
                connection = factory.newConnection();
                channel = connection.createChannel();

                channel.queueDeclare(QUEUE_NAME, false, false, false, null);

                QueueingConsumer consumer = new QueueingConsumer(channel);
                channel.basicConsume(QUEUE_NAME, true, consumer);

                while (RECV.get() < TO_SEND) {
                    QueueingConsumer.Delivery delivery = consumer.nextDelivery(1000);
                    if (delivery != null) {
                        String s = new String(delivery.getBody());
                        String uuid = StringUtils.trim(StringUtils.substringBefore(s, SEP_STR));
                        long time = Long.parseLong(StringUtils.trim(StringUtils.substringAfter(s, SEP_STR)));
                        long now = System.currentTimeMillis();

                        TOTAL_DELAY.addAndGet(now - time);
                        RECV.incrementAndGet();
                        UUID_TRACK.remove(uuid);
                    }
                }
            } catch (Exception e) {
                log.warn(e.getMessage());
            } finally {
                close(channel);
                close(connection);
                CONSUMER_DONE.countDown();
            }
            long e1 = System.currentTimeMillis();
            log.warn("Full Recv: " + DurationFormatUtils.formatDurationHMS(e1 - s1));
        }
    }
}
