package com.mobilefabrik.app;

public class ByteHex1 {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void main(String[] args) {

        byte[] bytes = {10, 2, 15, 11};

        System.out.println("* ByteHex1 <=> converting byte to hex string using byte operation (one way):");
        String s = bytesToHex(bytes);
        System.out.println(s+"\n");

    }
}
