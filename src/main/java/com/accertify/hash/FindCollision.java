package com.accertify.hash;

import com.accertify.random.MersenneTwister;
import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 *
 */
public class FindCollision {
    protected static transient Log log = LogFactory.getLog(FindCollision.class);

    public static void main(String[] args) {
        int digestOutputBits = 256;
        int inputSizeBits = digestOutputBits*2;
        int inputSizeBytes = inputSizeBits / 8;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            MersenneTwister twister = new MersenneTwister(toLong(SecureRandom.getSeed(8)));
            byte[] values = new byte[inputSizeBytes];
            for( int i=0; i<inputSizeBytes; i++ ) {
                values[i] = twister.nextByte();
            }
            byte[] digest = md.digest(values);
            log.warn(Hex.encodeHexString(values) + " => " + Hex.encodeHexString(digest));
        } catch (NoSuchAlgorithmException e) {
            ;
        }
    }

    public static long toLong(byte[] data) {
        if (data == null || data.length != 8) return 0x0;
        // ----------
        return (
                // (Below) convert to longs before shift because digits
                //         are lost with ints beyond the 32-bit limit
                (long) (0xff & data[0]) << 56 |
                        (long) (0xff & data[1]) << 48 |
                        (long) (0xff & data[2]) << 40 |
                        (long) (0xff & data[3]) << 32 |
                        (long) (0xff & data[4]) << 24 |
                        (long) (0xff & data[5]) << 16 |
                        (long) (0xff & data[6]) << 8 |
                        (long) (0xff & data[7]) << 0
        );
    }
}
