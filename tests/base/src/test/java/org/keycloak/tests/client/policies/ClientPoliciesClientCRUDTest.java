package org.keycloak.tests.client.policies;

import java.util.List;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeCondition;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientProtocolCondition;
import org.keycloak.services.clientpolicy.condition.ClientProtocolConditionFactory;
import org.keycloak.services.clientpolicy.executor.FullScopeDisabledExecutorFactory;
import org.keycloak.services.clientpolicy.executor.RejectImplicitGrantExecutorFactory;
import org.keycloak.services.clientpolicy.executor.RejectResourceOwnerPasswordCredentialsGrantExecutor;
import org.keycloak.services.clientpolicy.executor.RejectResourceOwnerPasswordCredentialsGrantExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.utils.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.tests.utils.ClientPoliciesUtil.createClientAccessTypeConditionConfig;
import static org.keycloak.tests.utils.ClientPoliciesUtil.createClientProtocolConditionConfig;
import static org.keycloak.tests.utils.ClientPoliciesUtil.createFullScopeDisabledExecutorConfig;
import static org.keycloak.tests.utils.ClientPoliciesUtil.createRejectImplicitGrantExecutorConfig;
import static org.keycloak.tests.utils.ClientPoliciesUtil.createRejectisResourceOwnerPasswordCredentialsGrantExecutorConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    
    @Test
    public void testClientAccessTypeConditionClientCRUD() throws Exception {
        // Setup policy: condition = confidential client access type, executor = reject resource owner password credentials grant
        RejectResourceOwnerPasswordCredentialsGrantExecutor.Configuration rejectResourceOwnerConfig =
                createRejectisResourceOwnerPasswordCredentialsGrantExecutorConfig(false);
        ClientAccessTypeCondition.Configuration accessTypeConditionConfig =
                createClientAccessTypeConditionConfig(List.of(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL));
        setupPolicy(realm, RejectResourceOwnerPasswordCredentialsGrantExecutorFactory.PROVIDER_ID, rejectResourceOwnerConfig,
                ClientAccessTypeConditionFactory.PROVIDER_ID, accessTypeConditionConfig);

        // Attempt to create a confidential client with directAccessGrants enabled. Should fail because condition matches.
        try {
            createClientByAdmin(realm, "confidential-client", OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep -> {
                clientRep.setPublicClient(Boolean.FALSE);
                clientRep.setBearerOnly(Boolean.FALSE);
                clientRep.setDirectAccessGrantsEnabled(true);
            });
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals("Invalid client metadata: resource owner password credentials grant enabled", cpe.getErrorDetail());
        }

        // Create a public client with directAccessGrants enabled. Should succeed because condition does not match (public != confidential).
        String publicClientUUID = createClientByAdmin(realm, "public-client", OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep -> {
            clientRep.setPublicClient(Boolean.TRUE);
            clientRep.setBearerOnly(Boolean.FALSE);
            clientRep.setDirectAccessGrantsEnabled(true);
        });

        // Verify client is public
        ClientRepresentation foundRep = realm.admin().clients().get(publicClientUUID).toRepresentation();
        assertEquals(Boolean.TRUE, foundRep.isPublicClient());

        // Attempt to update the public client to become confidential while keeping directAccessGrants enabled.
        // Should fail because updating TO confidential must also trigger the condition (this is the bug fix of #51380).
        try {
            updateClientByAdmin(realm, publicClientUUID, clientRep -> {
                clientRep.setPublicClient(Boolean.FALSE);
                clientRep.setBearerOnly(Boolean.FALSE);
                clientRep.setDirectAccessGrantsEnabled(true);
            });
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals("Invalid client metadata: resource owner password credentials grant enabled", cpe.getErrorDetail());
        }

        // Verify client is still public (update was rolled back)
        foundRep = realm.admin().clients().get(publicClientUUID).toRepresentation();
        assertEquals(Boolean.TRUE, foundRep.isPublicClient());

        // Update the public client to confidential WITHOUT directAccessGrants. Should succeed.
        updateClientByAdmin(realm, publicClientUUID, clientRep -> {
            clientRep.setPublicClient(Boolean.FALSE);
            clientRep.setBearerOnly(Boolean.FALSE);
            clientRep.setDirectAccessGrantsEnabled(false);
        });

        // Verify client is now confidential
        foundRep = realm.admin().clients().get(publicClientUUID).toRepresentation();
        assertFalse(foundRep.isPublicClient());
    }

    @Test
    public void testFullScopeDisabledExecutorClientCRUD() throws Exception {
        // Setup policy: any client condition + FullScopeDisabled executor (validate mode, not auto-configure)
        setupPolicy(realm, FullScopeDisabledExecutorFactory.PROVIDER_ID, createFullScopeDisabledExecutorConfig(Boolean.FALSE),
                AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig());

        // Register client without setting fullScopeAllowed (null → defaults to true). Should fail (bug #51382).
        try {
            createClientByAdmin(realm, "client-null-full-scope", OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep -> {
                clientRep.setFullScopeAllowed(null);
            });
            fail("Registration should have failed because fullScopeAllowed defaults to true");
        } catch (ClientPolicyException cpe) {
            assertEquals("Not permitted to enable fullScopeAllowed", cpe.getErrorDetail());
        }

        // Register client with fullScopeAllowed=true. Should also fail.
        try {
            createClientByAdmin(realm, "client-full-scope-true", OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep -> {
                clientRep.setFullScopeAllowed(Boolean.TRUE);
            });
            fail("Registration should have failed because fullScopeAllowed is explicitly true");
        } catch (ClientPolicyException cpe) {
            assertEquals("Not permitted to enable fullScopeAllowed", cpe.getErrorDetail());
        }

        // Register client with fullScopeAllowed=false. Should succeed.
        String clientUUID = createClientByAdmin(realm, "client-full-scope-false", OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep -> {
            clientRep.setFullScopeAllowed(Boolean.FALSE);
        });
        ClientRepresentation foundRep = realm.admin().clients().get(clientUUID).toRepresentation();
        assertFalse(foundRep.isFullScopeAllowed());

        // Update the client without touching fullScopeAllowed (null in the update rep). Should succeed
        // because the existing client already has fullScopeAllowed=false.
        updateClientByAdmin(realm, clientUUID, clientRep -> {
            clientRep.setFullScopeAllowed(null);
        });

        // Update the client and explicitly set fullScopeAllowed=true. Should fail.
        try {
            updateClientByAdmin(realm, clientUUID, clientRep -> {
                clientRep.setFullScopeAllowed(Boolean.TRUE);
            });
            fail("Update should have failed because fullScopeAllowed is being set to true");
        } catch (ClientPolicyException cpe) {
            assertEquals("Not permitted to enable fullScopeAllowed", cpe.getErrorDetail());
        }

        // Verify fullScopeAllowed is still false on the client (update was rolled back)
        foundRep = realm.admin().clients().get(clientUUID).toRepresentation();
        assertFalse(foundRep.isFullScopeAllowed());
    }

    @Test
    public void testRejectImplicitGrantExecutorClientCRUD() throws Exception {
        // Setup policy: any client condition + RejectImplicitGrant executor in validate-only mode (auto-configure=false)
        setupPolicy(realm, RejectImplicitGrantExecutorFactory.PROVIDER_ID, createRejectImplicitGrantExecutorConfig(Boolean.FALSE),
                AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig());

        // Register client without implicitFlowEnabled (null). Should succeed since null means implicit flow disabled
        // (issue #54153: used to fail with a NullPointerException surfaced as HTTP 500 unknown_error)
        String clientUUID = createClientByAdmin(realm, "client-null-implicit-flow", OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep -> {});
        ClientRepresentation foundRep = realm.admin().clients().get(clientUUID).toRepresentation();
        assertFalse(foundRep.isImplicitFlowEnabled());

        // Register client with implicitFlowEnabled=true. Should fail
        try {
            createClientByAdmin(realm, "client-implicit-flow-true", OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep -> {
                clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            });
            fail("Registration should have failed because implicit flow is enabled");
        } catch (ClientPolicyException cpe) {
            assertEquals("Invalid client metadata: implicit flow enabled", cpe.getErrorDetail());
        }

        // Register client with implicitFlowEnabled=false. Should succeed
        String clientUUIDFalse = createClientByAdmin(realm, "client-implicit-flow-false", OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep -> {
            clientRep.setImplicitFlowEnabled(Boolean.FALSE);
        });
        foundRep = realm.admin().clients().get(clientUUIDFalse).toRepresentation();
        assertFalse(foundRep.isImplicitFlowEnabled());

        // Update the client to enable implicit flow. Should fail
        try {
            updateClientByAdmin(realm, clientUUIDFalse, clientRep -> {
                clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            });
            fail("Update should have failed because implicit flow is being enabled");
        } catch (ClientPolicyException cpe) {
            assertEquals("Invalid client metadata: implicit flow enabled", cpe.getErrorDetail());
        }

        // Update the client sending an empty representation without implicitFlowEnabled. Should succeed
        updateClientByAdmin(realm, clientUUIDFalse, clientRep -> {
            clientRep.setImplicitFlowEnabled(null);
        });

        // Verify implicit flow is still disabled on the client
        foundRep = realm.admin().clients().get(clientUUIDFalse).toRepresentation();
        assertFalse(foundRep.isImplicitFlowEnabled());

        // auto-configure=true: the executor sets implicitFlowEnabled=false on the client representation
        setupPolicy(realm, RejectImplicitGrantExecutorFactory.PROVIDER_ID, createRejectImplicitGrantExecutorConfig(Boolean.TRUE),
                AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig());

        // Register client without implicitFlowEnabled. Should succeed and be auto-configured to implicit flow disabled
        String clientUUIDAuto = createClientByAdmin(realm, "client-auto-configured", OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep -> {});
        foundRep = realm.admin().clients().get(clientUUIDAuto).toRepresentation();
        assertFalse(foundRep.isImplicitFlowEnabled());
    }

    @Test
    public void testRejectImplicitGrantExecutorEmptyConfigurationClientCRUD() throws Exception {
        // Setup policy: any client condition + RejectImplicitGrant executor declared with an empty configuration
        // (no auto-configure option) - validate-only mode, must not fail with a NullPointerException
        realm.updateWithCleanup(r -> {
            r.resetClientProfiles()
                    .clientProfile(ClientProfileBuilder.create()
                    .name("executor")
                    .description("executor description")
                    .executor(RejectImplicitGrantExecutorFactory.PROVIDER_ID, null)
                    .build());
            r.resetClientPolicies()
                    .clientPolicy(ClientPolicyBuilder.create()
                    .name("policy")
                    .description("description of policy")
                    .condition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig())
                    .profile("executor")
                    .build());
            return r;
        });

        // Register client without implicitFlowEnabled (null). Should succeed even with the empty configuration
        createClientByAdmin(realm, "client-empty-config", OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep -> {});

        // Register client with implicitFlowEnabled=true. Still rejected - validate runs even without the auto-configure option
        try {
            createClientByAdmin(realm, "client-empty-config-implicit", OIDCLoginProtocol.LOGIN_PROTOCOL, clientRep -> {
                clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            });
            fail("Registration should have failed because implicit flow is enabled");
        } catch (ClientPolicyException cpe) {
            assertEquals("Invalid client metadata: implicit flow enabled", cpe.getErrorDetail());
        }
    }
}
