package com.mobilefabrik.app;

import java.lang.Math;

// converting byte ot hex using builtin Integer.toHexString
public class ByteToHex2 {  

  public static void main(String args[]) {  
    int num1 = 212;  
    // returns the string representation of the unsigned int value  
    // represented by the argument in binary (base 2)  
    System.out.println("* ByteToHex2 <=> converting individual byte to hex directly (Integer.toHexString(...)):");
    System.out.println("Hexadecimal of string "+num1+" is: " + Integer.toHexString(num1));  
    int num2 = 34;  
    System.out.println("Hexadecimal of string "+num2+" is: " + Integer.toHexString(num2));  
    int num3 = -20;  
    System.out.println("Hexadecimal of string "+num3+" is: " + Integer.toHexString(num3));  
    System.out.println("\n");
  }
}
