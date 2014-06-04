package com.mrose.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;

/**
 * User: mrose
 * Date: 6/26/13
 * Time: 3:11 PM
 * <p/>
 * Comments
 */
public class SocketTimeout {
    public static final long init = System.currentTimeMillis();
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss.SSS");
    public static final Long MILLIS_PER_SECOND = 1000l;
    public static final Long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60l;
    public static final Long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60l;

    public static void main(String[] args) {
        try {
            ServerSocket ss = bindClosestServerSocket(8888);
            Socket s = ss.accept();
            log("Linger: " + s.getSoLinger());
        } catch (IOException e) {
            log("Error:" + e.getMessage());
        }
    }

    public static ServerSocket bindClosestServerSocket(int serverPort) {
        for(int i=0;i<10; i++) {
            try {
                ServerSocket serverSocket = new ServerSocket(serverPort+i, 1);
                log("Listening on " + (serverPort + i));
                return serverSocket;
            } catch(IOException iex) {
                ;
            }
        }
        return null;
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

    static class MutableLong {
        Long value = null;

        MutableLong(Long l) {
            value = l;
        }
    }

}
