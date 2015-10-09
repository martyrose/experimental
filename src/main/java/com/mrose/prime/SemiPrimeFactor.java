package com.mrose.prime;

import com.google.common.math.BigIntegerMath;

import com.mrose.math.BigInteger;
import com.mrose.random.MersenneTwister;
import com.mrose.util.Log;
import com.mrose.util.LogFactory;

import java.math.RoundingMode;
import java.security.SecureRandom;

/**
 * http://asecuritysite.com/Encryption/rsa
 */
public class SemiPrimeFactor {

  protected static transient Log log = LogFactory.getLog(SemiPrimeFactor.class);


  public static void main(String[] args) throws Exception {
    byte[] seed = SecureRandom.getSeed(8);
    MersenneTwister twister = new MersenneTwister(toLong(seed));
    final int BIT_PRIMES = 64;
    BigInteger prime1 = BigInteger.probablePrime(BIT_PRIMES, twister);
    BigInteger prime2 = BigInteger.probablePrime(BIT_PRIMES, twister);
    BigInteger modulus = prime1.multiply(prime2);

    log.warn(prime1);
    log.warn(prime2);
    log.warn(modulus);

    // Convert to java.math.BigInteger to call square root helper, add two, then convert back to custom
    // BigInteger impl
    BigInteger searchSpot = new BigInteger(BigIntegerMath
        .sqrt(new java.math.BigInteger(modulus.toString(16), 16), RoundingMode.CEILING).add(
            java.math.BigInteger.ONE).add(java.math.BigInteger.ONE)
        .toString(16), 16);
    BigInteger bestRemainder = searchSpot;
    BigInteger threshold = new BigInteger("256", 10);

    do {
      searchSpot = searchSpot.subtract(BigInteger.ONE);
      BigInteger remainder = modulus.mod(searchSpot);
//    log.warn(modulus + " % " + searchSpot + " == " + remainder);
//      log.warn("Best Remainder.1: " + bestRemainder);
      if (remainder.compareTo(bestRemainder) == -1) {
        System.out.println("New Best Remainder: " + bestRemainder);
        bestRemainder = remainder;
      }
//      log.warn("Best Remainder.2: " + bestRemainder);

//      log.warn("Comparison: " + bestRemainder.compareTo(threshold));
//      Thread.sleep(1000);
    } while (bestRemainder.compareTo(threshold) == 1);
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
