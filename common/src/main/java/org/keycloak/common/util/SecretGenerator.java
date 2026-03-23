package org.keycloak.common.util;

import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

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
        return generateSecureUUID().toString();
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

    /**
     * Returns a pseudo-UUID where all bits are random to have a maximum entropy.
     *
     * @return UUID with all bits random
     */
    public UUID generateSecureUUID() {
        byte[] data = randomBytes(16);
        return new UUID(toLong(data, 0), toLong(data, 8));
    }

    private static long toLong(byte[] data, int offset) {
        return  ((data[offset] & 0xFFL) << 56) |
                ((data[offset + 1] & 0xFFL) << 48) |
                ((data[offset + 2] & 0xFFL) << 40) |
                ((data[offset + 3] & 0xFFL) << 32) |
                ((data[offset + 4] & 0xFFL) << 24) |
                ((data[offset + 5] & 0xFFL) << 16) |
                ((data[offset + 6] & 0xFFL) <<  8) |
                ((data[offset + 7] & 0xFFL)) ;
    }

}
