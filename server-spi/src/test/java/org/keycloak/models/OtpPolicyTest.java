package org.keycloak.models;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.HmacOTP;

import java.net.URI;

public class OtpPolicyTest {

    OTPPolicy totpPolicy;

    @Before
    public void setup() {
        totpPolicy = new OTPPolicy();
        totpPolicy.setAlgorithm(HmacOTP.HMAC_SHA1);
        totpPolicy.setDigits(6);
        totpPolicy.setType(OTPCredentialModel.TOTP);
    }

    @Test
    public void keyUriShouldBeValidForRealmDisplayNameWithColon() {

        String keyURI = totpPolicy.getKeyURI("Test:Realm", "tester", "secret");
        Assert.assertEquals("Test Realm", getLabelComponent(keyURI));
    }

    @Test
    public void keyUriShouldBeValidForRealmDisplayNameWithSlash() {

        String keyURI = totpPolicy.getKeyURI("Test/Realm", "tester", "secret");
        Assert.assertEquals("Test/Realm", getLabelComponent(keyURI));
    }

    static String getLabelComponent(String keyURI) {
        return URI.create(keyURI).getPath().substring(1).split(":")[0];
    }
}
