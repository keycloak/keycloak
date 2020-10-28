package org.keycloak.utils;

import org.junit.Test;
import org.keycloak.crypto.CibaLoginHintEncryptor;

import java.security.GeneralSecurityException;

import static org.junit.Assert.assertEquals;

public class CibaLoginHintEncryptorTests {

    @Test
    public void encrypt_decrypt_test() throws GeneralSecurityException {
        String login = "admin";
        String secret = "201ee451-32f6-4fb1-82ee-a2f31f31ea70";
        String encrypted = CibaLoginHintEncryptor.encodeLoginHint(secret, login);
        System.out.println(encrypted);
        String loginDecrypted = CibaLoginHintEncryptor.decodeLoginHint(secret, encrypted);
        assertEquals(login,loginDecrypted);
    }
}
