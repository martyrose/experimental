package com.mrose.prime;

import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;

import com.mrose.math.BigInteger;
import com.mrose.random.MersenneTwister;
import com.mrose.util.Log;
import com.mrose.util.LogFactory;

import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * http://asecuritysite.com/Encryption/rsa
 */
public class SemiPrimeFactor {

  protected static transient Log log = LogFactory.getLog(SemiPrimeFactor.class);


  public static void main(String[] args) throws Exception {
    byte[] seed = SecureRandom.getSeed(8);
    MersenneTwister twister = new MersenneTwister(toLong(seed));
    BigInteger prime1 = BigInteger.probablePrime(32, twister);
    BigInteger prime2 = BigInteger.probablePrime(32, twister);
    BigInteger modulus = prime1.multiply(prime2);

    log.warn(modulus);

    Set<BigInteger> smallPrimes = readPrimes();
    List<Pair<BigInteger, BigInteger>> remainders = new ArrayList<>(smallPrimes.size());
    for(BigInteger prime: smallPrimes) {
      BigInteger remainder = modulus.remainder(prime);
      remainders.add(Pair.of(prime, remainder));
    }
    Collections.sort(remainders, new Comparator<Pair<BigInteger, BigInteger>>() {
      @Override
      public int compare(Pair<BigInteger, BigInteger> o1, Pair<BigInteger, BigInteger> o2) {
        return o1.getValue().compareTo(o2.getValue());
      }
    });
    log.warn(remainders);
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

  public static Set<BigInteger> readPrimes() throws IOException {
    Set<BigInteger> results = new HashSet<>();
    try (InputStream is = new FileInputStream("/tmp/primes.txt")) {
      List<String> linez = CharStreams.readLines(new InputStreamReader(is, StandardCharsets.UTF_8));
      for(String line: linez) {
        List<String> values = Splitter.on(' ').omitEmptyStrings().splitToList(line);
        for(String v: values) {
          results.add(new BigInteger(v));
        }
      }
      return results;
    }
  }
}
