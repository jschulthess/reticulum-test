package com.mobilefabrik.app;

// converting byte ot hex using builtin Integer.toHexString
public class ByteToHex1 {  

  public static void main(String args[]) {
    byte num = (byte)4556;
    System.out.println("\n");
    System.out.println("* ByteToHex1 <=> converting byte to hex directly (Integer.toHexString(...)):");
    System.out.println("Byte = "+num);
    int hex = num & 0xFF;
    System.out.println("Hexadecimal Equivalent= "+Integer.toHexString(hex));
    System.out.println("\n");
  }
}
