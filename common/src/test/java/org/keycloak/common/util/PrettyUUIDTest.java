package org.keycloak.common.util;

import org.junit.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

import static org.junit.Assert.*;

public class PrettyUUIDTest {

    @Test
    public void unsigned() {
        UUID uuid = UUID.randomUUID();
        String encoded = PrettyUUID.encode(uuid);
        assertTrue(encoded.length() <= 22);

        PrettyUUID decoded = PrettyUUID.decode(encoded);
        assertEquals(uuid, decoded.getUuid());
        assertFalse(decoded.isSigned());
    }

    @Test
    public void signed() throws InvalidKeyException, NoSuchAlgorithmException {
        UUID uuid = UUID.randomUUID();
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        byte[] signature = sign(secret, uuid.toString(), "something else");

        String encoded = PrettyUUID.encode(uuid, signature);
        assertTrue(encoded.length() >= 60);

        PrettyUUID decoded = PrettyUUID.decode(encoded);
        assertEquals(uuid, decoded.getUuid());
        assertTrue(decoded.isSigned());
        assertTrue(verify(secret, decoded.getSignature(), decoded.getUuid().toString(), "something else"));
    }

    private static byte[] sign(byte[] secret, String... parts) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HMACSHA256");
        mac.init(new SecretKeySpec(secret, mac.getAlgorithm()));
        for (String s : parts) {
            mac.update(s.getBytes(StandardCharsets.UTF_8));
        }

        byte[] signature = mac.doFinal();
        return signature;
    }

    private static boolean verify(byte[] secret, byte[] signature, String... parts) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HMACSHA256");
        mac.init(new SecretKeySpec(secret, mac.getAlgorithm()));
        for (String s : parts) {
            mac.update(s.getBytes(StandardCharsets.UTF_8));
        }
        return MessageDigest.isEqual(signature, mac.doFinal());
    }

}
