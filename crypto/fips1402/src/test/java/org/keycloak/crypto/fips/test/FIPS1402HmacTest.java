package org.keycloak.crypto.fips.test;

import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.common.util.Environment;
import org.keycloak.jose.HmacTest;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.HMACProvider;


/**
 * Another variation to this test using SecretKeyFactory
 *
 */
public class FIPS1402HmacTest extends HmacTest {

    @Before
    public void before() {
        // Run this test just if java is in FIPS mode
        Assume.assumeTrue("Java is not in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }

    @Test
    public void testHmacSignaturesFIPS() throws Exception {
        //

        SecretKeyFactory skFact = SecretKeyFactory.getInstance("HmacSHA256", BouncyIntegration.PROVIDER );
        SecretKey secret = skFact.generateSecret(new SecretKeySpec(UUID.randomUUID().toString().getBytes(), "HmacSHA256"));
        String encoded = new JWSBuilder().content("12345678901234567890".getBytes())
                .hmac256(secret);
        System.out.println("length: " + encoded.length());
        JWSInput input = new JWSInput(encoded);
        Assert.assertTrue(HMACProvider.verify(input, secret));
    }
}
