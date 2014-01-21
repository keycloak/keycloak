package org.keycloak.models.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * <p>
 * Password that uses SHA to encode passwords. You can always change the SHA strength by specifying a valid
 * integer when creating a new instance.
 * </p>
 * <p>Passwords are returned with a Base64 encoding.</p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 *
 */
public class SHAPasswordEncoder {

    private int strength;

    public SHAPasswordEncoder(int strength) {
        this.strength = strength;
    }

    public String encode(String rawPassword, String salt) {
        MessageDigest messageDigest = getMessageDigest();

        // TODO: externalize it, so that it can be configured by the application
        int iterations = 5000;

        // TODO: externalize it, perhaps generating it on first-deploy with SecureRandom
        String pepper = "fNY07rZXP2epfJxrvgw6l2wAmZ6uadqX";

        String encodedPassword = pepper + salt + rawPassword;

        try {
            for (int i = 0 ; i < iterations ; i++) {
                encodedPassword += salt;
                byte[] digest = messageDigest.digest(encodedPassword.getBytes("UTF-8"));
                encodedPassword = Base64.encodeBytes(digest);
            }

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Credential could not be encoded");
        }

        return encodedPassword;
    }

    public boolean verify(String rawPassword, String encodedPassword, String salt) {
        return encode(rawPassword, salt).equals(encodedPassword);
    }

    protected final MessageDigest getMessageDigest() throws IllegalArgumentException {
        String algorithm = "SHA-" + this.strength;

        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("invalid credential encoding algorithm");
        }
    }

    public int getStrength() {
        return this.strength;
    }
}
