/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.testsuite.migrated.admin;

import java.util.List;
import javax.ws.rs.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class RealmTest extends AbstractClientTest {

    public static final String PRIVATE_KEY = "MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=";
    public static final String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";
    public static final String CERTIFICATE = "MIICsTCCAZkCBgFTLB5bhDANBgkqhkiG9w0BAQsFADAcMRowGAYDVQQDDBFhZG1pbi1jbGllbnQtdGVzdDAeFw0xNjAyMjkwODIwMDBaFw0yNjAyMjgwODIxNDBaMBwxGjAYBgNVBAMMEWFkbWluLWNsaWVudC10ZXN0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAquzJtpAlpTFnJzILjTOHW+SOWav1eIsCtlAqiFTvBskbod6b4BtVaR3FVrQm8rFiwDOIEWT3IG3ZIz0LKYxnqvuffyLHGHjiroqrR63kY9Wa9B790lSEWVaGeNOMnKleqKu5QUNfL3wVebUh/C/QfxZ29R1EIbxNe2ThN8yuIca8Ltn43D5VlyatptojffxpCYiYqAmIwQDaq1um2cQ+4rPBLxC5jM9UBvYOMUP4u0caNSaPI1o9lHVKgTtWcdQzUeMmAGsnLV26XGhA/OwRduUxksumR1kh/KSqowasjgSrpVqtF/uo5TY57s7drD+zKG58cdHLreclB9AQNvNwZwIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBh4iwg8GnadeQP52pV5vKJ4Z8A1R2aYCzoW7Lc3FI/pXWX9Af5dKILX5O2j/daamPS+WtDWxIuwvZC5drrkvJn/r8e4KstnXQzPQggIJbI9v3wfIX3VlFvwvZVGiuE5PSLSWb0L57PEojZVpIU5bLchq4yRSD2zK4dWX8Y6I/D40a74KDvPOlEL8405/T1iW7ytKT9awNJW04N91owoI+kdUL+DMnnGzIxDAoYAeZI/1vcwoaH24zyTLGItkzpKxqLOdB05cnxn5jCWY2Hyd1zqtRkadhgZaqu4lcDHAHEMDp6dEjLZW8ym8bnlto+MD2y//CsyPCzyCLlA726vrli";

    @Test
    public void getRealms() {
        List<RealmRepresentation> realms = keycloak.realms().findAll();
        assertNames(realms, "master", "test", REALM_NAME);

        for (RealmRepresentation rep : realms) {
            assertNull(rep.getPrivateKey());
            assertNull(rep.getCodeSecret());
            assertNotNull(rep.getPublicKey());
            assertNotNull(rep.getCertificate());
        }
    }

    @Test
    public void createRealmEmpty() {
        RealmRepresentation rep = new RealmRepresentation();
        try {
            rep.setRealm("new-realm");
            keycloak.realms().create(rep);
            assertNames(keycloak.realms().findAll(), "master", "test", REALM_NAME, "new-realm");
        } finally {
            removeRealm(rep);
        }
    }

    @Test
    public void createRealm() {
        RealmRepresentation rep = loadJson(getClass().getResourceAsStream("/admin-test/testrealm.json"), RealmRepresentation.class);
        try {
            keycloak.realms().create(rep);

            RealmRepresentation created = keycloak.realms().realm("admin-test-1").toRepresentation();
            assertRealm(rep, created);
        } finally {
            removeRealm(rep);
        }
    }

    @Test
    public void uploadRealmKeys() throws Exception {
        String originalPublicKey = realm.toRepresentation().getPublicKey();

        RealmRepresentation rep = new RealmRepresentation();
        rep.setPrivateKey("INVALID");
        rep.setPublicKey(PUBLIC_KEY);

        try {
            realm.update(rep);
            fail("Expected BadRequestException");
        } catch (BadRequestException e) {
        }

        rep.setPrivateKey(PRIVATE_KEY);
        rep.setPublicKey("INVALID");

        try {
            realm.update(rep);
            fail("Expected BadRequestException");
        } catch (BadRequestException e) {
        }

        assertEquals(originalPublicKey, realm.toRepresentation().getPublicKey());

        rep.setPublicKey(PUBLIC_KEY);
        realm.update(rep);

        assertEquals(PUBLIC_KEY, rep.getPublicKey());

        String privateKey2048 = IOUtils.toString(getClass().getResourceAsStream("/keys/private2048.pem"));
        String publicKey2048 = IOUtils.toString(getClass().getResourceAsStream("/keys/public2048.pem"));

        rep.setPrivateKey(privateKey2048);

        try {
            realm.update(rep);
            fail("Expected BadRequestException");
        } catch (BadRequestException e) {
        }

        assertEquals(PUBLIC_KEY, realm.toRepresentation().getPublicKey());

        rep.setPublicKey(publicKey2048);

        realm.update(rep);

        assertEquals(publicKey2048, realm.toRepresentation().getPublicKey());

        String privateKey4096 = IOUtils.toString(getClass().getResourceAsStream("/keys/private4096.pem"));
        String publicKey4096 = IOUtils.toString(getClass().getResourceAsStream("/keys/public4096.pem"));
        rep.setPrivateKey(privateKey4096);
        rep.setPublicKey(publicKey4096);

        realm.update(rep);

        assertEquals(publicKey4096, realm.toRepresentation().getPublicKey());
    }

    public static void assertRealm(RealmRepresentation realm, RealmRepresentation storedRealm) {
        if (realm.getId() != null) {
            Assert.assertEquals(realm.getId(), storedRealm.getId());
        }
        if (realm.getRealm() != null) {
            Assert.assertEquals(realm.getRealm(), storedRealm.getRealm());
        }
        if (realm.isEnabled() != null) Assert.assertEquals(realm.isEnabled(), storedRealm.isEnabled());
        if (realm.isBruteForceProtected() != null) Assert.assertEquals(realm.isBruteForceProtected(), storedRealm.isBruteForceProtected());
        if (realm.getMaxFailureWaitSeconds() != null) Assert.assertEquals(realm.getMaxFailureWaitSeconds(), storedRealm.getMaxFailureWaitSeconds());
        if (realm.getMinimumQuickLoginWaitSeconds() != null) Assert.assertEquals(realm.getMinimumQuickLoginWaitSeconds(), storedRealm.getMinimumQuickLoginWaitSeconds());
        if (realm.getWaitIncrementSeconds() != null) Assert.assertEquals(realm.getWaitIncrementSeconds(), storedRealm.getWaitIncrementSeconds());
        if (realm.getQuickLoginCheckMilliSeconds() != null) Assert.assertEquals(realm.getQuickLoginCheckMilliSeconds(), storedRealm.getQuickLoginCheckMilliSeconds());
        if (realm.getMaxDeltaTimeSeconds() != null) Assert.assertEquals(realm.getMaxDeltaTimeSeconds(), storedRealm.getMaxDeltaTimeSeconds());
        if (realm.getFailureFactor() != null) Assert.assertEquals(realm.getFailureFactor(), storedRealm.getFailureFactor());
        if (realm.isRegistrationAllowed() != null) Assert.assertEquals(realm.isRegistrationAllowed(), storedRealm.isRegistrationAllowed());
        if (realm.isRegistrationEmailAsUsername() != null) Assert.assertEquals(realm.isRegistrationEmailAsUsername(), storedRealm.isRegistrationEmailAsUsername());
        if (realm.isRememberMe() != null) Assert.assertEquals(realm.isRememberMe(), storedRealm.isRememberMe());
        if (realm.isVerifyEmail() != null) Assert.assertEquals(realm.isVerifyEmail(), storedRealm.isVerifyEmail());
        if (realm.isResetPasswordAllowed() != null) Assert.assertEquals(realm.isResetPasswordAllowed(), storedRealm.isResetPasswordAllowed());
        if (realm.isEditUsernameAllowed() != null) Assert.assertEquals(realm.isEditUsernameAllowed(), storedRealm.isEditUsernameAllowed());
        if (realm.getSslRequired() != null) Assert.assertEquals(realm.getSslRequired(), storedRealm.getSslRequired());
        if (realm.getAccessCodeLifespan() != null) Assert.assertEquals(realm.getAccessCodeLifespan(), storedRealm.getAccessCodeLifespan());
        if (realm.getAccessCodeLifespanUserAction() != null)
            Assert.assertEquals(realm.getAccessCodeLifespanUserAction(), storedRealm.getAccessCodeLifespanUserAction());
        if (realm.getNotBefore() != null) Assert.assertEquals(realm.getNotBefore(), storedRealm.getNotBefore());
        if (realm.getAccessTokenLifespan() != null) Assert.assertEquals(realm.getAccessTokenLifespan(), storedRealm.getAccessTokenLifespan());
        if (realm.getAccessTokenLifespanForImplicitFlow() != null) Assert.assertEquals(realm.getAccessTokenLifespanForImplicitFlow(), storedRealm.getAccessTokenLifespanForImplicitFlow());
        if (realm.getSsoSessionIdleTimeout() != null) Assert.assertEquals(realm.getSsoSessionIdleTimeout(), storedRealm.getSsoSessionIdleTimeout());
        if (realm.getSsoSessionMaxLifespan() != null) Assert.assertEquals(realm.getSsoSessionMaxLifespan(), storedRealm.getSsoSessionMaxLifespan());
        if (realm.getRequiredCredentials() != null) {
            Assert.assertNotNull(storedRealm.getRequiredCredentials());
            for (String cred : realm.getRequiredCredentials()) {
                Assert.assertTrue(storedRealm.getRequiredCredentials().contains(cred));
            }
        }
        if (realm.getLoginTheme() != null) Assert.assertEquals(realm.getLoginTheme(), storedRealm.getLoginTheme());
        if (realm.getAccountTheme() != null) Assert.assertEquals(realm.getAccountTheme(), storedRealm.getAccountTheme());
        if (realm.getAdminTheme() != null) Assert.assertEquals(realm.getAdminTheme(), storedRealm.getAdminTheme());
        if (realm.getEmailTheme() != null) Assert.assertEquals(realm.getEmailTheme(), storedRealm.getEmailTheme());

        if (realm.getPasswordPolicy() != null) Assert.assertEquals(realm.getPasswordPolicy(), storedRealm.getPasswordPolicy());

        if (realm.getDefaultRoles() != null) {
            Assert.assertNotNull(storedRealm.getDefaultRoles());
            for (String role : realm.getDefaultRoles()) {
                Assert.assertTrue(storedRealm.getDefaultRoles().contains(role));
            }
        }

        if (realm.getSmtpServer() != null) {
            Assert.assertEquals(realm.getSmtpServer(), storedRealm.getSmtpServer());
        }

        if (realm.getBrowserSecurityHeaders() != null) {
            Assert.assertEquals(realm.getBrowserSecurityHeaders(), storedRealm.getBrowserSecurityHeaders());
        }

    }
}
