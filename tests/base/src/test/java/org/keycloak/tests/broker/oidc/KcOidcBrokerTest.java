package org.keycloak.tests.broker.oidc;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcOidcBrokerTest extends AbstractKcOidcBrokerTest {

    @Test
    public void testTrustEmailWhenEmailVerifiedClaimIsMissingFromUserInfo() {
        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        idp.setTrustEmail(true);
        idp.getConfig().put("disableUserInfo", "false");
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);

        // Send the email only through UserInfo and omit email_verified.
        ClientScopeResource emailScope = AdminApiUtil.findClientScopeByName(providerRealm.admin(), "email");
        emailScope.getProtocolMappers().getMappers().forEach(mapper -> {
            if (IDToken.EMAIL_VERIFIED.equals(mapper.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME))) {
                emailScope.getProtocolMappers().delete(mapper.getId());
            } else if (IDToken.EMAIL.equals(mapper.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME))) {
                mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "false");
                emailScope.getProtocolMappers().update(mapper.getId(), mapper);
            }
        });
        ClientResource providerClient = providerRealm.admin().clients().get(
                providerRealm.admin().clients().findByClientId(CLIENT_ID).get(0).getId());
        providerClient.getProtocolMappers().getMappers().forEach(mapper -> {
            if (IDToken.EMAIL.equals(mapper.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME))) {
                mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "false");
                providerClient.getProtocolMappers().update(mapper.getId(), mapper);
            }
        });

        disableUpdateProfileOnFirstLogin();
        logInAsUserInIDP();
        assertTrue(oauth.parseLoginResponse().isSuccess());
        UserRepresentation consumerUser = AccountHelper.getUserRepresentation(consumerRealm.admin(), getUserLogin());
        assertEquals(getUserEmail(), consumerUser.getEmail());
        assertTrue(consumerUser.isEmailVerified());
    }

    @Test
    public void testUserInfoEmailWithoutEmailVerifiedPreservesTokenValue() {
        IdentityProviderRepresentation idpRep = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        idpRep.setTrustEmail(true);
        idpRep.getConfig().put("disableUserInfo", "false");
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idpRep);

        ClientScopeResource emailScope = AdminApiUtil.findClientScopeByName(providerRealm.admin(), "email");
        emailScope.getProtocolMappers().getMappers().forEach(mapper -> {
            if (IDToken.EMAIL_VERIFIED.equals(mapper.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME))) {
                emailScope.getProtocolMappers().delete(mapper.getId());
            }
        });

        ClientResource brokerAppResource = providerRealm.admin().clients().get(
                providerRealm.admin().clients().findByClientId(CLIENT_ID).get(0).getId());

        addIdTokenEmailVerifiedFalseMapper(brokerAppResource, "id-token-email-verified-false");

        disableUpdateProfileOnFirstLogin();
        logInAsUserInIDP();
        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation consumerUser = AccountHelper.getUserRepresentation(consumerRealm.admin(), getUserLogin());
        assertEquals(getUserEmail(), consumerUser.getEmail());
        assertFalse(consumerUser.isEmailVerified(),
                "email_verified=false from ID token should be preserved when userinfo provides email without email_verified");
    }

    @Test
    public void testForceSyncReLoginPreservesTokenEmailVerifiedFalse() {
        // configure the IdP: trustEmail=true, FORCE sync, UserInfo enabled
        IdentityProviderRepresentation idpRep = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        idpRep.setTrustEmail(true);
        idpRep.getConfig().put("disableUserInfo", "false");
        idpRep.getConfig().put(IdentityProviderModel.SYNC_MODE, IdentityProviderSyncMode.FORCE.toString());
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idpRep);

        // remove email_verified from the email scope so it is absent from UserInfo
        ClientScopeResource emailScope = AdminApiUtil.findClientScopeByName(providerRealm.admin(), "email");
        emailScope.getProtocolMappers().getMappers().forEach(mapper -> {
            if (IDToken.EMAIL_VERIFIED.equals(mapper.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME))) {
                emailScope.getProtocolMappers().delete(mapper.getId());
            }
        });

        // add email_verified=false to the ID token only
        ClientResource brokerAppResource = providerRealm.admin().clients().get(
                providerRealm.admin().clients().findByClientId(CLIENT_ID).get(0).getId());

        addIdTokenEmailVerifiedFalseMapper(brokerAppResource, "id-token-email-verified-false-force");

        // first login: user is created with emailVerified=false
        disableUpdateProfileOnFirstLogin();
        logInAsUserInIDP();
        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation consumerUser = AccountHelper.getUserRepresentation(consumerRealm.admin(), getUserLogin());
        assertEquals(getUserEmail(), consumerUser.getEmail());
        assertFalse(consumerUser.isEmailVerified(),
                "first login: email_verified=false from ID token must be applied");

        // admin marks the account as verified
        consumerUser.setEmailVerified(true);
        consumerRealm.admin().users().get(consumerUser.getId()).update(consumerUser);
        assertTrue(AccountHelper.getUserRepresentation(consumerRealm.admin(), getUserLogin()).isEmailVerified(),
                "pre-condition: admin must be able to set emailVerified=true");

        AccountHelper.logout(consumerRealm.admin(), getUserLogin());
        AccountHelper.logout(providerRealm.admin(), getUserLogin());

        logInAsUserInIDP();
        assertTrue(oauth.parseLoginResponse().isSuccess());

        // FORCE sync must re-apply the token's false, overriding the locally-set true
        UserRepresentation reloginUser = AccountHelper.getUserRepresentation(consumerRealm.admin(), getUserLogin());
        assertFalse(reloginUser.isEmailVerified(),
                "second login with FORCE sync: email_verified=false from ID token must override locally-set true");
    }

    private void addIdTokenEmailVerifiedFalseMapper(ClientResource client, String mapperName) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(mapperName);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setProtocolMapper(HardcodedClaim.PROVIDER_ID);
        mapper.getConfig().put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, IDToken.EMAIL_VERIFIED);
        mapper.getConfig().put(OIDCAttributeMapperHelper.JSON_TYPE, "boolean");
        mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "false");
        mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "false");
        mapper.getConfig().put(HardcodedClaim.CLAIM_VALUE, "false");

        Response response = client.getProtocolMappers().createMapper(mapper);
        // asserts the 201 status and closes the response
        ApiUtil.getCreatedId(response);
    }
}
