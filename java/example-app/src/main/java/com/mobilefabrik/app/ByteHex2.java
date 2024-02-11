package com.mobilefabrik.app;

import java.util.Arrays;
import org.apache.commons.codec.binary.Hex;

// ref: https://www.baeldung.com/java-byte-arrays-hex-strings
// 
public class ByteHex2 {

  public static String byteToHex(byte num) {
    // we created a char array of length 2 to store the output:
    char[] hexDigits = new char[2];
    // we isolated higher order bits by right shifting 4 bits. And then, we applied a mask to isolate lower order 4 bits.
    // Masking is required because negative numbers are internally represented as two’s complement of the positive number.
    hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
    // we convert the remaining 4 bits to hexadecimal
    hexDigits[1] = Character.forDigit((num & 0xF), 16);
    return new String(hexDigits);
  }

  public static byte hexToByte(String hexString) {
    // we converted hexadecimal characters into integers
    int firstDigit = toDigit(hexString.charAt(0));
    int secondDigit = toDigit(hexString.charAt(1));
    // we left shifted most significant digit by 4 bits.
    // Consequently, the binary representation has zeros at four least significant bits.
    return (byte) ((firstDigit << 4) + secondDigit);
  }

  private static int toDigit(char hexChar) {
    int digit = Character.digit(hexChar, 16);
    if(digit == -1) {
      throw new IllegalArgumentException(
        "Invalid Hexadecimal Character: "+ hexChar);
    }
    return digit;
  }

  public static String byteArrayToHexString(byte[] bytes) {
    return Hex.encodeHexString(bytes);
  }

  public static char[] byteArrayToHexChars(byte[] bytes) {
    return Hex.encodeHex(bytes);
  }

  public static byte[] hexStringToByteArray(String data) {
    byte[] byteArray;
    try {
        byteArray = Hex.decodeHex(data);
        return byteArray;
    } catch (Exception e) {
        System.out.println("failed to convert string: "+data);
        throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {

    byte[] bytes = {10, 2, 15, 11};
    StringBuilder output = new StringBuilder();
    String outString = new String();;
    //char[] hexChars = new char[b.length * 2];

    System.out.println("* ByteHex2 <=> converting byte to hex using byte operation (both ways):");
    //System.out.println("bytes: {"+bytes[0]+", "+bytes[1]+", "+bytes[2]+", "+bytes[3]+"]");
    System.out.println("bytes: "+Arrays.toString(bytes));
    for (byte b: bytes) {
      System.out.println(byteToHex(b));
    }
    System.out.println("");

    System.out.println("byes converted to hex and back again:");
    for (byte b: bytes) {
      System.out.println(hexToByte(byteToHex(b)));
      output.append(byteToHex(b));
      outString += byteToHex(b);
    }
    System.out.println("constructed byteToHex string: "+output);
    System.out.println("");

    System.out.println("converted byte Array "+Arrays.toString(bytes)+" to hex string using byteArrayToHexString(bytes): "+byteArrayToHexString(bytes));
    System.out.println("");

    System.out.println("decode hex string "+outString+" to byte array using hexStringToByteArray: "+Arrays.toString(hexStringToByteArray(outString)));
    System.out.println("");
  }
}
