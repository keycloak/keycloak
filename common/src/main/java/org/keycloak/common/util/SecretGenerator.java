package org.keycloak.common.util;

import java.security.SecureRandom;
import java.util.Random;

public class SecretGenerator {

    public static final int SECRET_LENGTH_256_BITS = 32;
    public static final int SECRET_LENGTH_384_BITS = 48;
    public static final int SECRET_LENGTH_512_BITS = 64;

    public static final char[] UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public static final char[] DIGITS = "0123456789".toCharArray();

    public static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private static final SecretGenerator instance = new SecretGenerator();

    private ThreadLocal<Random> random = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new SecureRandom();
        }
    };

    private SecretGenerator() {
    }

    public static SecretGenerator getInstance() {
        return instance;
    }
    
    public String generateSecureID() {
        StringBuilder builder = new StringBuilder(instance.randomBytesHex(16));
        builder.insert(8, '-');
        builder.insert(13, '-');
        builder.insert(18, '-');
        builder.insert(23, '-');
        return builder.toString();
    }

    public String randomString() {
        return randomString(SECRET_LENGTH_256_BITS, ALPHANUM);
    }

    public String randomString(int length) {
        return randomString(length, ALPHANUM);
    }

    public String randomString(int length, char[] symbols) {
        if (length < 1) {
            throw new IllegalArgumentException();
        }
        if (symbols == null || symbols.length < 2) {
            throw new IllegalArgumentException();
        }

        Random r = random.get();
        char[] buf = new char[length];

        for (int idx = 0; idx < buf.length; ++idx) {
            buf[idx] = symbols[r.nextInt(symbols.length)];
        }

        return new String(buf);
    }

    public byte[] randomBytes() {
        return randomBytes(SECRET_LENGTH_256_BITS);
    }

    public byte[] randomBytes(int length) {
        if (length < 1) {
            throw new IllegalArgumentException();
        }

        byte[] buf = new byte[length];
        random.get().nextBytes(buf);
        return buf;
    }

    public String randomBytesHex(int length) {
        final StringBuilder sb = new StringBuilder();
        for (byte b : randomBytes(length)) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit((b & 0xF), 16));
        }
        return sb.toString();
    }

    /**
     * Returns the equivalent length for a destination alphabet to have the same
     * entropy bits than a byte array random generated.
     *
     * @param byteLengthEntropy The desired entropy in bytes
     * @param dstAlphabetLeng The length of the destination alphabet
     * @return The equivalent length in destination alphabet to have the same entropy bits
     */
    public static int equivalentEntropySize(int byteLengthEntropy, int dstAlphabetLeng) {
        return equivalentEntropySize(byteLengthEntropy, 256, dstAlphabetLeng);
    }

    /**
     * Returns the equivalent length for a destination alphabet to have the same
     * entropy bits than another source alphabet.
     *
     * @param length The length of the string encoded in source alphabet
     * @param srcAlphabetLength The length of the source alphabet
     * @param dstAlphabetLeng The length of the destination alphabet
     * @return The equivalent length (same entropy) in destination alphabet for a string of length in source alphabet
     */
    public static int equivalentEntropySize(int length, int srcAlphabetLength, int dstAlphabetLeng) {
        return (int) Math.ceil(length * ((Math.log(srcAlphabetLength)) / (Math.log(dstAlphabetLeng))));
    }
}
