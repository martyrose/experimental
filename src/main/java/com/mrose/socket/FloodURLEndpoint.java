package com.mrose.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Flood a URL endpoint
 */
public class FloodURLEndpoint {
    public static final long init = System.currentTimeMillis();
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss.SSS");
    public static final int ITER = 100000;

    public static final Long MILLIS_PER_SECOND = 1000l;
    public static final Long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60l;
    public static final Long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60l;

    private static final AtomicInteger socketsCreated = new AtomicInteger(0);

    public static void main(String[] args) {
        if( args.length != 4 ) {
            System.err.println("PROGGIE HOST PORT PATH EXPECT");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String path = args[2];
        String expect = args[3];

        System.out.println("Host: " + host);
        System.out.println("Port: " + port);
        System.out.println("Path: " + path);
        System.out.println("Expect: " + expect);

        try {
            Socket socket = null;
            PrintWriter out = null;
            BufferedReader in = null;
            AtomicBoolean persistentClose = new AtomicBoolean(true);

            long start = System.currentTimeMillis();
            for (int i = 0; i < 100000; i++) {
                if( socket == null || persistentClose.get() ) {
                    close(socket);
                    // We need to rebuild the socket
                    socket = createSocket(host, port);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    persistentClose.set(false);
                }
                sendRequest(path, expect, in, out, persistentClose);
            }
            long end = System.currentTimeMillis();

            close(socket);

            System.out.println("");
            System.out.println("Iterations: " + ITER);
            System.out.println("Took: " + formatDurationHMS(end-start));
            System.out.println("Sockets Created: " + socketsCreated.get());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void close(Socket s) {
        if( s != null ) {
            try {
                s.close();
            } catch(Throwable t) {
                ;
            }
        }
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


    public static final String lpad(long l, int pad, char c) {
        String s = "" + l;
        while(s.length() < pad) {
            s = c + s;
        }
        return s;
    }

    private static Socket createSocket(String host, int port) throws UnknownHostException, IOException {
        socketsCreated.incrementAndGet();
        return new Socket(host, port);
    }

    private static void sendRequest(String path, String expect, BufferedReader in, PrintWriter out, AtomicBoolean persistentClose) throws IOException {
        //Send request
        out.println("GET " + path + " HTTP/1.0\nConnection: Keep-Alive\n\n");
        out.flush();

        String s = null;

        while ( true ) {
            s = in.readLine();
            if( s.contains(expect) ) {
                // System.out.println("Response1: " + s);
                break;
            } else {
                s = s.toLowerCase();
                if(s.startsWith("Connection:")) {
                    if( !s.endsWith("keep-alive")) {
                        persistentClose.set(true);
                    }
                }
            }

        }

    }
}
