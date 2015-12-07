package com.mrose.ipv6;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * TODO(martinrose) : Add Documentation
 */
public class HexDecode {
  private static final String HEX_ADDR = "26010243C7013700FEAA14FFFE5733C8";

  public static void main(String[] args) throws DecoderException {
    try {
      byte[] bz = Hex.decodeHex(HEX_ADDR.toCharArray());
      System.out.println(bz.length);
      InetAddress ia = InetAddress.getByAddress(bz);
      System.out.println(ia.getCanonicalHostName());
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }
}
