package io.reticulum.utils;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtocolUtils {

    public String byteToHex(byte num) {
        // we created a char array of length 2 to store the output:
        char[] hexDigits = new char[2];
        // we isolated higher order bits by right shifting 4 bits. And then, we applied a mask to isolate lower order 4 bits.
        // Masking is required because negative numbers are internally represented as two’s complement of the positive number.
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        // we convert the remaining 4 bits to hexadecimal
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
      }
    
      public byte hexToByte(String hexString) {
        // we converted hexadecimal characters into integers
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        // we left shifted most significant digit by 4 bits.
        // Consequently, the binary representation has zeros at four least significant bits.
        return (byte) ((firstDigit << 4) + secondDigit);
      }
    
      private int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if(digit == -1) {
          throw new IllegalArgumentException(
            "Invalid Hexadecimal Character: "+ hexChar);
        }
        return digit;
      }

      public String byteArrayToHexString(byte[] bytes) {
        return Hex.encodeHexString(bytes);
      }

      public char[] byteArrayToHexChars(byte[] bytes) {
        return Hex.encodeHex(bytes);
      }

      public byte[] hexStringToByteArray(String data) {
        byte[] byteArray;
        try {
            byteArray = Hex.decodeHex(data);
            return byteArray;
        } catch (Exception e) {
            log.error(data, e);
            throw new RuntimeException(e);
        }
      }
}

