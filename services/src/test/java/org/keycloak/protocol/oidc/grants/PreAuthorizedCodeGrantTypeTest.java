package org.keycloak.protocol.oidc.grants;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.protocol.oid4vc.issuance.OffsetTimeProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.utils.OID4VCUtil;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PreAuthorizedCodeGrantTypeTest {

    @Test
    public void shouldAcceptOfferWithinConfiguredLifespan() {
        CredentialOfferState offerState = createOfferState(300);

        assertTrue(PreAuthorizedCodeGrantType.isCredentialOfferLifespanValid(offerState, 300));
    }

    @Test
    public void shouldRejectOfferCreatedWithExcessiveLifespan() {
        CredentialOfferState offerState = createOfferState(3600);

        assertFalse(PreAuthorizedCodeGrantType.isCredentialOfferLifespanValid(offerState, 300));
    }

    @Test
    public void shouldRejectLegacyOfferWithoutCreationTime() throws Exception {
        CredentialOfferState legacyOfferState = JsonSerialization.readValue(
                "{\"expiresAt\":2147483647}", CredentialOfferState.class);

        assertFalse(PreAuthorizedCodeGrantType.isCredentialOfferLifespanValid(legacyOfferState, 300));
    }

    @Test
    public void shouldAcceptOfferCreatedBeyondIntegerTimeRange() {
        int originalOffset = Time.getOffset();
        try {
            Time.setOffset(Integer.MAX_VALUE);
            assertTrue(new OffsetTimeProvider().currentTimeSeconds() > Integer.MAX_VALUE);

            CredentialOfferState offerState = createOfferState(300);

            assertTrue(offerState.getCreatedAt() > Integer.MAX_VALUE);
            assertFalse(offerState.isExpired());
            assertTrue(PreAuthorizedCodeGrantType.isCredentialOfferLifespanValid(offerState, 300));
        } finally {
            Time.setOffset(originalOffset);
        }
    }

    @Test
    public void shouldResolveFederatedPasswordTimestamp() {
        CredentialModel federatedPassword = credential(PasswordCredentialModel.TYPE, 1234L);
        federatedPassword.setFederationLink("ldap-provider");

        assertEquals(1234L, OID4VCUtil.getPasswordCredentialTimestamp(userWithCredentials(federatedPassword)));
    }

    @Test
    public void shouldIgnoreNonPasswordCredentialsBeforeFederatedPassword() {
        CredentialModel localOtp = credential("otp", 1000L);
        CredentialModel federatedPassword = credential(PasswordCredentialModel.TYPE, 2000L);
        federatedPassword.setFederationLink("ldap-provider");

        assertEquals(2000L,
                OID4VCUtil.getPasswordCredentialTimestamp(userWithCredentials(localOtp, federatedPassword)));
    }

    @Test
    public void shouldUseNewestLocalOrFederatedPasswordTimestamp() {
        CredentialModel localPassword = credential(PasswordCredentialModel.TYPE, 2000L);
        CredentialModel federatedPassword = credential(PasswordCredentialModel.TYPE, 1000L);
        federatedPassword.setFederationLink("ldap-provider");
        UserModel user = userWithCredentials(localPassword, federatedPassword);

        assertEquals(2000L, OID4VCUtil.getPasswordCredentialTimestamp(user));

        federatedPassword.setCreatedDate(3000L);
        assertEquals(3000L, OID4VCUtil.getPasswordCredentialTimestamp(user));
    }

    @Test
    public void shouldPreserveSentinelsForMissingPasswordMetadata() {
        CredentialModel passwordWithoutTimestamp = credential(PasswordCredentialModel.TYPE, null);

        assertEquals(0L, OID4VCUtil.getPasswordCredentialTimestamp(userWithCredentials(passwordWithoutTimestamp)));
        assertEquals(-1L, OID4VCUtil.getPasswordCredentialTimestamp(userWithCredentials()));
    }

    private CredentialOfferState createOfferState(int lifespan) {
        return new CredentialOfferState(new CredentialsOffer(), null, null, Time.currentTimeSeconds() + lifespan, null);
    }

    private CredentialModel credential(String type, Long createdDate) {
        CredentialModel credential = new CredentialModel();
        credential.setType(type);
        credential.setCreatedDate(createdDate);
        return credential;
    }

    private UserModel userWithCredentials(CredentialModel... credentials) {
        SubjectCredentialManager credentialManager = (SubjectCredentialManager) Proxy.newProxyInstance(
                SubjectCredentialManager.class.getClassLoader(),
                new Class<?>[]{SubjectCredentialManager.class},
                (proxy, method, args) -> {
                    if ("getCredentials".equals(method.getName())) {
                        return Arrays.stream(credentials);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        return (UserModel) Proxy.newProxyInstance(
                UserModel.class.getClassLoader(),
                new Class<?>[]{UserModel.class},
                (proxy, method, args) -> {
                    if ("credentialManager".equals(method.getName())) {
                        return credentialManager;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
