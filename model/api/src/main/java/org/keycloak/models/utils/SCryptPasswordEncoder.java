package org.keycloak.models.utils;

import org.bouncycastle.crypto.generators.SCrypt;
import org.keycloak.models.UserCredentialModel;

import java.io.UnsupportedEncodingException;

/**
 * User: jpkroehling
 * Date: 12/5/13
 * Time: 2:25 PM
 */
public class SCryptPasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(UserCredentialModel credentialModel) {

        try {
            byte[] passwordBytes = credentialModel.getValue().getBytes("UTF-8");
            byte[] saltBytes = credentialModel.getSalt().getBytes("UTF-8");
            byte[] digest = SCrypt.generate(passwordBytes, saltBytes, 16384, 8, 1, 218);

            return Base64.encodeBytes(digest);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Credential could not be encoded", e);
        }
    }

    @Override
    public boolean verify(UserCredentialModel real, UserCredentialModel provided) {
        return encode(provided).equals(real.getValue());
    }
}
