package com.mobilefabrik.app;

/**
 * SHA256 from string
 *
 */

import org.apache.commons.codec.digest.DigestUtils;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.digest.DigestUtils.sha256;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;

public class Sha256App {
    public static String data = new String("qortal");

    public static byte[] fullHash(final byte[] data) {
        return DigestUtils.getSha256Digest().digest(data);
    }

    public static byte[] getSha256Bytes(String value) {
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(value.getBytes(UTF_8));
            byte[] digested = md.digest();
            return digested;
            //return md.digest(value.getBytes(UTF_8));
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
     }

    public static String getSha256(String value) {
       try{
           MessageDigest md = MessageDigest.getInstance("SHA-256");
           md.update(value.getBytes(UTF_8));
           byte[] digested = md.digest();
           //return bytesToHex(md.digest());
           //return bytesTyHex(digested);
           StringBuffer sb = new StringBuffer();
           for (int i = 0; i < digested.length; i++) {
               //sb.append(Integer.toHexString(0xff & digested[i]));
               sb.append(String.format("%02x", 0xff & digested[i]));
           }
           return sb.toString();
       } catch(Exception ex){
           throw new RuntimeException(ex);
       }
    }

    public static int[] signedToUnsigned(byte[] input) {
        var result = new int[input.length+1];
        //var x = new byte[]{};
        int i;
        for (i=0; i < input.length; i++) {
            //ArrayUtils.add(x, (byte) (input[i] & 0xff));
            result[i] =  input[i] & 0xFF;
            //result[i] = String.format("%02x", 0xff & input[i]);
            //String.format("%02x", 0xff & digested[i])
        }
        //return x;
        return result;
    }

    //private static String bytesToHex(byte[] bytes) {
    //   StringBuffer result = new StringBuffer();
    //   for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
    //   return result.toString();
    //}
    //
    //public synchronized static String encrypt(String hash) {
    //    try {
    //
    //        StringBuilder builder = new StringBuilder();
    //        builder.append(hash);
    //
    //        // first time , encrypt user name , password and static key
    //        String encryptedCredentials = encryptionIterator(builder.toString());
    //       return encryptedCredentials;
    //    } 
    //
    //    catch (Exception e) {
    //        e.printStackTrace();
    //    }
    //
    //    return "";
    //}
    //
    //private static String encryptionIterator(String content) {
    //    try {
    //        var sha256 = MessageDigest.getInstance("SHA-256");
    //        // append the static key to each iteration
    //        byte[] passBytes = (content).getBytes();
    //        sha256.reset();
    //        byte[] digested = sha256.digest(passBytes);
    //        StringBuffer sb = new StringBuffer();
    //        for (int i = 0; i < digested.length; i++) {
    //            //sb.append(Integer.toHexString(0xff & digested[i]));
    //            sb.append(String.format("%02x", 0xff & digested[i]));
    //        }
    //        return sb.toString();
    //    } catch (NoSuchAlgorithmException ex) {
    //        ex.printStackTrace();
    //    }
    //    return "";
    //}

    public static void main( String[] args ) {

        Sha256App app = new Sha256App();
        byte[] unsignedArray = new byte[]{};
        byte[] sha256Array = sha256(data);
        byte[] hexArray = new byte[]{};
        String hexString = new String("725980de35b1f113754cd3f951f2f13c075178dfb89b57c3f887f391d9a0d060");
        //try {
        //    System.out.println("yyy - "+Arrays.toString(MessageDigest.getInstance("SHA-256").digest(data.getBytes(UTF_8))));
        //} catch (Exception e) {
        //    System.out.println("failed.");
        //}
        //String encrypt = encrypt(hexString);
        //System.out.println("Your Password Is '" + encrypt + "'");

        byte [] fullHash = Sha256App.fullHash(data.getBytes(UTF_8));
        System.out.println("has for "+data+" using org.oracle.commons.codec.digest.DigestUtils: ");
        System.out.println( "hash (fullHash) for 'qortal': "+Arrays.toString(fullHash) );
        System.out.println( "hash (sha256()) for 'qortal': "+Arrays.toString(sha256(data)) );
        System.out.println( "array unsigned: "+Arrays.toString( signedToUnsigned(sha256(data)) ));
        

        System.out.println("\nhash for"+data+" using java.security.MessageDigest: ");
        System.out.println( "hash (getSha256() using java.security.MessageDigest) for 'qortal': "+Arrays.toString(getSha256Bytes(data)));
        System.out.println( "hash (getSha256() using java.security.MessageDigest) for 'qortal': "+getSha256(data));
        try {
            hexArray = Hex.decodeHex(getSha256(data));
            System.out.println( "hash (getSha256() using java.security.MessageDigest) for 'qortal': "+Arrays.toString(Hex.decodeHex(getSha256(data))));
            //System.out.println("hash: "+Arrays.toString(Hex.decodeHex(hexString)));
        } catch (DecoderException e) {
            System.out.println("error decoding data");
        }
        // fix: https://stackoverflow.com/questions/51444782/sha256-encryption-in-java-and-python-produce-different-results
        //sb.append(String.format("%02x", 0xff & digested[i]));
 
    }
}
