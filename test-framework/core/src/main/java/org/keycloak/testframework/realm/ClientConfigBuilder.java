package org.keycloak.testframework.realm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

public class ClientConfigBuilder {

    private final ClientRepresentation rep;

    private ClientConfigBuilder(ClientRepresentation rep) {
        this.rep = rep;
    }

    public static ClientConfigBuilder create() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setEnabled(true);
        return new ClientConfigBuilder(rep);
    }

    public static ClientConfigBuilder update(ClientRepresentation rep) {
        return new ClientConfigBuilder(rep);
    }

    public ClientConfigBuilder enabled(boolean enabled) {
        rep.setEnabled(enabled);
        return this;
    }

    public ClientConfigBuilder clientId(String clientId) {
        rep.setClientId(clientId);
        return this;
    }

    public ClientConfigBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public ClientConfigBuilder secret(String secret) {
        rep.setSecret(secret);
        return this;
    }

    public ClientConfigBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public ClientConfigBuilder description(String description) {
        rep.setDescription(description);
        return this;
    }

    public ClientConfigBuilder publicClient(boolean publicClient) {
        rep.setPublicClient(publicClient);
        return this;
    }

    public ClientConfigBuilder redirectUris(String... redirectUris) {
        rep.setRedirectUris(Collections.combine(rep.getRedirectUris(), redirectUris));
        return this;
    }

    public ClientConfigBuilder adminUrl(String adminUrl) {
        rep.setAdminUrl(adminUrl);
        return this;
    }

    public ClientConfigBuilder rootUrl(String rootUrl) {
        rep.setRootUrl(rootUrl);
        return this;
    }

    public ClientConfigBuilder baseUrl(String baseUrl) {
        rep.setBaseUrl(baseUrl);
        return this;
    }

    public ClientConfigBuilder protocol(String protocol) {
        rep.setProtocol(protocol);
        return this;
    }

    public ClientConfigBuilder bearerOnly(boolean bearerOnly) {
        rep.setBearerOnly(bearerOnly);
        return this;
    }


    public ClientConfigBuilder serviceAccountsEnabled(boolean enabled) {
        rep.setServiceAccountsEnabled(enabled);
        return this;
    }

    public ClientConfigBuilder directAccessGrantsEnabled(boolean enabled) {
        rep.setDirectAccessGrantsEnabled(enabled);
        return this;
    }

    public ClientConfigBuilder authorizationServicesEnabled(boolean enabled) {
        rep.setAuthorizationServicesEnabled(enabled);
        return this;
    }

    public ClientConfigBuilder fullScopeEnabled(boolean enabled) {
        rep.setFullScopeAllowed(enabled);
        return this;
    }

    public ClientConfigBuilder authenticatorType(String authenticatorType) {
        rep.setClientAuthenticatorType(authenticatorType);
        return this;
    }

    public ClientConfigBuilder attribute(String key, String value) {
        if (rep.getAttributes() == null) {
            rep.setAttributes(new HashMap<>());
        }

        rep.getAttributes().put(key, value);
        return this;
    }

    public ClientConfigBuilder defaultClientScopes(String... defaultClientScopes) {
        rep.setDefaultClientScopes(Collections.combine(rep.getDefaultClientScopes(), defaultClientScopes));
        return this;
    }

    public ClientConfigBuilder protocolMappers(List<ProtocolMapperRepresentation> mappers) {
        rep.setProtocolMappers(Collections.combine(rep.getProtocolMappers(), mappers));
        return this;
    }

    public ClientConfigBuilder consentRequired(boolean enabled) {
        rep.setConsentRequired(enabled);
        return this;
    }

    public ClientConfigBuilder webOrigins(String... webOrigins) {
        rep.setWebOrigins(Collections.combine(rep.getWebOrigins(), webOrigins));
        return this;
    }

    /**
     * Best practice is to use other convenience methods when configuring a client, but while the framework is under
     * active development there may not be a way to perform all updates required. In these cases this method allows
     * applying any changes to the underlying representation.
     *
     * @param update
     * @return this
     * @deprecated
     */
    public ClientConfigBuilder update(ClientUpdate... update) {
        Arrays.stream(update).forEach(u -> u.update(rep));
        return this;
    }

    public ClientRepresentation build() {
        return rep;
    }

    public interface ClientUpdate {

        void update(ClientRepresentation client);

    }

}
