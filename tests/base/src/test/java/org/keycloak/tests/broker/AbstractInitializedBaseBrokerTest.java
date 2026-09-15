package org.keycloak.tests.broker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import org.junit.jupiter.api.BeforeEach;

import static org.keycloak.broker.oidc.OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL;
import static org.keycloak.tests.broker.BrokerTestConstants.CLIENT_ID;
import static org.keycloak.tests.broker.BrokerTestConstants.IDP_SAML_ALIAS;
import static org.keycloak.tests.utils.admin.AdminApiUtil.createUserWithAdminClient;
import static org.keycloak.tests.utils.admin.AdminApiUtil.resetUserPassword;

public abstract class AbstractInitializedBaseBrokerTest extends AbstractBaseBrokerTest {

    protected IdentityProviderResource identityProviderResource;
    protected String userId;

    protected void postInitializeUser(UserRepresentation user) {
    }

    @Override
    @BeforeEach
    public void beforeBrokerTest() {
        super.beforeBrokerTest();
        log.debug("creating user for realm " + bc.providerRealmName());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(bc.getUserLogin());
        user.setEmail(bc.getUserEmail());
        if (IDP_SAML_ALIAS.equals(bc.getIDPAlias())) {
            user.setFirstName("Firstname");
            user.setLastName("Lastname");
        }
        user.setEmailVerified(true);
        user.setEnabled(true);
        postInitializeUser(user);

        RealmResource realmResource = adminClient.realm(bc.providerRealmName());
        userId = createUserWithAdminClient(realmResource, user);
        resetUserPassword(realmResource.users().get(userId), bc.getUserPassword(), false);

        log.debug("adding identity provider to realm " + bc.consumerRealmName());
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        Response response = realm.identityProviders().create(bc.setUpIdentityProvider());
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String error = null;
            try {
                error = response.readEntity(String.class);
            } catch (Exception ignored) {
            }
            throw new IllegalStateException("Failed to add identity provider " + bc.getIDPAlias() + " to realm "
                    + bc.consumerRealmName() + ": " + response.getStatus()
                    + (error != null && !error.isBlank() ? " - " + error : ""));
        }
        response.close();
        identityProviderResource = realm.identityProviders().get(bc.getIDPAlias());

        addClientsToProviderAndConsumer();
        configureBrokerEndpoints();
    }

    protected void configureBrokerEndpoints() {
        String providerBaseUrl = realmBaseUrl(bc.providerRealmName());
        IdentityProviderRepresentation idp = adminClient.realm(bc.consumerRealmName())
                .identityProviders().get(bc.getIDPAlias()).toRepresentation();
        Map<String, String> config = idp.getConfig();
        config.put(OIDCIdentityProviderConfig.ISSUER, providerBaseUrl);
        config.put("authorizationUrl", providerBaseUrl + "/protocol/openid-connect/auth");
        config.put(TOKEN_ENDPOINT_URL, providerBaseUrl + "/protocol/openid-connect/token");
        config.put("logoutUrl", providerBaseUrl + "/protocol/openid-connect/logout");
        config.put("userInfoUrl", providerBaseUrl + "/protocol/openid-connect/userinfo");
        config.put(OIDCIdentityProviderConfig.JWKS_URL, providerBaseUrl + "/protocol/openid-connect/certs");
        config.put(OIDCIdentityProviderConfig.USE_JWKS_URL, "true");
        config.put(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
        adminClient.realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()).update(idp);

        String consumerBaseUrl = realmBaseUrl(bc.consumerRealmName());
        String consumerBrokerEndpoint = consumerBaseUrl + "/broker/" + bc.getIDPAlias() + "/endpoint";
        ClientsResource providerClients = adminClient.realm(bc.providerRealmName()).clients();
        List<ClientRepresentation> clients = providerClients.findByClientId(CLIENT_ID);
        if (!clients.isEmpty()) {
            ClientRepresentation client = clients.get(0);
            client.setRedirectUris(List.of(consumerBrokerEndpoint + "/*"));
            client.setAdminUrl(consumerBrokerEndpoint);
            Map<String, String> attributes = client.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
                client.setAttributes(attributes);
            }
            attributes.put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL,
                    consumerBaseUrl + "/protocol/openid-connect/logout/backchannel-logout");
            providerClients.get(client.getId()).update(client);
        }
    }

    private String realmBaseUrl(String realmName) {
        return keycloakUrls.getBase() + "/realms/" + realmName;
    }

    protected void updateExecutions(BiConsumer<AuthenticationExecutionInfoRepresentation, AuthenticationManagementResource> action) {
        AuthenticationManagementResource flows = adminClient.realm(bc.consumerRealmName()).flows();
        for (AuthenticationExecutionInfoRepresentation execution : flows.getExecutions(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW)) {
            action.accept(execution, flows);
        }
    }
}
