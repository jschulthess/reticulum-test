package com.mobilefabrik.app;

import java.util.Arrays;

// converting Byte Array usning String.format() (slow)
public class ByteToHex4 {

  public static void main(String args[]) {
    byte[] bytes = {11, 5, 19, 56};

    System.out.println("* ByteToHex4 <=> converting Byte Array of to hex using only String.format(...)):");
    System.out.println("Byte Array: "+Arrays.toString(bytes));
    System.out.println("Hex values: ");
    for (byte b : bytes) {
      String str = String.format("%02X", b);
      System.out.print(str+"\t");
    }
    System.out.println("");
    System.out.println("\n");
  }
}
