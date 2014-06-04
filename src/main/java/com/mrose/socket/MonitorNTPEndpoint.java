package com.accertify.socket;

import javax.net.SocketFactory;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 3/24/11
 * Time: 9:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class MonitorNTPEndpoint {
    public static final long init = System.currentTimeMillis();
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss.SSS");

    public static final Long INTERVAL = 15000l;
    public static final Long MILLIS_PER_SECOND = 1000l;
    public static final Long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60l;
    public static final Long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60l;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("PROGGIE HOST PORT");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        while (true) {
            MutableLong s1 = new MutableLong(System.currentTimeMillis());
            MutableLong s2 = new MutableLong(s1.value);
            boolean success = doCreateEndpoint(host, port, s1, s2);
            long end = System.currentTimeMillis();

            if (success) {
                System.out.println("S|" + sdf.format(new Date()) + "|" + (s2.value - s1.value));
            } else {
                System.out.println("F|" + sdf.format(new Date()) + "|" + (s2.value - s1.value));
            }

            long sleep = INTERVAL - (end % INTERVAL);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static boolean doCreateEndpoint(String host, int port, MutableLong s1, MutableLong s2) {
        DatagramSocket udpSocket = null;
        try {
            udpSocket = new DatagramSocket();
            s1.value = System.currentTimeMillis();
            InetAddress hostAddr = InetAddress.getByName(host);
            udpSocket.connect(hostAddr, port);
            udpSocket.setSoTimeout(60000);

            NtpV3Impl message = new NtpV3Impl();
            message.setMode(NtpV3Impl.MODE_CLIENT);
            message.setVersion(NtpV3Impl.VERSION_3);
            DatagramPacket sendPacket = message.getDatagramPacket();
            sendPacket.setAddress(hostAddr);
            sendPacket.setPort(port);

            NtpV3Impl recMessage = new NtpV3Impl();
            DatagramPacket receivePacket = recMessage.getDatagramPacket();

            /*
            * Must minimize the time between getting the current time,
            * timestamping the packet, and sending it out which
            * introduces an error in the delay time.
            * No extraneous logging and initializations here !!!
            */
            TimeStamp now = TimeStamp.getCurrentTime();

            // Note that if you do not set the transmit time field then originating time
            // in server response is all 0's which is "Thu Feb 07 01:28:16 EST 2036".
            message.setTransmitTime(now);

            udpSocket.send(sendPacket);
            udpSocket.receive(receivePacket);

            long returnTime = System.currentTimeMillis();
// create TimeInfo message container but don't pre-compute the details yet
            TimeInfo info = new TimeInfo(recMessage, returnTime, false);
            s2.value = System.currentTimeMillis();

            info.computeDetails();

//            log("Delay: " + info.getDelay());
//            log("Offset: " + info.getOffset());
//            log("Time: " + new Date(info.getReturnTime()));

            return udpSocket != null && !udpSocket.isClosed();
        } catch (Throwable t) {
            s2.value = System.currentTimeMillis();
            log(t);
        } finally {
            close(udpSocket);
        }
        return false;
    }

    public static final void log(String s) {
        System.out.println(sdf.format(new java.util.Date()) + " (" + formatDurationHMS(System.currentTimeMillis() - init) + ") --- " + s);
    }

    public static final void log(Throwable e) {
        System.out.print(sdf.format(new java.util.Date()) + ": ex : ");
        e.printStackTrace(System.out);
    }

    public static final String lpad(long l, int pad, char c) {
        String s = "" + l;
        while (s.length() < pad) {
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
        if (balance / MILLIS_PER_HOUR > 0) {
            hours = balance / MILLIS_PER_HOUR;
            balance = balance - hours * MILLIS_PER_HOUR;
        }

        if (balance / MILLIS_PER_MINUTE > 0) {
            minutes = balance / MILLIS_PER_MINUTE;
            balance = balance - minutes * MILLIS_PER_MINUTE;
        }

        if (balance / MILLIS_PER_SECOND > 0) {
            seconds = balance / MILLIS_PER_SECOND;
            balance = balance - seconds * MILLIS_PER_SECOND;
        }

        millis = balance;

        return lpad(hours, 2, '0') + ':' + lpad(minutes, 2, '0') + ':' + lpad(seconds, 2, '0') + '.' + lpad(millis, 3, '0');
    }

    public static final void close(DatagramSocket s) {
        if (s != null) {
            try {
                s.close();
            } catch (Throwable e) {
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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

    /**
     * Implementation of NtpV3Packet with methods converting Java objects to/from
     * the Network Time Protocol (NTP) data message header format described in RFC-1305.
     *
     * @author Naz Irizarry, MITRE Corp
     * @author Jason Mathews, MITRE Corp
     * @version $Revision: 658518 $ $Date: 2008-05-21 02:04:30 +0100 (Wed, 21 May 2008) $
     */
    public static class NtpV3Impl {

        /**
         * Standard NTP UDP port
         */
        public static final int NTP_PORT = 123;

        public static final int LI_NO_WARNING = 0;
        public static final int LI_LAST_MINUTE_HAS_61_SECONDS = 1;
        public static final int LI_LAST_MINUTE_HAS_59_SECONDS = 2;
        public static final int LI_ALARM_CONDITION = 3;

        /* mode options */
        public static final int MODE_RESERVED = 0;
        public static final int MODE_SYMMETRIC_ACTIVE = 1;
        public static final int MODE_SYMMETRIC_PASSIVE = 2;
        public static final int MODE_CLIENT = 3;
        public static final int MODE_SERVER = 4;
        public static final int MODE_BROADCAST = 5;
        public static final int MODE_CONTROL_MESSAGE = 6;
        public static final int MODE_PRIVATE = 7;

        public static final int NTP_MINPOLL = 4;  // 16 seconds
        public static final int NTP_MAXPOLL = 14; // 16284 seconds

        public static final int NTP_MINCLOCK = 1;
        public static final int NTP_MAXCLOCK = 10;

        public static final int VERSION_3 = 3;
        public static final int VERSION_4 = 4;

        /* possible getType values such that other time-related protocols can
        * have its information represented as NTP packets
        */
        public static final String TYPE_NTP = "NTP";         // RFC-1305/2030
        public static final String TYPE_ICMP = "ICMP";       // RFC-792
        public static final String TYPE_TIME = "TIME";       // RFC-868
        public static final String TYPE_DAYTIME = "DAYTIME"; // RFC-867

        private static final int MODE_INDEX = 0;
        private static final int MODE_SHIFT = 0;

        private static final int VERSION_INDEX = 0;
        private static final int VERSION_SHIFT = 3;

        private static final int LI_INDEX = 0;
        private static final int LI_SHIFT = 6;

        private static final int STRATUM_INDEX = 1;
        private static final int POLL_INDEX = 2;
        private static final int PRECISION_INDEX = 3;

        private static final int ROOT_DELAY_INDEX = 4;
        private static final int ROOT_DISPERSION_INDEX = 8;
        private static final int REFERENCE_ID_INDEX = 12;

        private static final int REFERENCE_TIMESTAMP_INDEX = 16;
        private static final int ORIGINATE_TIMESTAMP_INDEX = 24;
        private static final int RECEIVE_TIMESTAMP_INDEX = 32;
        private static final int TRANSMIT_TIMESTAMP_INDEX = 40;

        private static final int KEY_IDENTIFIER_INDEX = 48;
        private static final int MESSAGE_DIGEST = 54; /* len 16 bytes */

        private byte[] buf = new byte[48];

        private volatile DatagramPacket dp;

        /**
         * Creates a new instance of NtpV3Impl
         */
        public NtpV3Impl() {
        }

        /**
         * Returns mode as defined in RFC-1305 which is a 3-bit integer
         * whose value is indicated by the MODE_xxx parameters.
         *
         * @return mode as defined in RFC-1305.
         */
        public int getMode() {
            return (ui(buf[MODE_INDEX]) >> MODE_SHIFT) & 0x7;
        }

        /**
         * Return human-readable name of message mode type (RFC 1305).
         *
         * @param mode
         * @return mode name
         */
        public String getModeName(int mode) {
            switch (mode) {
                case MODE_RESERVED:
                    return "Reserved";
                case MODE_SYMMETRIC_ACTIVE:
                    return "Symmetric Active";
                case MODE_SYMMETRIC_PASSIVE:
                    return "Symmetric Passive";
                case MODE_CLIENT:
                    return "Client";
                case MODE_SERVER:
                    return "Server";
                case MODE_BROADCAST:
                    return "Broadcast";
                case MODE_CONTROL_MESSAGE:
                    return "Control";
                case MODE_PRIVATE:
                    return "Private";
                default:
                    return "Unknown";
            }
        }

        /**
         * Return human-readable name of message mode type as described in
         * RFC 1305.
         *
         * @return mode name as string.
         */
        public String getModeName() {
            return getModeName(getMode());
        }

        /**
         * Set mode as defined in RFC-1305.
         *
         * @param mode
         */
        public void setMode(int mode) {
            buf[MODE_INDEX] = (byte) (buf[MODE_INDEX] & 0xF8 | mode & 0x7);
        }

        /**
         * Returns leap indicator as defined in RFC-1305 which is a two-bit code:
         * 0=no warning
         * 1=last minute has 61 seconds
         * 2=last minute has 59 seconds
         * 3=alarm condition (clock not synchronized)
         *
         * @return leap indicator as defined in RFC-1305.
         */
        public int getLeapIndicator() {
            return (ui(buf[LI_INDEX]) >> LI_SHIFT) & 0x3;
        }

        /**
         * Set leap indicator as defined in RFC-1305.
         *
         * @param li leap indicator.
         */
        public void setLeapIndicator(int li) {
            buf[LI_INDEX] = (byte) (buf[LI_INDEX] & 0x3F | ((li & 0x3) << LI_SHIFT));
        }

        /**
         * Returns poll interval as defined in RFC-1305, which is an eight-bit
         * signed integer indicating the maximum interval between successive
         * messages, in seconds to the nearest power of two (e.g. value of six
         * indicates an interval of 64 seconds. The values that can appear in
         * this field range from NTP_MINPOLL to NTP_MAXPOLL inclusive.
         *
         * @return poll interval as defined in RFC-1305.
         */
        public int getPoll() {
            return buf[POLL_INDEX];
        }

        /**
         * Set poll interval as defined in RFC-1305.
         *
         * @param poll poll interval.
         */
        public void setPoll(int poll) {
            buf[POLL_INDEX] = (byte) (poll & 0xFF);
        }

        /**
         * Returns precision as defined in RFC-1305 encoded as an 8-bit signed
         * integer (seconds to nearest power of two).
         * Values normally range from -6 to -20.
         *
         * @return precision as defined in RFC-1305.
         */
        public int getPrecision() {
            return buf[PRECISION_INDEX];
        }

        /**
         * Set precision as defined in RFC-1305.
         *
         * @param precision
         */
        public void setPrecision(int precision) {
            buf[PRECISION_INDEX] = (byte) (precision & 0xFF);
        }

        /**
         * Returns NTP version number as defined in RFC-1305.
         *
         * @return NTP version number.
         */
        public int getVersion() {
            return (ui(buf[VERSION_INDEX]) >> VERSION_SHIFT) & 0x7;
        }

        /**
         * Set NTP version as defined in RFC-1305.
         *
         * @param version NTP version.
         */
        public void setVersion(int version) {
            buf[VERSION_INDEX] = (byte) (buf[VERSION_INDEX] & 0xC7 | ((version & 0x7) << VERSION_SHIFT));
        }

        /**
         * Returns Stratum as defined in RFC-1305, which indicates the stratum level
         * of the local clock, with values defined as follows: 0=unspecified,
         * 1=primary ref clock, and all others a secondary reference (via NTP).
         *
         * @return Stratum level as defined in RFC-1305.
         */
        public int getStratum() {
            return ui(buf[STRATUM_INDEX]);
        }

        /**
         * Set stratum level as defined in RFC-1305.
         *
         * @param stratum stratum level.
         */
        public void setStratum(int stratum) {
            buf[STRATUM_INDEX] = (byte) (stratum & 0xFF);
        }

        /**
         * Return root delay as defined in RFC-1305, which is the total roundtrip delay
         * to the primary reference source, in seconds. Values can take positive and
         * negative values, depending on clock precision and skew.
         *
         * @return root delay as defined in RFC-1305.
         */
        public int getRootDelay() {
            return getInt(ROOT_DELAY_INDEX);
        }

        /**
         * Return root delay as defined in RFC-1305 in milliseconds, which is
         * the total roundtrip delay to the primary reference source, in
         * seconds. Values can take positive and negative values, depending
         * on clock precision and skew.
         *
         * @return root delay in milliseconds
         */
        public double getRootDelayInMillisDouble() {
            double l = getRootDelay();
            return l / 65.536;
        }

        /**
         * Returns root dispersion as defined in RFC-1305.
         *
         * @return root dispersion.
         */
        public int getRootDispersion() {
            return getInt(ROOT_DISPERSION_INDEX);
        }

        /**
         * Returns root dispersion (as defined in RFC-1305) in milliseconds.
         *
         * @return root dispersion in milliseconds
         */
        public long getRootDispersionInMillis() {
            long l = getRootDispersion();
            return (l * 1000) / 65536L;
        }

        /**
         * Returns root dispersion (as defined in RFC-1305) in milliseconds
         * as double precision value.
         *
         * @return root dispersion in milliseconds
         */
        public double getRootDispersionInMillisDouble() {
            double l = getRootDispersion();
            return l / 65.536;
        }

        /**
         * Set reference clock identifier field with 32-bit unsigned integer value.
         * See RFC-1305 for description.
         *
         * @param refId reference clock identifier.
         */
        public void setReferenceId(int refId) {
            for (int i = 3; i >= 0; i--) {
                buf[REFERENCE_ID_INDEX + i] = (byte) (refId & 0xff);
                refId >>>= 8; // shift right one-byte
            }
        }

        /**
         * Returns the reference id as defined in RFC-1305, which is
         * a 32-bit integer whose value is dependent on several criteria.
         *
         * @return the reference id as defined in RFC-1305.
         */
        public int getReferenceId() {
            return getInt(REFERENCE_ID_INDEX);
        }

        /**
         * Returns the reference id string. String cannot be null but
         * value is dependent on the version of the NTP spec supported
         * and stratum level. Value can be an empty string, clock type string,
         * IP address, or a hex string.
         *
         * @return the reference id string.
         */
        public String getReferenceIdString() {
            int version = getVersion();
            int stratum = getStratum();
            if (version == VERSION_3 || version == VERSION_4) {
                if (stratum == 0 || stratum == 1) {
                    return idAsString(); // 4-character ASCII string (e.g. GPS, USNO)
                }
                // in NTPv4 servers this is latest transmit timestamp of ref source
                if (version == VERSION_4)
                    return idAsHex();
            }

            // Stratum 2 and higher this is a four-octet IPv4 address
            // of the primary reference host.
            if (stratum >= 2) {
                return idAsIPAddress();
            }
            return idAsHex();
        }

        /**
         * Returns Reference id as dotted IP address.
         *
         * @return refId as IP address string.
         */
        private String idAsIPAddress() {
            return ui(buf[REFERENCE_ID_INDEX]) + "." +
                    ui(buf[REFERENCE_ID_INDEX + 1]) + "." +
                    ui(buf[REFERENCE_ID_INDEX + 2]) + "." +
                    ui(buf[REFERENCE_ID_INDEX + 3]);
        }

        private String idAsString() {
            StringBuilder id = new StringBuilder();
            for (int i = 0; i <= 3; i++) {
                char c = (char) buf[REFERENCE_ID_INDEX + i];
                if (c == 0) break; // 0-terminated string
                id.append(c);
            }
            return id.toString();
        }

        private String idAsHex() {
            return Integer.toHexString(getReferenceId());
        }

        /**
         * Returns the transmit timestamp as defined in RFC-1305.
         *
         * @return the transmit timestamp as defined in RFC-1305.
         *         Never returns a null object.
         */
        public TimeStamp getTransmitTimeStamp() {
            return getTimestamp(TRANSMIT_TIMESTAMP_INDEX);
        }

        /**
         * Set transmit time with NTP timestamp.
         * If <code>ts</code> is null then zero time is used.
         *
         * @param ts NTP timestamp
         */
        public void setTransmitTime(TimeStamp ts) {
            setTimestamp(TRANSMIT_TIMESTAMP_INDEX, ts);
        }

        /**
         * Set originate timestamp given NTP TimeStamp object.
         * If <code>ts</code> is null then zero time is used.
         *
         * @param ts NTP timestamp
         */
        public void setOriginateTimeStamp(TimeStamp ts) {
            setTimestamp(ORIGINATE_TIMESTAMP_INDEX, ts);
        }

        /**
         * Returns the originate time as defined in RFC-1305.
         *
         * @return the originate time.
         *         Never returns null.
         */
        public TimeStamp getOriginateTimeStamp() {
            return getTimestamp(ORIGINATE_TIMESTAMP_INDEX);
        }

        /**
         * Returns the reference time as defined in RFC-1305.
         *
         * @return the reference time as <code>TimeStamp</code> object.
         *         Never returns null.
         */
        public TimeStamp getReferenceTimeStamp() {
            return getTimestamp(REFERENCE_TIMESTAMP_INDEX);
        }

        /**
         * Set Reference time with NTP timestamp. If <code>ts</code> is null
         * then zero time is used.
         *
         * @param ts NTP timestamp
         */
        public void setReferenceTime(TimeStamp ts) {
            setTimestamp(REFERENCE_TIMESTAMP_INDEX, ts);
        }

        /**
         * Returns receive timestamp as defined in RFC-1305.
         *
         * @return the receive time.
         *         Never returns null.
         */
        public TimeStamp getReceiveTimeStamp() {
            return getTimestamp(RECEIVE_TIMESTAMP_INDEX);
        }

        /**
         * Set receive timestamp given NTP TimeStamp object.
         * If <code>ts</code> is null then zero time is used.
         *
         * @param ts timestamp
         */
        public void setReceiveTimeStamp(TimeStamp ts) {
            setTimestamp(RECEIVE_TIMESTAMP_INDEX, ts);
        }

        /**
         * Return type of time packet. The values (e.g. NTP, TIME, ICMP, ...)
         * correspond to the protocol used to obtain the timing information.
         *
         * @return packet type string identifier which in this case is "NTP".
         */
        public String getType() {
            return "NTP";
        }

        /**
         * @return 4 bytes as 32-bit int
         */
        private int getInt(int index) {
            int i = ui(buf[index]) << 24 |
                    ui(buf[index + 1]) << 16 |
                    ui(buf[index + 2]) << 8 |
                    ui(buf[index + 3]);

            return i;
        }

        /**
         * Get NTP Timestamp at specified starting index.
         *
         * @param index index into data array
         * @return TimeStamp object for 64 bits starting at index
         */
        private TimeStamp getTimestamp(int index) {
            return new TimeStamp(getLong(index));
        }

        /**
         * Get Long value represented by bits starting at specified index.
         *
         * @return 8 bytes as 64-bit long
         */
        private long getLong(int index) {
            long i = ul(buf[index]) << 56 |
                    ul(buf[index + 1]) << 48 |
                    ul(buf[index + 2]) << 40 |
                    ul(buf[index + 3]) << 32 |
                    ul(buf[index + 4]) << 24 |
                    ul(buf[index + 5]) << 16 |
                    ul(buf[index + 6]) << 8 |
                    ul(buf[index + 7]);
            return i;
        }

        /**
         * Sets the NTP timestamp at the given array index.
         *
         * @param index index into the byte array.
         * @param t     TimeStamp.
         */
        private void setTimestamp(int index, TimeStamp t) {
            long ntpTime = (t == null) ? 0 : t.ntpValue();
            // copy 64-bits from Long value into 8 x 8-bit bytes of array
            // one byte at a time shifting 8-bits for each position.
            for (int i = 7; i >= 0; i--) {
                buf[index + i] = (byte) (ntpTime & 0xFF);
                ntpTime >>>= 8; // shift to next byte
            }
            // buf[index] |= 0x80;  // only set if 1900 baseline....
        }

        /**
         * Returns the datagram packet with the NTP details already filled in.
         *
         * @return a datagram packet.
         */
        public synchronized DatagramPacket getDatagramPacket() {
            if (dp == null) {
                dp = new DatagramPacket(buf, buf.length);
                dp.setPort(NTP_PORT);
            }
            return dp;
        }

        /**
         * Set the contents of this object from source datagram packet.
         *
         * @param srcDp source DatagramPacket to copy contents from.
         */
        public void setDatagramPacket(DatagramPacket srcDp) {
            byte[] incomingBuf = srcDp.getData();
            int len = srcDp.getLength();
            if (len > buf.length)
                len = buf.length;

            System.arraycopy(incomingBuf, 0, buf, 0, len);
        }

        /**
         * Convert byte to unsigned integer.
         * Java only has signed types so we have to do
         * more work to get unsigned ops.
         *
         * @param b
         * @return unsigned int value of byte
         */
        protected final int ui(byte b) {
            int i = b & 0xFF;
            return i;
        }

        /**
         * Convert byte to unsigned long.
         * Java only has signed types so we have to do
         * more work to get unsigned ops
         *
         * @param b
         * @return unsigned long value of byte
         */
        protected final long ul(byte b) {
            long i = b & 0xFF;
            return i;
        }

        /**
         * Returns details of NTP packet as a string.
         *
         * @return details of NTP packet as a string.
         */
        @Override
        public String toString() {
            return "[" +
                    "version:" + getVersion() +
                    ", mode:" + getMode() +
                    ", poll:" + getPoll() +
                    ", precision:" + getPrecision() +
                    ", delay:" + getRootDelay() +
                    ", dispersion(ms):" + getRootDispersionInMillisDouble() +
                    ", id:" + getReferenceIdString() +
                    ", xmitTime:" + getTransmitTimeStamp().toDateString() +
                    " ]";
        }

    }


    /**
     * TimeStamp class represents the Network Time Protocol (NTP) timestamp
     * as defined in RFC-1305 and SNTP (RFC-2030). It is represented as a
     * 64-bit unsigned fixed-point number in seconds relative to 0-hour on 1-January-1900.
     * The 32-bit low-order bits are the fractional seconds whose precision is
     * about 200 picoseconds. Assumes overflow date when date passes MAX_LONG
     * and reverts back to 0 is 2036 and not 1900. Test for most significant
     * bit: if MSB=0 then 2036 basis is used otherwise 1900 if MSB=1.
     * <p>
     * Methods exist to convert NTP timestamps to and from the equivalent Java date
     * representation, which is the number of milliseconds since the standard base
     * time known as "the epoch", namely January 1, 1970, 00:00:00 GMT.
     * </p>
     *
     * @author Jason Mathews, MITRE Corp
     * @version $Revision: 658518 $ $Date: 2008-05-21 02:04:30 +0100 (Wed, 21 May 2008) $
     * @see java.util.Date
     */
    public static class TimeStamp implements java.io.Serializable, Comparable // TODO add comparable type?
    {

        /**
         * baseline NTP time if bit-0=0 -> 7-Feb-2036 @ 06:28:16 UTC
         */
        protected static final long msb0baseTime = 2085978496000L;

        /**
         * baseline NTP time if bit-0=1 -> 1-Jan-1900 @ 01:00:00 UTC
         */
        protected static final long msb1baseTime = -2208988800000L;

        /**
         * Default NTP date string format. E.g. Fri, Sep 12 2003 21:06:23.860.
         * See <code>java.text.SimpleDateFormat</code> for code descriptions.
         */
        public final static String NTP_DATE_FORMAT = "EEE, MMM dd yyyy HH:mm:ss.SSS";

        /*
        * Caches for the DateFormatters used by various toString methods.
        */
        private SoftReference<DateFormat> simpleFormatter = null;
        private SoftReference<DateFormat> utcFormatter = null;

        /**
         * NTP timestamp value: 64-bit unsigned fixed-point number as defined in RFC-1305
         * with high-order 32 bits the seconds field and the low-order 32-bits the
         * fractional field.
         */
        private long ntpTime;

        private static final long serialVersionUID = 8139806907588338737L;

        // initialization of static time bases
        /*
        static {
            TimeZone utcZone = TimeZone.getTimeZone("UTC");
            Calendar calendar = Calendar.getInstance(utcZone);
            calendar.set(1900, Calendar.JANUARY, 1, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            msb1baseTime = calendar.getTime().getTime();
            calendar.set(2036, Calendar.FEBRUARY, 7, 6, 28, 16);
            calendar.set(Calendar.MILLISECOND, 0);
            msb0baseTime = calendar.getTime().getTime();
        }
        */

        /**
         * Constructs a newly allocated NTP timestamp object
         * that represents the native 64-bit long argument.
         */
        public TimeStamp(long ntpTime) {
            this.ntpTime = ntpTime;
        }

        /**
         * Constructs a newly allocated NTP timestamp object
         * that represents the value represented by the string
         * in hexdecimal form (e.g. "c1a089bd.fc904f6d").
         *
         * @throws NumberFormatException - if the string does not contain a parsable timestamp.
         */
        public TimeStamp(String s) throws NumberFormatException {
            ntpTime = decodeNtpHexString(s);
        }

        /**
         * Constructs a newly allocated NTP timestamp object
         * that represents the Java Date argument.
         *
         * @param d - the Date to be represented by the Timestamp object.
         */
        public TimeStamp(Date d) {
            ntpTime = (d == null) ? 0 : toNtpTime(d.getTime());
        }

        /**
         * Returns the value of this Timestamp as a long value.
         *
         * @return the 64-bit long value represented by this object.
         */
        public long ntpValue() {
            return ntpTime;
        }

        /**
         * Returns high-order 32-bits representing the seconds of this NTP timestamp.
         *
         * @return seconds represented by this NTP timestamp.
         */
        public long getSeconds() {
            return (ntpTime >>> 32) & 0xffffffffL;
        }

        /**
         * Returns low-order 32-bits representing the fractional seconds.
         *
         * @return fractional seconds represented by this NTP timestamp.
         */
        public long getFraction() {
            return ntpTime & 0xffffffffL;
        }

        /**
         * Convert NTP timestamp to Java standard time.
         *
         * @return NTP Timestamp in Java time
         */
        public long getTime() {
            return getTime(ntpTime);
        }

        /**
         * Convert NTP timestamp to Java Date object.
         *
         * @return NTP Timestamp in Java Date
         */
        public Date getDate() {
            long time = getTime(ntpTime);
            return new Date(time);
        }

        /**
         * Convert 64-bit NTP timestamp to Java standard time.
         * <p/>
         * Note that java time (milliseconds) by definition has less precision
         * then NTP time (picoseconds) so converting NTP timestamp to java time and back
         * to NTP timestamp loses precision. For example, Tue, Dec 17 2002 09:07:24.810 EST
         * is represented by a single Java-based time value of f22cd1fc8a, but its
         * NTP equivalent are all values ranging from c1a9ae1c.cf5c28f5 to c1a9ae1c.cf9db22c.
         *
         * @param ntpTimeValue
         * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT
         *         represented by this NTP timestamp value.
         */
        public long getTime(long ntpTimeValue) {
            long seconds = (ntpTimeValue >>> 32) & 0xffffffffL;     // high-order 32-bits
            long fraction = ntpTimeValue & 0xffffffffL;             // low-order 32-bits

            // Use round-off on fractional part to preserve going to lower precision
            fraction = Math.round(1000D * fraction / 0x100000000L);

            /*
            * If the most significant bit (MSB) on the seconds field is set we use
            * a different time base. The following text is a quote from RFC-2030 (SNTP v4):
            *
            *  If bit 0 is set, the UTC time is in the range 1968-2036 and UTC time
            *  is reckoned from 0h 0m 0s UTC on 1 January 1900. If bit 0 is not set,
            *  the time is in the range 2036-2104 and UTC time is reckoned from
            *  6h 28m 16s UTC on 7 February 2036.
            */
            long msb = seconds & 0x80000000L;
            if (msb == 0) {
                // use base: 7-Feb-2036 @ 06:28:16 UTC
                return msb0baseTime + (seconds * 1000) + fraction;
            } else {
                // use base: 1-Jan-1900 @ 01:00:00 UTC
                return msb1baseTime + (seconds * 1000) + fraction;
            }
        }

        /**
         * Helper method to convert Java time to NTP timestamp object.
         * Note that Java time (milliseconds) by definition has less precision
         * then NTP time (picoseconds) so converting Ntptime to Javatime and back
         * to Ntptime loses precision. For example, Tue, Dec 17 2002 09:07:24.810
         * is represented by a single Java-based time value of f22cd1fc8a, but its
         * NTP equivalent are all values from c1a9ae1c.cf5c28f5 to c1a9ae1c.cf9db22c.
         *
         * @param date the milliseconds since January 1, 1970, 00:00:00 GMT.
         * @return NTP timestamp object at the specified date.
         */
        public static TimeStamp getNtpTime(long date) {
            return new TimeStamp(toNtpTime(date));
        }

        /**
         * Constructs a NTP timestamp object and initializes it so that
         * it represents the time at which it was allocated, measured to the
         * nearest millisecond.
         *
         * @return NTP timestamp object set to the current time.
         * @see java.lang.System#currentTimeMillis()
         */
        public static TimeStamp getCurrentTime() {
            return getNtpTime(System.currentTimeMillis());
        }

        /**
         * Convert NTP timestamp hexstring (e.g. "c1a089bd.fc904f6d") to the NTP
         * 64-bit unsigned fixed-point number.
         *
         * @return NTP 64-bit timestamp value.
         * @throws NumberFormatException - if the string does not contain a parsable timestamp.
         */
        protected long decodeNtpHexString(String s)
                throws NumberFormatException {
            if (s == null) {
                throw new NumberFormatException("null");
            }
            int ind = s.indexOf('.');
            if (ind == -1) {
                if (s.length() == 0) return 0;
                return Long.parseLong(s, 16) << 32; // no decimal
            }

            return Long.parseLong(s.substring(0, ind), 16) << 32 |
                    Long.parseLong(s.substring(ind + 1), 16);
        }

        /**
         * Parses the string argument as a NTP hexidecimal timestamp representation string
         * (e.g. "c1a089bd.fc904f6d").
         *
         * @param s - hexstring.
         * @return the Timestamp represented by the argument in hexidecimal.
         * @throws NumberFormatException - if the string does not contain a parsable timestamp.
         */
        public TimeStamp parseNtpString(String s)
                throws NumberFormatException {
            return new TimeStamp(decodeNtpHexString(s));
        }

        /**
         * Converts Java time to 64-bit NTP time representation.
         *
         * @param t Java time
         * @return NTP timestamp representation of Java time value.
         */
        protected static long toNtpTime(long t) {
            boolean useBase1 = t < msb0baseTime;    // time < Feb-2036
            long baseTime;
            if (useBase1) {
                baseTime = t - msb1baseTime; // dates <= Feb-2036
            } else {
                // if base0 needed for dates >= Feb-2036
                baseTime = t - msb0baseTime;
            }

            long seconds = baseTime / 1000;
            long fraction = ((baseTime % 1000) * 0x100000000L) / 1000;

            if (useBase1) {
                seconds |= 0x80000000L; // set high-order bit if msb1baseTime 1900 used
            }

            long time = seconds << 32 | fraction;
            return time;
        }

        /**
         * Computes a hashcode for this Timestamp. The result is the exclusive
         * OR of the two halves of the primitive <code>long</code> value
         * represented by this <code>TimeStamp</code> object. That is, the hashcode
         * is the value of the expression:
         * <blockquote><pre>
         * (int)(this.ntpValue()^(this.ntpValue() >>> 32))
         * </pre></blockquote>
         *
         * @return a hash code value for this object.
         */
        @Override
        public int hashCode() {
            return (int) (ntpTime ^ (ntpTime >>> 32));
        }

        /**
         * Compares this object against the specified object.
         * The result is <code>true</code> if and only if the argument is
         * not <code>null</code> and is a <code>Long</code> object that
         * contains the same <code>long</code> value as this object.
         *
         * @param obj the object to compare with.
         * @return <code>true</code> if the objects are the same;
         *         <code>false</code> otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TimeStamp) {
                return ntpTime == ((TimeStamp) obj).ntpValue();
            }
            return false;
        }

        /**
         * Converts this <code>TimeStamp</code> object to a <code>String</code>.
         * The NTP timestamp 64-bit long value is represented as hex string with
         * seconds separated by fractional seconds by a decimal point;
         * e.g. c1a089bd.fc904f6d <=> Tue, Dec 10 2002 10:41:49.986
         *
         * @return NTP timestamp 64-bit long value as hex string with seconds
         *         separated by fractional seconds.
         */
        @Override
        public String toString() {
            return toString(ntpTime);
        }

        /**
         * Left-pad 8-character hex string with 0's
         *
         * @param buf - StringBuffer which is appended with leading 0's.
         * @param l   - a long.
         */
        private void appendHexString(StringBuffer buf, long l) {
            String s = Long.toHexString(l);
            for (int i = s.length(); i < 8; i++)
                buf.append('0');
            buf.append(s);
        }

        /**
         * Converts 64-bit NTP timestamp value to a <code>String</code>.
         * The NTP timestamp value is represented as hex string with
         * seconds separated by fractional seconds by a decimal point;
         * e.g. c1a089bd.fc904f6d <=> Tue, Dec 10 2002 10:41:49.986
         *
         * @return NTP timestamp 64-bit long value as hex string with seconds
         *         separated by fractional seconds.
         */
        public String toString(long ntpTime) {
            StringBuffer buf = new StringBuffer();
            // high-order second bits (32..63) as hexstring
            appendHexString(buf, (ntpTime >>> 32) & 0xffffffffL);

            // low-order fractional seconds bits (0..31) as hexstring
            buf.append('.');
            appendHexString(buf, ntpTime & 0xffffffffL);

            return buf.toString();
        }

        /**
         * Converts this <code>TimeStamp</code> object to a <code>String</code>
         * of the form:
         * <blockquote><pre>
         * EEE, MMM dd yyyy HH:mm:ss.SSS</pre></blockquote>
         * See java.text.SimpleDataFormat for code descriptions.
         *
         * @return a string representation of this date.
         */
        public String toDateString() {
            DateFormat formatter = null;
            if (simpleFormatter != null) {
                formatter = simpleFormatter.get();
            }
            if (formatter == null) {
                // No cache yet, or cached formatter GC'd
                formatter = new SimpleDateFormat(NTP_DATE_FORMAT, Locale.US);
                formatter.setTimeZone(TimeZone.getDefault());
                simpleFormatter = new SoftReference<DateFormat>(formatter);
            }
            Date ntpDate = getDate();
            synchronized (formatter) {
                return formatter.format(ntpDate);
            }
        }

        /**
         * Converts this <code>TimeStamp</code> object to a <code>String</code>
         * of the form:
         * <blockquote><pre>
         * EEE, MMM dd yyyy HH:mm:ss.SSS UTC</pre></blockquote>
         * See java.text.SimpleDataFormat for code descriptions.
         *
         * @return a string representation of this date in UTC.
         */
        public String toUTCString() {
            DateFormat formatter = null;
            if (utcFormatter != null)
                formatter = utcFormatter.get();
            if (formatter == null) {
                // No cache yet, or cached formatter GC'd
                formatter = new SimpleDateFormat(NTP_DATE_FORMAT + " 'UTC'",
                        Locale.US);
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                utcFormatter = new SoftReference<DateFormat>(formatter);
            }
            Date ntpDate = getDate();
            synchronized (formatter) {
                return formatter.format(ntpDate);
            }
        }

        /**
         * Compares two Timestamps numerically.
         *
         * @param anotherTimeStamp - the <code>TimeStamp</code> to be compared.
         * @return the value <code>0</code> if the argument TimeStamp is equal to
         *         this TimeStamp; a value less than <code>0</code> if this TimeStamp
         *         is numerically less than the TimeStamp argument; and a
         *         value greater than <code>0</code> if this TimeStamp is
         *         numerically greater than the TimeStamp argument
         *         (signed comparison).
         */
        public int compareTo(TimeStamp anotherTimeStamp) {
            long thisVal = this.ntpTime;
            long anotherVal = anotherTimeStamp.ntpTime;
            return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
        }

        /**
         * Compares this TimeStamp to another Object.  If the Object is a TimeStamp,
         * this function behaves like <code>compareTo(TimeStamp)</code>.  Otherwise,
         * it throws a <code>ClassCastException</code> (as TimeStamps are comparable
         * only to other TimeStamps).
         *
         * @param o the <code>Object</code> to be compared.
         * @return the value <code>0</code> if the argument is a TimeStamp
         *         numerically equal to this TimeStamp; a value less than
         *         <code>0</code> if the argument is a TimeStamp numerically
         *         greater than this TimeStamp; and a value greater than
         *         <code>0</code> if the argument is a TimeStamp numerically
         *         less than this TimeStamp.
         * @throws ClassCastException if the argument is not a
         *                            <code>TimeStamp</code>.
         * @see java.lang.Comparable
         */
        public int compareTo(Object o) {
            return compareTo((TimeStamp) o);
        }

    }

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


    /**
     * Wrapper class to network time packet messages (NTP, etc) that computes
     * related timing info and stats.
     *
     * @author Jason Mathews, MITRE Corp
     * @version $Revision: 658518 $ $Date: 2008-05-21 02:04:30 +0100 (Wed, 21 May 2008) $
     */
    public static class TimeInfo {

        private NtpV3Impl _message;
        private List<String> _comments;
        private Long _delay;
        private Long _offset;

        /**
         * time at which time message packet was received by local machine
         */
        private long _returnTime;

        /**
         * flag indicating that the TimeInfo details was processed and delay/offset were computed
         */
        private boolean _detailsComputed;

        /**
         * Create TimeInfo object with raw packet message and destination time received.
         *
         * @param message    NTP message packet
         * @param returnTime destination receive time
         * @throws IllegalArgumentException if message is null
         */
        public TimeInfo(NtpV3Impl message, long returnTime) {
            this(message, returnTime, null, true);
        }

        /**
         * Create TimeInfo object with raw packet message and destination time received.
         *
         * @param message    NTP message packet
         * @param returnTime destination receive time
         * @param comments   List of errors/warnings identified during processing
         * @throws IllegalArgumentException if message is null
         */
        public TimeInfo(NtpV3Impl message, long returnTime, List<String> comments) {
            this(message, returnTime, comments, true);
        }

        /**
         * Create TimeInfo object with raw packet message and destination time received.
         * Auto-computes details if computeDetails flag set otherwise this is delayed
         * until computeDetails() is called. Delayed computation is for fast
         * intialization when sub-millisecond timing is needed.
         *
         * @param msgPacket        NTP message packet
         * @param returnTime       destination receive time
         * @param doComputeDetails flag to pre-compute delay/offset values
         * @throws IllegalArgumentException if message is null
         */
        public TimeInfo(NtpV3Impl msgPacket, long returnTime, boolean doComputeDetails) {
            this(msgPacket, returnTime, null, doComputeDetails);
        }

        /**
         * Create TimeInfo object with raw packet message and destination time received.
         * Auto-computes details if computeDetails flag set otherwise this is delayed
         * until computeDetails() is called. Delayed computation is for fast
         * intialization when sub-millisecond timing is needed.
         *
         * @param message          NTP message packet
         * @param returnTime       destination receive time
         * @param comments         list of comments used to store errors/warnings with message
         * @param doComputeDetails flag to pre-compute delay/offset values
         * @throws IllegalArgumentException if message is null
         */
        public TimeInfo(NtpV3Impl message, long returnTime, List<String> comments,
                        boolean doComputeDetails) {
            if (message == null)
                throw new IllegalArgumentException("message cannot be null");
            this._returnTime = returnTime;
            this._message = message;
            this._comments = comments;
            if (doComputeDetails)
                computeDetails();
        }

        /**
         * Add comment (error/warning) to list of comments associated
         * with processing of NTP parameters. If comment list not create
         * then one will be created.
         *
         * @param comment
         */
        public void addComment(String comment) {
            if (_comments == null) {
                _comments = new ArrayList<String>();
            }
            _comments.add(comment);
        }

        /**
         * Compute and validate details of the NTP message packet. Computed
         * fields include the offset and delay.
         */
        public void computeDetails() {
            if (_detailsComputed) {
                return; // details already computed - do nothing
            }
            _detailsComputed = true;
            if (_comments == null) {
                _comments = new ArrayList<String>();
            }

            TimeStamp origNtpTime = _message.getOriginateTimeStamp();
            long origTime = origNtpTime.getTime();

            // Receive Time is time request received by server (t2)
            TimeStamp rcvNtpTime = _message.getReceiveTimeStamp();
            long rcvTime = rcvNtpTime.getTime();

            // Transmit time is time reply sent by server (t3)
            TimeStamp xmitNtpTime = _message.getTransmitTimeStamp();
            long xmitTime = xmitNtpTime.getTime();

            /*
            * Round-trip network delay and local clock offset (or time drift) is calculated
            * according to this standard NTP equation:
            *
            * LocalClockOffset = ((ReceiveTimestamp - OriginateTimestamp) +
            *                     (TransmitTimestamp - DestinationTimestamp)) / 2
            *
            * equations from RFC-1305 (NTPv3)
            *      roundtrip delay = (t4 - t1) - (t3 - t2)
            *      local clock offset = ((t2 - t1) + (t3 - t4)) / 2
            *
            * It takes into account network delays and assumes that they are symmetrical.
            *
            * Note the typo in SNTP RFCs 1769/2030 which state that the delay
            * is (T4 - T1) - (T2 - T3) with the "T2" and "T3" switched.
            */
            if (origNtpTime.ntpValue() == 0) {
                // without originate time cannot determine when packet went out
                // might be via a broadcast NTP packet...
                if (xmitNtpTime.ntpValue() != 0) {
                    _offset = Long.valueOf(xmitTime - _returnTime);
                    _comments.add("Error: zero orig time -- cannot compute delay");
                } else
                    _comments.add("Error: zero orig time -- cannot compute delay/offset");
            } else if (rcvNtpTime.ntpValue() == 0 || xmitNtpTime.ntpValue() == 0) {
                _comments.add("Warning: zero rcvNtpTime or xmitNtpTime");
                // assert destTime >= origTime since network delay cannot be negative
                if (origTime > _returnTime)
                    _comments.add("Error: OrigTime > DestRcvTime");
                else {
                    // without receive or xmit time cannot figure out processing time
                    // so delay is simply the network travel time
                    _delay = Long.valueOf(_returnTime - origTime);
                }
                // TODO: is offset still valid if rcvNtpTime=0 || xmitNtpTime=0 ???
                // Could always hash origNtpTime (sendTime) but if host doesn't set it
                // then it's an malformed ntp host anyway and we don't care?
                // If server is in broadcast mode then we never send out a query in first place...
                if (rcvNtpTime.ntpValue() != 0) {
                    // xmitTime is 0 just use rcv time
                    _offset = Long.valueOf(rcvTime - origTime);
                } else if (xmitNtpTime.ntpValue() != 0) {
                    // rcvTime is 0 just use xmitTime time
                    _offset = Long.valueOf(xmitTime - _returnTime);
                }
            } else {
                long delayValue = _returnTime - origTime;
                // assert xmitTime >= rcvTime: difference typically < 1ms
                if (xmitTime < rcvTime) {
                    // server cannot send out a packet before receiving it...
                    _comments.add("Error: xmitTime < rcvTime"); // time-travel not allowed
                } else {
                    // subtract processing time from round-trip network delay
                    long delta = xmitTime - rcvTime;
                    // in normal cases the processing delta is less than
                    // the total roundtrip network travel time.
                    if (delta <= delayValue) {
                        delayValue -= delta; // delay = (t4 - t1) - (t3 - t2)
                    } else {
                        // if delta - delayValue == 1 ms then it's a round-off error
                        // e.g. delay=3ms, processing=4ms
                        if (delta - delayValue == 1) {
                            // delayValue == 0 -> local clock saw no tick change but destination clock did
                            if (delayValue != 0) {
                                _comments.add("Info: processing time > total network time by 1 ms -> assume zero delay");
                                delayValue = 0;
                            }
                        } else
                            _comments.add("Warning: processing time > total network time");
                    }
                }
                _delay = Long.valueOf(delayValue);
                if (origTime > _returnTime) // assert destTime >= origTime
                    _comments.add("Error: OrigTime > DestRcvTime");

                _offset = Long.valueOf(((rcvTime - origTime) + (xmitTime - _returnTime)) / 2);
            }
        }

        /**
         * Return list of comments (if any) during processing of NTP packet.
         *
         * @return List or null if not yet computed
         */
        public List<String> getComments() {
            return _comments;
        }

        /**
         * Get round-trip network delay. If null then could not compute the delay.
         *
         * @return Long or null if delay not available.
         */
        public Long getDelay() {
            return _delay;
        }

        /**
         * Get clock offset needed to adjust local clock to match remote clock. If null then could not
         * compute the offset.
         *
         * @return Long or null if offset not available.
         */
        public Long getOffset() {
            return _offset;
        }

        /**
         * Returns NTP message packet.
         *
         * @return NTP message packet.
         */
        public NtpV3Impl getMessage() {
            return _message;
        }

        /**
         * Returns time at which time message packet was received by local machine.
         *
         * @return packet return time.
         */
        public long getReturnTime() {
            return _returnTime;
        }

    }

}

