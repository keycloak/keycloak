package org.keycloak.common.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Supplier;

public class SecretGenerator {

    public static final int SECRET_LENGTH_256_BITS = 32;
    public static final int SECRET_LENGTH_384_BITS = 48;
    public static final int SECRET_LENGTH_512_BITS = 64;
    /**
     * Session ID length in bytes.
     * <p />
     * Both NIST and ANSSI ask for at least 128 bits of entropy, see <a href="https://github.com/keycloak/keycloak/issues/38663">#38663</a>.
     * As we are about to filter those session IDs on each node to find a key of the local segment using Infinispan's org.infinispan.affinity.KeyAffinityServiceFactory,
     * we add some more entropy so that the filtering then leaves enough entropy for those IDs.
     * Usually there are 256 segments in a cache. Just in case someone increases it, we add 16 bits.
     * This should handle the case when a caller connects to one node and generates codes (as it is the case with a keep-alive HTTP connection),
     * instead of a caller connecting to a random node on each request.
     */
    private static final int SESSION_ID_BYTES = 18;
    public static final Supplier<String> SECURE_ID_GENERATOR = () -> getInstance().generateBase64SecureId(SESSION_ID_BYTES);

    public static final char[] UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public static final char[] DIGITS = "0123456789".toCharArray();

    public static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private static final SecretGenerator instance = new SecretGenerator();

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SecretGenerator() {
    }

    public static SecretGenerator getInstance() {
        return instance;
    }
    
    public String generateSecureID() {
        return generateSecureUUID().toString();
    }

    public String generateBase64SecureId(int nBytes) {
        assert nBytes > 0;
        byte[] data = new byte[nBytes];
        SECURE_RANDOM.nextBytes(data);
        String id = Base64.getUrlEncoder().encodeToString(data);

        // disallow a dot, as a dot is used as a separator in AuthenticationSessionManager.decodeBase64AndValidateSignature
        assert !id.contains(".");

        // disallow a space, as session_state must not contain a space (see https://openid.net/specs/openid-connect-session-1_0.html#CreatingUpdatingSessions)
        assert !id.contains(" ");

        return id;
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

        char[] buf = new char[length];

        for (int idx = 0; idx < buf.length; ++idx) {
            buf[idx] = symbols[SECURE_RANDOM.nextInt(symbols.length)];
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
        SECURE_RANDOM.nextBytes(buf);
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
