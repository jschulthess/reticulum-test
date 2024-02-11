package com.mobilefabrik.app;

import java.util.Arrays;

// converting Byte Array usning STringBuilder and String.format() (slow)
public class ByteToHex3 {  

  public static void main(String args[]) {
    byte[] byteArray = {0, 9, 3, -1, 5, 8, -2};
    StringBuilder sb = new StringBuilder();

    System.out.println("* ByteToHex3 <=> converting Byte Array of to hex using StringBuilder and  String.format(...)):");
    System.out.println("Byte Array: "+Arrays.toString(byteArray));
    for (byte b : byteArray) {
      sb.append(String.format("%02X ", b));
    }
    System.out.println("Hex values: "+sb.toString());
    System.out.println("\n");
  }
}
