package com.mrose.prime;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;


/**
 * Created with IntelliJ IDEA. User: mrose Date: 2/5/14 Time: 10:34 AM To change this template use
 * File | Settings | File Templates.
 */
public class ReadCertificate {

  private static final Logger log = LoggerFactory.getLogger(ReadCertificate.class);

  public static void main(String[] args) {
    try {
      X509Certificate cert = readWWDRCertificate("/tmp/gmail.crt");
      sun.security.rsa.RSAPublicKeyImpl pubKey = (sun.security.rsa.RSAPublicKeyImpl) cert
          .getPublicKey();

      pubKey.getAlgorithm();
      pubKey.getPublicExponent();

      String algo = pubKey.getAlgorithm();
      BigInteger modulus = new BigInteger(pubKey.getModulus().toByteArray());
      BigInteger pubExponent = new BigInteger(pubKey.getPublicExponent().toByteArray());

      log.warn("Algorithm: " + algo);
      log.warn("Modulus: " + modulus.toString());
      log.warn("Public Exponent: " + pubExponent.toString());


      log.warn("Bits: " + sqrt(modulus).toString(2).length());

      for (long l = 2; ; l++) {
        BigInteger bi = BigInteger.valueOf(l);
        BigInteger remainder = modulus.mod(bi);
        if (remainder.compareTo(ZERO) == 0) {
          log.info("" + l + " => " + remainder);
        }
      }
    } catch (Throwable t) {
      log.warn(t.getMessage(), t);
    }
  }

  // Compare ONE.compareTo(TWO) = -1
  // Compare TWO.compareTo(ONE) = 1
  private static final BigInteger ZERO = BigInteger.valueOf(0);
  private static final BigInteger ONE = BigInteger.valueOf(1);
  private static final BigInteger TWO = BigInteger.valueOf(2);
  private static final BigInteger THREE = BigInteger.valueOf(3);
  private static final BigInteger FOUR = BigInteger.valueOf(4);
  private static final BigInteger FIVE = BigInteger.valueOf(5);

  public static BigInteger sqrt(BigInteger value) {
    return sqrti(value, ONE, value);
  }

  public static BigInteger sqrti(BigInteger result, BigInteger lowGuess, BigInteger highGuess) {
    int iter = 0;
    while (highGuess.subtract(lowGuess).compareTo(FIVE) > 0) {
      iter++;
      BigInteger midpoint = highGuess.subtract(lowGuess).divide(TWO).add(lowGuess);

      if (midpoint.multiply(midpoint).compareTo(result) < 0) {
        // midpoint*midpoint < target (guess is too low)
        lowGuess = midpoint;
      } else {
        // midpoint*midpoint > target (guess is too high)
        highGuess = midpoint;
      }
    }
    return highGuess;
  }

  public static X509Certificate readWWDRCertificate(String keyFile)
      throws IOException, KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException {
    FileInputStream fis = null;
    ByteArrayInputStream bais = null;
    try {
      // use FileInputStream to read the file
      fis = new FileInputStream(keyFile);

      // read the bytes
      byte value[] = new byte[fis.available()];
      fis.read(value);
      bais = new ByteArrayInputStream(value);

      // get X509 certificate factory
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

      // certificate factory can now create the certificate
      return (X509Certificate) certFactory.generateCertificate(bais);
    } finally {
      IOUtils.closeQuietly(fis);
      IOUtils.closeQuietly(bais);
    }
  }
}
