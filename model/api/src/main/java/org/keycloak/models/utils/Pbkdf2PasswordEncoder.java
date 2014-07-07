package org.keycloak.models.utils;

import net.iharder.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * <p>
 * Encoder that uses PBKDF2 function to cryptographically derive passwords.
 * </p>
 * <p>Passwords are returned with a Base64 encoding.</p>
 *
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 *
 */
public class Pbkdf2PasswordEncoder {

    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";
    public static final String RNG_ALGORITHM = "SHA1PRNG";

    private static final int DERIVED_KEY_SIZE = 512;
    private static final int ITERATIONS = 1;

    private final int iterations;
    private byte[] salt;

    public Pbkdf2PasswordEncoder(byte[] salt, int iterations) {
        this.salt = salt;
        this.iterations = iterations;
    }

    public Pbkdf2PasswordEncoder(byte[] salt) {
        this(salt, ITERATIONS);
    }

    /**
     * Encode the raw password provided
     * @param rawPassword The password used as a master key to derive into a session key
     * @return encoded password in Base64
     */
    public String encode(String rawPassword, int iterations) {

        String encodedPassword;

        KeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, DERIVED_KEY_SIZE);

        try {
            byte[] key = getSecretKeyFactory().generateSecret(spec).getEncoded();
            encodedPassword = Base64.encodeBytes(key);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Credential could not be encoded");
        }

        return encodedPassword;
    }

    public String encode(String rawPassword) {
        return encode(rawPassword, iterations);
    }

    /**
     * Encode the password provided and compare with the hash stored into the database
     * @param rawPassword The password provided
     * @param encodedPassword Encoded hash stored into the database
     * @return true if the password is valid, otherwise false for invalid credentials
     */
    public boolean verify(String rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }

    /**
     * Encode the password provided and compare with the hash stored into the database
     * @param rawPassword The password provided
     * @param encodedPassword Encoded hash stored into the database
     * @return true if the password is valid, otherwise false for invalid credentials
     */
    public boolean verify(String rawPassword, String encodedPassword, int iterations) {
        return encode(rawPassword, iterations).equals(encodedPassword);
    }

    /**
     * Generate a salt for each password
     * @return cryptographically strong random number
     */
    public static byte[] getSalt() {
        byte[] buffer = new byte[16];

        SecureRandom secureRandom;

        try {
            secureRandom = SecureRandom.getInstance(RNG_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RNG algorithm not found");
        }

        secureRandom.nextBytes(buffer);

        return buffer;
    }

    private static SecretKeyFactory getSecretKeyFactory() {
        try {
            return SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("PBKDF2 algorithm not found");
        }
    }
}
