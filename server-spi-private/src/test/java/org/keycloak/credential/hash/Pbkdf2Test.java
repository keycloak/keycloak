package org.keycloak.credential.hash;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.util.Base64;
import org.keycloak.credential.CredentialModel;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for PBKDF2 password hash providers
 *
 * @author <a href="mailto:abkaplan07@gmail.com">Adam Kaplan</a>
 */
public class Pbkdf2Test {

    private SecureRandom random;
    private APbkdf2PasswordHashProvider hashProvider;

    @Before
    public void setUp() {
        random = new SecureRandom();
    }

    @Test
    public void testSha1HashProvider() {
        hashProvider = new Pbkdf2PasswordHashProvider();
        checkPasswordHashProvider(2);
    }

    @Test
    public void testSha256HashProvider() {
        hashProvider = new Pbkdf2Sha256PasswordHashProvider();
        checkPasswordHashProvider(2);
    }

    @Test
    public void testSha512HashProvider() {
        hashProvider = new Pbkdf2Sha512PasswordHashProvider();
        checkPasswordHashProvider(2);
    }

    private void checkPasswordHashProvider(int iterations) {
        byte[] salt = new byte[4];
        random.nextBytes(salt);
        String rawPassword = "test1234!!";
        KeySpec keySpec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, hashProvider.getDerivedKeySize());
        try {
            String expectedEncoded = Base64.encodeBytes(SecretKeyFactory.getInstance(hashProvider.getPbkdf2Algorithm())
                    .generateSecret(keySpec).getEncoded());
            String actualEncoded = hashProvider.encode(rawPassword, iterations, salt);
            assertEquals(expectedEncoded, actualEncoded);
            CredentialModel credentials = new CredentialModel();
            credentials.setAlgorithm(hashProvider.getAlgorithmAlias());
            credentials.setHashIterations(iterations);
            credentials.setSalt(salt);
            credentials.setValue(actualEncoded);
            assertTrue(hashProvider.verify(rawPassword, credentials));
        } catch (NoSuchAlgorithmException e) {
            fail("Algorithm " + hashProvider.getPbkdf2Algorithm() + " does not exist");
        } catch (InvalidKeySpecException e) {
            fail("Key spec invalid: " + e.getMessage());
        }
    }
}
