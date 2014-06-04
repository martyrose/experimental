package com.mrose.prime;

import com.mrose.math.BigInteger;
import com.mrose.random.MersenneTwister;
import com.mrose.util.Log;
import com.mrose.util.LogFactory;
import java.security.SecureRandom;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 2/15/12
 * Time: 9:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class SemiPrimeFactor {
    protected static transient Log log = LogFactory.getLog(SemiPrimeFactor.class);


    public static void main(String[] args) {
        byte[] seed = SecureRandom.getSeed(8);
        MersenneTwister twister = new MersenneTwister(toLong(seed));
        BigInteger prime1 = BigInteger.probablePrime(32, twister);
        BigInteger prime2 = BigInteger.probablePrime(32, twister);
        log.warn(prime1);
        log.warn(prime2);
        BigInteger semiprime = prime1.multiply(prime2);

        log.warn(semiprime);
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
