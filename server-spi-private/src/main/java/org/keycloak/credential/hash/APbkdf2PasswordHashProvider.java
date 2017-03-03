package org.keycloak.credential.hash;

import org.keycloak.common.util.Base64;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.UserCredentialModel;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Abstract class providing core encoding and verification functions for PBKDF2 password hash algorithms with HMAC.
 *
 * @author <a href="mailto:abkaplan07@gmail.com">Adam Kaplan</a>
 */
public abstract class APbkdf2PasswordHashProvider implements PasswordHashProvider {

    @Override
    public boolean policyCheck(PasswordPolicy policy, CredentialModel credential) {
        return credential.getHashIterations() == policy.getHashIterations() && getAlgorithmAlias().equals(credential.getAlgorithm());
    }

    @Override
    public void encode(String rawPassword, PasswordPolicy policy, CredentialModel credential) {
        byte[] salt = getSalt();
        String encodedPassword = encode(rawPassword, policy.getHashIterations(), salt);

        credential.setAlgorithm(getAlgorithmAlias());
        credential.setType(UserCredentialModel.PASSWORD);
        credential.setSalt(salt);
        credential.setHashIterations(policy.getHashIterations());
        credential.setValue(encodedPassword);
    }

    @Override
    public boolean verify(String rawPassword, CredentialModel credential) {
        return encode(rawPassword, credential.getHashIterations(), credential.getSalt()).equals(credential.getValue());
    }

    public void close() {
    }

    /**
     * The alias for the PBKDF2 algorithm, which is displayed in the Keycloak Admin console.
     */
    protected abstract String getAlgorithmAlias();

    /**
     * The identity of the PBKDF2 algorithm used by the Java Runtime Environment.
     *
     * @return a valid algorithm ID for the JRE. See <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SecretKeyFactory">SecretKeyFactory standard names</a>
     */
    protected abstract String getPbkdf2Algorithm();

    /**
     * The maximum encoded key size for the algorithm.
     *
     * @return the max encoded key size, in bytes.
     */
    protected abstract int getDerivedKeySize();

    protected String encode(String rawPassword, int iterations, byte[] salt) {
        KeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, getDerivedKeySize());

        try {
            byte[] key = getSecretKeyFactory().generateSecret(spec).getEncoded();
            return Base64.encodeBytes(key);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Credential could not be encoded", e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected byte[] getSalt() {
        byte[] buffer = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(buffer);
        return buffer;
    }

    private SecretKeyFactory getSecretKeyFactory() {
        try {
            return SecretKeyFactory.getInstance(getPbkdf2Algorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("PBKDF2 algorithm not found", e);
        }
    }
}
