package org.keycloak.tests.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.clientregistration.policy.impl.TrustedHostClientRegistrationPolicyFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
public class DcrExternalTokenPrivilegeEscalationTest extends AbstractClientRegistrationTest {

    private static final String IDP_ALIAS = "allowed-idp";
    private static final String OTHER_IDP_ALIAS = "other-idp";
    private static final String FUTURE_EXTERNAL_TOKEN_ATTRIBUTE = "external.token.future";

    @Test
    public void externalTokenAttributesAreStrippedOnDcrCreateWithInitialAccessToken() throws ClientRegistrationException {
        reg.auth(Auth.token(createInitialAccessToken()));

        ClientRepresentation toCreate = new ClientRepresentation();
        toCreate.setAttributes(externalTokenAttributes(IDP_ALIAS));
        toCreate.getAttributes().put(FUTURE_EXTERNAL_TOKEN_ATTRIBUTE, "injected");
        toCreate.getAttributes().put("safe.attribute", "allowed");

        ClientRepresentation created = reg.create(toCreate);
        cleanupClient(created.getClientId());

        Map<String, String> stored = getStoredAttributes(created.getClientId());
        assertExternalTokenAccessNotGranted(stored);
        assertEquals("allowed", stored.get("safe.attribute"), "Non-privileged attributes must still be stored");
    }

    @Test
    public void externalTokenAttributesAreStrippedOnDcrUpdateWithRegistrationAccessToken() throws ClientRegistrationException {
        reg.auth(Auth.token(createInitialAccessToken()));
        ClientRepresentation created = reg.create(new ClientRepresentation());
        cleanupClient(created.getClientId());

        reg.auth(Auth.token(created.getRegistrationAccessToken()));
        ClientRepresentation update = new ClientRepresentation();
        update.setClientId(created.getClientId());
        update.setAttributes(externalTokenAttributes(IDP_ALIAS));
        update.getAttributes().put(FUTURE_EXTERNAL_TOKEN_ATTRIBUTE, "injected");
        update.getAttributes().put("safe.attribute", "allowed");

        reg.update(update);

        Map<String, String> stored = getStoredAttributes(created.getClientId());
        assertExternalTokenAccessNotGranted(stored);
        assertEquals("allowed", stored.get("safe.attribute"), "Non-privileged attributes must still be stored");
    }

    @Test
    public void externalTokenAttributesAreStrippedOnAnonymousDcrCreate() throws ClientRegistrationException {
        allowAnonymousRegistrationFromLocalhost();
        reg.auth(null);

        ClientRepresentation toCreate = new ClientRepresentation();
        toCreate.setClientId("anonymous-client");
        toCreate.setRedirectUris(Collections.singletonList("http://localhost:8080/callback"));
        toCreate.setAttributes(externalTokenAttributes(IDP_ALIAS));

        ClientRepresentation created = reg.create(toCreate);
        cleanupClient(created.getClientId());

        assertExternalTokenAccessNotGranted(getStoredAttributes(created.getClientId()));
    }

    @Test
    public void oidcRegisteredClientCannotSelfGrantExternalTokenAccessThroughDefaultEndpoint() throws ClientRegistrationException {
        reg.auth(Auth.token(createInitialAccessToken()));

        OIDCClientRepresentation oidcClient = new OIDCClientRepresentation();
        oidcClient.setClientName("oidc-registered-client");
        oidcClient.setRedirectUris(Collections.singletonList("http://localhost:8080/callback"));
        OIDCClientRepresentation created = reg.oidc().create(oidcClient);
        cleanupClient(created.getClientId());

        reg.auth(Auth.token(created.getRegistrationAccessToken()));
        ClientRepresentation update = new ClientRepresentation();
        update.setClientId(created.getClientId());
        update.setAttributes(externalTokenAttributes(IDP_ALIAS));
        reg.update(update);

        assertExternalTokenAccessNotGranted(getStoredAttributes(created.getClientId()));
    }

    @Test
    public void registrationAccessTokenCannotChangeExternalTokenAttributesGrantedByAdmin() throws ClientRegistrationException {
        String clientUuid = createClientAsAdmin("admin-allowed-client", IDP_ALIAS);
        String registrationAccessToken = regenerateRegistrationAccessToken(clientUuid);

        // the client tries to extend the grant to another identity provider
        reg.auth(Auth.token(registrationAccessToken));
        ClientRepresentation update = new ClientRepresentation();
        update.setClientId("admin-allowed-client");
        update.setAttributes(externalTokenAttributes(OTHER_IDP_ALIAS));
        ClientRepresentation updated = reg.update(update);
        assertExternalTokenAccessGranted(getStoredAttributes("admin-allowed-client"), IDP_ALIAS);

        // the client tries to extend the grant by sending a multivalued list of identity providers
        reg.auth(Auth.token(updated.getRegistrationAccessToken()));
        update.setAttributes(externalTokenAttributes(IDP_ALIAS + "##" + OTHER_IDP_ALIAS));
        updated = reg.update(update);
        assertExternalTokenAccessGranted(getStoredAttributes("admin-allowed-client"), IDP_ALIAS);

        // the attributes are owned by the administrator, the client cannot remove them either
        reg.auth(Auth.token(updated.getRegistrationAccessToken()));
        update.setAttributes(new HashMap<>());
        update.getAttributes().put(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.FALSE.toString());
        update.getAttributes().put(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, "");
        reg.update(update);
        assertExternalTokenAccessGranted(getStoredAttributes("admin-allowed-client"), IDP_ALIAS);
    }

