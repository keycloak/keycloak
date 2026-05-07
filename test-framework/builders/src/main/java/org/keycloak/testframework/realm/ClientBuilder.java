package org.keycloak.testframework.realm;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

public class ClientBuilder extends Builder<ClientRepresentation> {

    private ClientBuilder(ClientRepresentation rep) {
        super(rep);
    }

    public static ClientBuilder create() {
        return new ClientBuilder(new ClientRepresentation()).enabled(true);
    }

    public static ClientBuilder create(String clientId) {
        return create().clientId(clientId);
    }

    public static ClientBuilder update(ClientRepresentation rep) {
        return new ClientBuilder(rep);
    }

    public ClientBuilder enabled(Boolean enabled) {
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

    public ClientBuilder type(String type) {
        rep.setType(type);
        return this;
    }

    public ClientBuilder publicClient() {
        return publicClient(true);
    }

    public ClientBuilder publicClient(Boolean publicClient) {
        rep.setPublicClient(publicClient);
        return this;
    }

    public ClientBuilder redirectUris(String... redirectUris) {
        rep.setRedirectUris(combine(rep.getRedirectUris(), redirectUris));
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

    public ClientBuilder bearerOnly(Boolean bearerOnly) {
        rep.setBearerOnly(bearerOnly);
        return this;
    }

    public ClientBuilder serviceAccountsEnabled() {
        return serviceAccountsEnabled(true);
    }

    public ClientBuilder serviceAccountsEnabled(Boolean enabled) {
        rep.setServiceAccountsEnabled(enabled);
        return this;
    }

    public ClientBuilder directAccessGrantsEnabled() {
        return directAccessGrantsEnabled(true);
    }

    public ClientBuilder directAccessGrantsEnabled(Boolean enabled) {
        rep.setDirectAccessGrantsEnabled(enabled);
        return this;
    }

    public ClientBuilder authorizationServicesEnabled(Boolean enabled) {
        serviceAccountsEnabled(enabled);
        rep.setAuthorizationServicesEnabled(enabled);
        return this;
    }

    public ClientBuilder fullScopeEnabled(Boolean enabled) {
        rep.setFullScopeAllowed(enabled);
        return this;
    }

    public ClientBuilder frontchannelLogout(Boolean enabled) {
        rep.setFrontchannelLogout(enabled);
        return this;
    }

    public ClientBuilder authenticatorType(String authenticatorType) {
        rep.setClientAuthenticatorType(authenticatorType);
        return this;
    }

    public ClientBuilder attribute(String key, String value) {
        rep.setAttributes(Builder.createIfNull(rep.getAttributes(), HashMap::new));
        rep.getAttributes().put(key, value);
        return this;
    }

    public ClientBuilder attributes(Map<String, String> attributes) {
        rep.setAttributes(combineMap(rep.getAttributes(), attributes));
        return this;
    }

    public ClientBuilder removeAttributes(String... keys) {
        rep.setAttributes(removeKeys(rep.getAttributes(), keys));
        return this;
    }

    public ClientBuilder defaultClientScopes(String... defaultClientScopes) {
        rep.setDefaultClientScopes(combine(rep.getDefaultClientScopes(), defaultClientScopes));
        return this;
    }

    public ClientBuilder optionalClientScopes(String... optionalClientScopes) {
        rep.setOptionalClientScopes(combine(rep.getOptionalClientScopes(), optionalClientScopes));
        return this;
    }

    public ClientBuilder protocolMappers(ProtocolMapperRepresentation... mappers) {
        rep.setProtocolMappers(combine(rep.getProtocolMappers(), mappers));
        return this;
    }

    @Deprecated
    public ClientBuilder defaultRoles(String... roles) {
        rep.setDefaultRoles(combine(rep.getDefaultRoles(), roles));
        return this;
    }

    public ClientBuilder consentRequired(Boolean enabled) {
        rep.setConsentRequired(enabled);
        return this;
    }

    public ClientBuilder webOrigins(String... webOrigins) {
        rep.setWebOrigins(combine(rep.getWebOrigins(), webOrigins));
        return this;
    }

    public ClientBuilder alwaysDisplayInConsole(Boolean alwaysDisplayInConsole) {
        rep.setAlwaysDisplayInConsole(alwaysDisplayInConsole);
        return this;
    }

}
