package com.mobilefabrik.app;

import java.util.Arrays;

// converting byte ot hex using Byte operation
public class ByteToHex5 {  
  //defining character array of hexadecimal values  

  private final static char[] hexArray = "0123456789ABCDEF".toCharArray();  
  //function to convert byte to hex  
  public static String bytesToHex(byte[] bytes) {  
    char[] hexChars = new char[bytes.length * 2];  
    for ( int j = 0; j < bytes.length; j++ ) {  
      int v = bytes[j] & 0xFF;  
      hexChars[j * 2] = hexArray[v >>> 4];  
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];  
    }  
    //returns hexadecimal string  
    return new String(hexChars);  
  }  

  //driver code  
  public static void main(String args[]) {  
    byte[] bytes = {23, 21, 18, 11};  
    String str = bytesToHex(bytes);  
    System.out.println("* ByteToHex5 <=> converting byte to hex using byte operation):");
    System.out.println("Byte Array: "+Arrays.toString(bytes));
    //loop iterate over the array  
    System.out.println("Hex values: ");
    for (byte b : bytes) {  
      //converts the byte array to hex value   
      String s = String.format("%02X", b);  
      //prints the corresponding hexadecimal value  
      System.out.print(s+"\t");  
    }  
    System.out.print("\n\n");
  }  
}  
