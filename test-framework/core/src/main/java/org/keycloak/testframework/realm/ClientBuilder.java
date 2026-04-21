package org.keycloak.testframework.realm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.util.Collections;

public class ClientBuilder {

    private final ClientRepresentation rep;

    private ClientBuilder(ClientRepresentation rep) {
        this.rep = rep;
    }

    public static ClientBuilder create() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setEnabled(true);
        return new ClientBuilder(rep);
    }

    public static ClientBuilder update(ClientRepresentation rep) {
        return new ClientBuilder(rep);
    }

    public ClientBuilder enabled(boolean enabled) {
        rep.setEnabled(enabled);
        return this;
    }

    public ClientBuilder clientId(String clientId) {
        rep.setClientId(clientId);
        return this;
    }

    public ClientBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public ClientBuilder secret(String secret) {
        rep.setSecret(secret);
        return this;
    }

    public ClientBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public ClientBuilder description(String description) {
        rep.setDescription(description);
        return this;
    }

    public ClientBuilder publicClient(boolean publicClient) {
        rep.setPublicClient(publicClient);
        return this;
    }

    public ClientBuilder redirectUris(String... redirectUris) {
        rep.setRedirectUris(Collections.combine(rep.getRedirectUris(), redirectUris));
        return this;
    }

    public ClientBuilder adminUrl(String adminUrl) {
        rep.setAdminUrl(adminUrl);
        return this;
    }

    public ClientBuilder rootUrl(String rootUrl) {
        rep.setRootUrl(rootUrl);
        return this;
    }

    public ClientBuilder baseUrl(String baseUrl) {
        rep.setBaseUrl(baseUrl);
        return this;
    }

    public ClientBuilder protocol(String protocol) {
        rep.setProtocol(protocol);
        return this;
    }

    public ClientBuilder bearerOnly(boolean bearerOnly) {
        rep.setBearerOnly(bearerOnly);
        return this;
    }


    public ClientBuilder serviceAccountsEnabled(boolean enabled) {
        rep.setServiceAccountsEnabled(enabled);
        return this;
    }

    public ClientBuilder directAccessGrantsEnabled(boolean enabled) {
        rep.setDirectAccessGrantsEnabled(enabled);
        return this;
    }

    public ClientBuilder authorizationServicesEnabled(boolean enabled) {
        serviceAccountsEnabled(enabled);
        rep.setAuthorizationServicesEnabled(enabled);
        return this;
    }

    public ClientBuilder fullScopeEnabled(boolean enabled) {
        rep.setFullScopeAllowed(enabled);
        return this;
    }

    public ClientBuilder authenticatorType(String authenticatorType) {
        rep.setClientAuthenticatorType(authenticatorType);
        return this;
    }

    public ClientBuilder attribute(String key, String value) {
        if (rep.getAttributes() == null) {
            rep.setAttributes(new HashMap<>());
        }

        rep.getAttributes().put(key, value);
        return this;
    }

    public ClientBuilder defaultClientScopes(String... defaultClientScopes) {
        rep.setDefaultClientScopes(Collections.combine(rep.getDefaultClientScopes(), defaultClientScopes));
        return this;
    }

    public ClientBuilder optionalClientScopes(String... optionalClientScopes) {
        rep.setOptionalClientScopes(Collections.combine(rep.getOptionalClientScopes(), optionalClientScopes));
        return this;
    }

    public ClientBuilder protocolMappers(List<ProtocolMapperRepresentation> mappers) {
        rep.setProtocolMappers(Collections.combine(rep.getProtocolMappers(), mappers));
        return this;
    }

    public ClientBuilder consentRequired(boolean enabled) {
        rep.setConsentRequired(enabled);
        return this;
    }

    public ClientBuilder webOrigins(String... webOrigins) {
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
    public ClientBuilder update(ClientUpdate... update) {
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
