package com.mrose.prime;

import com.google.common.math.BigIntegerMath;

import com.mrose.math.BigInteger;
import com.mrose.random.MersenneTwister;
import com.mrose.util.Log;
import com.mrose.util.LogFactory;

import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.BitSet;

/**
 * http://asecuritysite.com/Encryption/rsa
 */
public class SemiPrimeFactor {

  protected static transient Log log = LogFactory.getLog(SemiPrimeFactor.class);


  public static void main(String[] args) throws Exception {
    byte[] seed = SecureRandom.getSeed(8);
    MersenneTwister twister = new MersenneTwister(toLong(seed));
    final int BIT_PRIMES = 32;
    BigInteger prime1 = BigInteger.probablePrime(BIT_PRIMES, twister);
    BigInteger prime2 = BigInteger.probablePrime(BIT_PRIMES, twister);
    BigInteger modulus = prime1.multiply(prime2);

    BigInteger sqrt = new BigInteger(
        BigIntegerMath.sqrt(new java.math.BigInteger(modulus.toByteArray()), RoundingMode.CEILING)
            .toByteArray());

    BigInteger bi = BigInteger.ONE.add(BigInteger.ONE).pow(BIT_PRIMES-1);
    log.warn("Prime 1: " + prime1.toString(10));
    log.warn("Prime 2: " + prime2.toString(10));
    log.warn("BS: " + bi.toString(10));

    log.warn("Modulus: " + modulus.toString(10));
    log.warn("SQRT: " + sqrt.toString(10));
    log.warn("ReMultiply SQRT: " + sqrt.multiply(sqrt).toString(10));


    log.warn("Search_Space: " + sqrt.subtract(bi).toString(10));

    log.warn("#### " + modulus.mod(prime1));
    log.warn("#### " + modulus.mod(prime2));
    log.warn("#### " + modulus.mod(BigInteger.probablePrime(BIT_PRIMES, twister)));

  }

  public static long toLong(byte[] data) {
    if (data == null || data.length != 8) {
      return 0x0;
    }
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
            (long) (0xff & data[7]) << 0);
  }

}
