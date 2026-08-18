package org.keycloak.tests.client.policies;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.ClientProtocolCondition;
import org.keycloak.services.clientpolicy.condition.ClientProtocolConditionFactory;
import org.keycloak.services.clientpolicy.executor.RejectResourceOwnerPasswordCredentialsGrantExecutor;
import org.keycloak.services.clientpolicy.executor.RejectResourceOwnerPasswordCredentialsGrantExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.utils.ClientPoliciesUtil.createClientProtocolConditionConfig;
import static org.keycloak.tests.utils.ClientPoliciesUtil.createRejectisResourceOwnerPasswordCredentialsGrantExecutorConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for the client policies with CRUD of clients. For example test for scenarios when registration/update of client should fail because of client policies
 */
@KeycloakIntegrationTest
public class ClientPoliciesClientCRUDTest extends AbstractClientPoliciesTest {

    @InjectRealm
    protected ManagedRealm realm;

    @Test
    public void testClientProtocolConditionClientCRUD() throws Exception {
        // Register profile
        RejectResourceOwnerPasswordCredentialsGrantExecutor.Configuration rejectResourceOwnerRegistration = createRejectisResourceOwnerPasswordCredentialsGrantExecutorConfig(false);

        // register policies - client protocol condition
        ClientProtocolCondition.Configuration protocolConditionConfig = createClientProtocolConditionConfig(OIDCLoginProtocol.LOGIN_PROTOCOL);
        setupPolicy(realm, RejectResourceOwnerPasswordCredentialsGrantExecutorFactory.PROVIDER_ID, rejectResourceOwnerRegistration,
                ClientProtocolConditionFactory.PROVIDER_ID, protocolConditionConfig);

        // Register client including protocol. Should fail
        try {
            createClientByAdmin(realm, "example-client", OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep -> {
                clientRep.setDirectAccessGrantsEnabled(true);
            });
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals("Invalid client metadata: resource owner password credentials grant enabled", cpe.getErrorDetail());
        }

        // Register client without protocol. Should fail as protocol is OIDC by default
        try {
            createClientByAdmin(realm, "example-client", OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep -> {
                clientRep.setProtocol(null);
                clientRep.setDirectAccessGrantsEnabled(true);
            });
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals("Invalid client metadata: resource owner password credentials grant enabled", cpe.getErrorDetail());
        }

        // Create SAML client - should be successful
        String clientUUID = createClientByAdmin(realm, "example-client", SamlProtocol.LOGIN_PROTOCOL,clientRep -> {});

        // Try to update existing SAML client. Change protocol to OIDC and try to enable directAccessGrant. It should fail
        try {
            updateClientByAdmin(realm, clientUUID, (clientRep) -> {
                clientRep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
                clientRep.setDirectAccessGrantsEnabled(true);
            });
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals("Invalid client metadata: resource owner password credentials grant enabled", cpe.getErrorDetail());
        }

        // Lookup client and check protocol is still SAML
        ClientRepresentation foundRep = realm.admin().clients().get(clientUUID).toRepresentation();
        assertEquals(SamlProtocol.LOGIN_PROTOCOL, foundRep.getProtocol());

        // Try to create OIDC client without directGrants - should be success
        createClientByAdmin(realm, "example-client-oidc", OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep -> {});
    }
}
