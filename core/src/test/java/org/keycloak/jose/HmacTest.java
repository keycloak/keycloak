package org.keycloak.jose;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.HMACProvider;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.UUID;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HmacTest {

    @Test
    public void testHmacSignatures() throws Exception {
        SecretKey secret = new SecretKeySpec(UUID.randomUUID().toString().getBytes(), "HmacSHA256");
        String encoded = new JWSBuilder().content("12345678901234567890".getBytes())
                .hmac256(secret);
        System.out.println("length: " + encoded.length());
        JWSInput input = new JWSInput(encoded);
        Assert.assertTrue(HMACProvider.verify(input, secret));
    }


}
