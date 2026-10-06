package org.keycloak.tests.broker.oidc;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
