package com.accertify.prime;

import com.accertify.util.Log;
import com.accertify.util.LogFactory;
import sun.security.util.BigInt;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Created by IntelliJ IDEA.
 * User: mrose
 * Date: 2/15/12
 * Time: 9:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class PrimeFind {
    protected static transient Log log = LogFactory.getLog(PrimeFind.class);

    
    public static void main(String[] args) {
        byte[] seed = SecureRandom.getSeed(8);
        MersenneTwister twister = new MersenneTwister(toLong(seed));
        BigInteger prime1 = BigInteger.probablePrime(256, twister);
        BigInteger prime2 = BigInteger.probablePrime(256, twister);
        log.warn(prime1);
        log.warn(prime2);
        BigInteger composite = prime1.multiply(prime2);

        log.warn(composite);
        log.warn(composite.remainder(new BigInteger("3")));
    }


    class Factors {
        
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
