package com.accertify.mq;
import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.jms.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hello world!
 */
public class MQFirst {
    protected static transient Log log = LogFactory.getLog(MQFirst.class);
    private static final String BROKER_URL = "nio://10.216.30.28:61616";
    private static final String QUEUE_NAME = "TEST.FOO";
    private static final char SEP = ':';
    private static final String SEP_STR = String.valueOf(SEP);

    private static final int NUM_CONSUMERS = 10;
    private static final int TO_SEND = 1000;
    private static final ConcurrentMap<String, Boolean> UUID_TRACK = new ConcurrentHashMap<String, Boolean>();
    private static final AtomicInteger SENT = new AtomicInteger(0);
    private static final AtomicInteger RECV = new AtomicInteger(0);
    private static final AtomicLong TOTAL_DELAY = new AtomicLong(0);

    private static final CountDownLatch CONSUMER_DONE = new CountDownLatch(NUM_CONSUMERS);
    private static final CountDownLatch RECV_ALL = new CountDownLatch(TO_SEND);

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

    public static class HelloWorldProducer implements Runnable {
        public void run() {
            long s1 = System.currentTimeMillis();
            try {
                // Create a ConnectionFactory
                ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);

                // Create a Connection
                Connection connection = connectionFactory.createConnection();
                connection.start();

                // Create a Session
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                // Create the destination (Topic or Queue)
                Destination destination = session.createQueue(QUEUE_NAME);

                // Create a MessageProducer from the Session to the Topic or Queue
                MessageProducer producer = session.createProducer(destination);
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                long s2 = System.currentTimeMillis();
                for (int i = 0; i < TO_SEND; i++) {
                    // Create a messages
                    String uuid = UUID.randomUUID().toString();
                    String time = String.valueOf(System.currentTimeMillis());
                    TextMessage message = session.createTextMessage(uuid + SEP + time);
                    producer.send(message);
                    SENT.incrementAndGet();
                    UUID_TRACK.put(uuid, Boolean.TRUE);

                    if( i % 50 == 0 ) {
                        log.warn("@ " + i);
                    }
                    Thread.sleep(50);
                }
                long e2 = System.currentTimeMillis();
                log.warn("Message Send " + DurationFormatUtils.formatDurationHMS(e2-s2));

                // Clean up
                session.close();
                connection.close();
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
            long e1 = System.currentTimeMillis();
            log.warn("Full Send " + DurationFormatUtils.formatDurationHMS(e1-s1));
        }
    }

    public static class HelloWorldConsumer implements Runnable, ExceptionListener {
        public void run() {
            long s1 = System.currentTimeMillis();
            try {
                // Create a ConnectionFactory
                ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);

                // Create a Connection
                Connection connection = connectionFactory.createConnection();

                connection.setExceptionListener(this);

                // Create a Session
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                // Create the destination (Topic or Queue)
                Destination destination = session.createQueue("TEST.FOO");

                // Create a MessageConsumer from the Session to the Topic or Queue
                MessageConsumer consumer = session.createConsumer(destination);

                consumer.setMessageListener(new HelloWorldListener());

                connection.start();

                long s2 = System.currentTimeMillis();
                RECV_ALL.await();
                long e2 = System.currentTimeMillis();

                log.warn("Message Recv: " + DurationFormatUtils.formatDurationHMS(e2-s2));

                // TODO What HAS to be closed here
                consumer.close();
                session.close();
                connection.close();
            } catch (Exception e) {
                log.warn(e.getMessage());
            } finally {
                CONSUMER_DONE.countDown();
            }
            long e1 = System.currentTimeMillis();
            log.warn("Full Recv: " + DurationFormatUtils.formatDurationHMS(e1-s1));
        }

        public synchronized void onException(JMSException ex) {
            log.warn(ex.getMessage());
        }
    }

    public static class HelloWorldListener implements MessageListener {
        @Override
        public void onMessage(javax.jms.Message message) {
            if( message instanceof TextMessage ) {
                TextMessage tm = (TextMessage)message;
                try {
                    String s = tm.getText();
                    String uuid = StringUtils.trim(StringUtils.substringBefore(s, SEP_STR));
                    long time = Long.parseLong(StringUtils.trim(StringUtils.substringAfter(s, SEP_STR)));
                    long now = System.currentTimeMillis();

                    TOTAL_DELAY.addAndGet(now - time);
                    log.debug("< " + s);
                    RECV.incrementAndGet();
                    UUID_TRACK.remove(uuid);
                    RECV_ALL.countDown();
                } catch (JMSException e) {
                    log.warn(e.getMessage());
                }
            }
        }
    }
}
// Expiring messages