    @Test
    public void externalTokenAttributesGrantedByAdminSurviveRegistrationAccessTokenRoundTrip() throws ClientRegistrationException {
        String clientUuid = createClientAsAdmin("round-trip-client", IDP_ALIAS);
        String registrationAccessToken = regenerateRegistrationAccessToken(clientUuid);

        reg.auth(Auth.token(registrationAccessToken));
        ClientRepresentation rep = reg.get("round-trip-client");
        assertExternalTokenAccessGranted(rep.getAttributes(), IDP_ALIAS);

        rep.setDescription("updated by the client");
        reg.update(rep);

        ClientRepresentation stored = managedRealm.admin().clients().get(clientUuid).toRepresentation();
        assertEquals("updated by the client", stored.getDescription());
        assertExternalTokenAccessGranted(stored.getAttributes(), IDP_ALIAS);
    }

    @Test
    public void externalTokenAttributesCanBeSetThroughDcrWithAdminBearerToken() throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation toCreate = buildClient();
        toCreate.setAttributes(externalTokenAttributes(IDP_ALIAS));
        ClientRepresentation created = reg.create(toCreate);
        cleanupClient(created.getClientId());
        assertExternalTokenAccessGranted(getStoredAttributes(created.getClientId()), IDP_ALIAS);

        ClientRepresentation update = new ClientRepresentation();
        update.setClientId(created.getClientId());
        update.setAttributes(externalTokenAttributes(OTHER_IDP_ALIAS));
        reg.update(update);
        assertExternalTokenAccessGranted(getStoredAttributes(created.getClientId()), OTHER_IDP_ALIAS);

        authCreateClients();
        ClientRepresentation toCreateWithCreateRole = new ClientRepresentation();
        toCreateWithCreateRole.setClientId("created-with-create-client-role");
        toCreateWithCreateRole.setAttributes(externalTokenAttributes(IDP_ALIAS));
        ClientRepresentation createdWithCreateRole = reg.create(toCreateWithCreateRole);
        cleanupClient(createdWithCreateRole.getClientId());
        assertExternalTokenAccessGranted(getStoredAttributes(createdWithCreateRole.getClientId()), IDP_ALIAS);
    }

    private String createInitialAccessToken() {
        return managedRealm.admin().clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 1)).getToken();
    }

    private String createClientAsAdmin(String clientId, String idpAlias) {
        ClientRepresentation rep = ClientBuilder.create(clientId)
                .secret("secret")
                .attributes(externalTokenAttributes(idpAlias))
                .build();
        try (Response response = managedRealm.admin().clients().create(rep)) {
            assertEquals(201, response.getStatus());
            String uuid = ApiUtil.getCreatedId(response);
            managedRealm.cleanup().add(realm -> realm.clients().get(uuid).remove());
            return uuid;
        }
    }

    private String regenerateRegistrationAccessToken(String clientUuid) {
        return managedRealm.admin().clients().get(clientUuid).regenerateRegistrationAccessToken().getRegistrationAccessToken();
    }

    private void allowAnonymousRegistrationFromLocalhost() {
        String anonymousPolicyType = ClientRegistrationPolicyManager.getComponentTypeKey(RegistrationAuth.ANONYMOUS);
        List<ComponentRepresentation> policies = managedRealm.admin().components()
                .query(managedRealm.getId(), ClientRegistrationPolicy.class.getName());
        ComponentRepresentation trustedHostsPolicy = policies.stream()
                .filter(p -> anonymousPolicyType.equals(p.getSubType()))
                .filter(p -> TrustedHostClientRegistrationPolicyFactory.PROVIDER_ID.equals(p.getProviderId()))
                .findFirst()
                .orElseThrow();
        managedRealm.updateComponent(trustedHostsPolicy.getId(), policy -> {
            policy.getConfig().put(TrustedHostClientRegistrationPolicyFactory.TRUSTED_HOSTS, Arrays.asList("localhost", "127.0.0.1"));
            policy.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH, "true");
            policy.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.CLIENT_URIS_MUST_MATCH, "false");
        });
    }

    private Map<String, String> getStoredAttributes(String clientId) {
        return managedRealm.admin().clients().findByClientId(clientId).get(0).getAttributes();
    }

    private void cleanupClient(String clientId) {
        managedRealm.cleanup().add(realm -> {
            List<ClientRepresentation> found = realm.clients().findByClientId(clientId);
            if (!found.isEmpty()) {
                realm.clients().get(found.get(0).getId()).remove();
            }
        });
    }

    private static Map<String, String> externalTokenAttributes(String idpAlias) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString());
        attributes.put(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, idpAlias);
        return attributes;
    }

    private static void assertExternalTokenAccessNotGranted(Map<String, String> attributes) {
        assertNull(attributes.get(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED),
                OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED + " must not be set through dynamic client registration");
        assertNull(attributes.get(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP),
                OIDCConfigAttributes.EXTERNAL_TOKEN_IDP + " must not be set through dynamic client registration");
        assertNull(attributes.get(FUTURE_EXTERNAL_TOKEN_ATTRIBUTE),
                FUTURE_EXTERNAL_TOKEN_ATTRIBUTE + " must not be set through dynamic client registration");
    }

    private static void assertExternalTokenAccessGranted(Map<String, String> attributes, String expectedIdpAlias) {
        assertEquals(Boolean.TRUE.toString(), attributes.get(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED));
        assertEquals(expectedIdpAlias, attributes.get(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP));
    }
}
