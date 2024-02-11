package com.mobilefabrik.app;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;
//import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.subarray;

@Slf4j
public class HMacTest {

    public static final String ALGORITHM = "HmacSHA256";
    public static byte[] raw = new byte[]{-44, 51, -1, 24, 117, -91, -79, -113, -80, -56, 4, 67, -21, 117, -42, 125, -55, 11, 109, -35, -63, 67, 55, -33, 51, -55, -127, -70, -30, -14, 31, -98, -108, 118, 86, 95, -85, 84, 97, -112, -108, 29, -48, 48, 38, 96, 30, -47, 33, -39, 83, 53, 31, -100, 33, 126, 51, 124, 99, -53, -85, 110, 67, 127, -88, -111, 110, 29, -52, 44, -32, 77, -98, -80, 107, -113, 64, 117, -94, 44, 127, 88, -107, 102, 38, -92, -25, -97, 43, -65, -34, -34, -39, 51, -29, 43, -44, -71, 50, -33, 32, 103, -75, -92, 75, 42, -123, 49, -80, -110, -83, -33, 12, -41, -50, 51, 77, -17, -118, -126, 63, -40, 40, 22, 115, 57, 47, 58, -47, -74, -31, 125, 94, -33, -11, -71, -68, -24, 69, -69, 87, -55, -68, -74, -50, 21, 55, 114, 36, -86, -80, 119, 101, 64, 98, 69, 73, -27, 91, -10, -58, 30, 80, 67, 117, -107, 25, -48, 22, 89, -82, 65, 11, -23, 117, 23, 17, -79, 60, 73, 123, -108, 68, -68, -64, -118, 127, -43, 81, 127, -44, 81, -121, 60, 20, -83, -83, -33, 64, -9, 74, -99, -92, -111, 46};

    public static String calculateHMac(String key, String data) throws Exception {
        Mac sha256_HMAC = Mac.getInstance(ALGORITHM);

        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), ALGORITHM);
        sha256_HMAC.init(secret_key);
        //log.debug("secret_key: {}", secret_key.toString());

        byte[] hmac = sha256_HMAC.doFinal(data.getBytes("UTF-8"));
        log.debug("hmac: {}", hmac);
        return byteArrayToHex(hmac);
        //return byteArrayToHex(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static void main(String [] args) throws Exception {
        // see https://en.wikipedia.org/wiki/HMAC#Examples
        // expected output:
        // HMAC_SHA256("key", "The quick brown fox jumps over the lazy dog") = f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8
        //System.out.println(calculateHMac("key", "The quick brown fox jumps over the lazy dog"));
        //System.out.println(calculateHMac("key", "qortal"));
        log.info("HMac for key 'qortal': {}", calculateHMac("key", "qortal"));

        //final byte[] raw = [0];
        var ifac = subarray(raw, 2, 2 + 16);
        log.trace("xyz - Transmit.inbound - ifac: {}, raw: {}, length: {}", ifac, raw, raw.length);
        
    }
}
