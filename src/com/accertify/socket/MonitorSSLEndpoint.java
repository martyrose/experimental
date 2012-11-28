package com.accertify.socket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 3/21/11
 * Time: 12:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class MonitorSSLEndpoint {
    public static final long init = System.currentTimeMillis();
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss.SSS");

    public static final Long INTERVAL = 15000l;
    public static final Long MILLIS_PER_SECOND = 1000l;
    public static final Long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60l;
    public static final Long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60l;

    public static void main(String[] args) {
        if( args.length != 2 ) {
            System.err.println("PROGGIE HOST PORT");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        while(true) {
            long start = System.currentTimeMillis();
            MutableLong s1 = new MutableLong(System.currentTimeMillis());
            MutableLong s2 = new MutableLong(s1.value);
            MutableLong s3 = new MutableLong(s1.value);
            boolean success = doCreateEndpoint(host, port, s1, s2, s3);
            long end = System.currentTimeMillis();

            if(success) {
                System.out.println("S|" + sdf.format(new Date()) + "|" + (end-start) + "|" + (s2.value-s1.value) + "|" + (s3.value-s2.value));
            } else {
                System.out.println("F|" + sdf.format(new Date()) + "|" + (end-start) + "|" + (s2.value-s1.value) + "|" + (s3.value-s2.value));
            }

            long sleep = INTERVAL - (end % INTERVAL);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static boolean doCreateEndpoint(String host, int port, MutableLong s1, MutableLong s2, MutableLong s3) {
        javax.net.SocketFactory factory = SocketFactory.getDefault();
        Socket plainSocket = null;
        SSLSocket sslSocket = null;
        try {
            plainSocket = factory.createSocket();
            s1.value = System.currentTimeMillis();
            plainSocket.connect(new InetSocketAddress(host, port), 60000);
            s2.value = System.currentTimeMillis();
            plainSocket.setSoTimeout(60000);
            sslSocket = (SSLSocket)((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(plainSocket, host, port, true);
            sslSocket.startHandshake();
            s3.value = System.currentTimeMillis();
            return sslSocket != null && !sslSocket.isClosed();
        } catch(Throwable t) {
            s3.value = System.currentTimeMillis();
            log(t);
        } finally {
            close(sslSocket);
        }
        return false;
    }

    public static final void log(String s) {
        System.out.println(sdf.format(new java.util.Date()) + " (" + formatDurationHMS(System.currentTimeMillis()-init) + ") --- " + s);
    }
    public static final void log(Throwable e) {
        System.out.print(sdf.format(new java.util.Date()) + ": ex : ");
        e.printStackTrace(System.out);
    }

    public static final String lpad(long l, int pad, char c) {
        String s = "" + l;
        while(s.length() < pad) {
            s = c + s;
        }
        return s;
    }

    public static final String formatDurationHMS(long l) {
        long balance = l;
        long hours = 0;
        long minutes = 0;
        long seconds = 0;
        long millis = 0;
        if( balance / MILLIS_PER_HOUR > 0) {
            hours = balance / MILLIS_PER_HOUR;
            balance = balance - hours*MILLIS_PER_HOUR;
        }

        if( balance / MILLIS_PER_MINUTE > 0) {
            minutes = balance / MILLIS_PER_MINUTE;
            balance = balance - minutes*MILLIS_PER_MINUTE;
        }

        if( balance / MILLIS_PER_SECOND > 0) {
            seconds = balance / MILLIS_PER_SECOND;
            balance = balance - seconds*MILLIS_PER_SECOND;
        }

        millis = balance;

        return lpad(hours, 2, '0') + ':' + lpad(minutes, 2, '0') + ':' + lpad(seconds, 2, '0') + '.' + lpad(millis, 3, '0');
    }

    public static final void close(Socket s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    static class MutableLong {
        Long value = null;

        MutableLong(Long l) {
            value = l;
        }
    }
}

