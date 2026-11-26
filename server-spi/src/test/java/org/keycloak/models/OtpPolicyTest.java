package org.keycloak.models;

import java.net.URI;

import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.HmacOTP;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

    @Test
    public void keyUriShouldPercentEncodeSpaceInAccountName() {

        String keyURI = totpPolicy.getKeyURI("Realm", "john doe", "secret");

        // The account-name label segment is a URI path component, where '+' is a literal
        // plus, not a space. URLEncoder emits '+' for a space, so it must be rewritten to
        // %20 (as the issuer segment already is); otherwise authenticator apps show "john+doe".
        Assert.assertEquals("john doe", getAccountComponent(keyURI));
        Assert.assertFalse("account name must be %20-encoded, not '+'", keyURI.contains("john+doe"));
    }

    static String getLabelComponent(String keyURI) {
        return URI.create(keyURI).getPath().substring(1).split(":")[0];
    }

    static String getAccountComponent(String keyURI) {
        return URI.create(keyURI).getPath().substring(1).split(":", 2)[1];
    }
}
