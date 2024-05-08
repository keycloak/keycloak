package org.keycloak.services.clienttype.client;

import org.keycloak.client.clienttype.ClientType;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeDefaultedClientRepresentation extends ClientRepresentation {

    private final ClientType clientType;
    private final ClientRepresentation delegate;

    public TypeDefaultedClientRepresentation(ClientType clientType, ClientRepresentation delegate) {
        this.clientType = clientType;
        this.delegate = delegate;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public void setId(String id) {
        delegate.setId(id);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    @Override
    public String getType() {
        return delegate.getType();
    }

    @Override
    public void setType(String type) {
        delegate.setType(type);
    }

    @Override
    public String getClientId() {
        return delegate.getClientId();
    }

    @Override
    public void setClientId(String clientId) {
        delegate.setClientId(clientId);
    }

    @Override
    public Boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public void setEnabled(Boolean enabled) {
        delegate.setEnabled(enabled);
    }

    @Override
    public Boolean isStandardFlowEnabled() {
        return TypedClientSimpleAttribute.STANDARD_FLOW_ENABLED
                .orDefault(clientType, delegate::isStandardFlowEnabled, Boolean.class);
    }

    @Override
    public void setStandardFlowEnabled(Boolean standardFlowEnabled) {
        delegate.setStandardFlowEnabled(standardFlowEnabled);
    }

    @Override
    public Boolean isBearerOnly() {
        return TypedClientSimpleAttribute.BEARER_ONLY
                .orDefault(clientType, delegate::isBearerOnly, Boolean.class);
    }

    @Override
    public void setBearerOnly(Boolean bearerOnly) {
        delegate.setBearerOnly(bearerOnly);
    }

    @Override
    public Boolean isConsentRequired() {
        return TypedClientSimpleAttribute.CONSENT_REQUIRED
                .orDefault(clientType, delegate::isConsentRequired, Boolean.class);
    }

    @Override
    public void setConsentRequired(Boolean consentRequired) {
        delegate.setConsentRequired(consentRequired);
    }

    @Override
    public Boolean isDirectAccessGrantsEnabled() {
        return TypedClientSimpleAttribute.DIRECT_ACCESS_GRANTS_ENABLED
                .orDefault(clientType, delegate::isDirectAccessGrantsEnabled, Boolean.class);
    }

    @Override
    public void setDirectAccessGrantsEnabled(Boolean directAccessGrantsEnabled) {
        delegate.setDirectAccessGrantsEnabled(directAccessGrantsEnabled);
    }

    @Override
    public Boolean isAlwaysDisplayInConsole() {
        return TypedClientSimpleAttribute.ALWAYS_DISPLAY_IN_CONSOLE
                .orDefault(clientType, delegate::isAlwaysDisplayInConsole, Boolean.class);
    }

    @Override
    public void setAlwaysDisplayInConsole(Boolean alwaysDisplayInConsole) {
        delegate.setAlwaysDisplayInConsole(alwaysDisplayInConsole);
    }

    @Override
    public Boolean isSurrogateAuthRequired() {
        return delegate.isSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(Boolean surrogateAuthRequired) {
        delegate.setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public String getRootUrl() {
        return delegate.getRootUrl();
    }

    @Override
    public void setRootUrl(String rootUrl) {
        delegate.setRootUrl(rootUrl);
    }

    @Override
    public String getAdminUrl() {
        return delegate.getAdminUrl();
    }

    @Override
    public void setAdminUrl(String adminUrl) {
        delegate.setAdminUrl(adminUrl);
    }

    @Override
    public String getBaseUrl() {
        return delegate.getBaseUrl();
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        delegate.setBaseUrl(baseUrl);
    }

    @Override
    public String getClientAuthenticatorType() {
        return delegate.getClientAuthenticatorType();
    }

    @Override
    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        delegate.setClientAuthenticatorType(clientAuthenticatorType);
    }

    @Override
    public String getSecret() {
        return delegate.getSecret();
    }

    @Override
    public void setSecret(String secret) {
        delegate.setSecret(secret);
    }

    @Override
    public String getRegistrationAccessToken() {
        return delegate.getRegistrationAccessToken();
    }

    @Override
    public void setRegistrationAccessToken(String registrationAccessToken) {
        delegate.setRegistrationAccessToken(registrationAccessToken);
    }

    @Override
    public Boolean isFrontchannelLogout() {
        return TypedClientSimpleAttribute.FRONTCHANNEL_LOGOUT
                .orDefault(clientType, delegate::isFrontchannelLogout, Boolean.class);
    }

    @Override
    public void setFrontchannelLogout(Boolean frontchannelLogout) {
        delegate.setFrontchannelLogout(frontchannelLogout);
    }

    @Override
    public List<ProtocolMapperRepresentation> getProtocolMappers() {
        return delegate.getProtocolMappers();
    }

    @Override
    public void setProtocolMappers(List<ProtocolMapperRepresentation> protocolMappers) {
        delegate.setProtocolMappers(protocolMappers);
    }

    @Override
    public String getClientTemplate() {
        return delegate.getClientTemplate();
    }

    @Override
    public Boolean isUseTemplateConfig() {
        return delegate.isUseTemplateConfig();
    }

    @Override
    public Boolean isUseTemplateScope() {
        return delegate.isUseTemplateScope();
    }

    @Override
    public Boolean isUseTemplateMappers() {
        return delegate.isUseTemplateMappers();
    }

    @Override
    public List<String> getDefaultClientScopes() {
        return delegate.getDefaultClientScopes();
    }

    @Override
    public void setDefaultClientScopes(List<String> defaultClientScopes) {
        delegate.setDefaultClientScopes(defaultClientScopes);
    }

    @Override
    public List<String> getOptionalClientScopes() {
        return delegate.getOptionalClientScopes();
    }

    @Override
    public void setOptionalClientScopes(List<String> optionalClientScopes) {
        delegate.setOptionalClientScopes(optionalClientScopes);
    }

    @Override
    public ResourceServerRepresentation getAuthorizationSettings() {
        return delegate.getAuthorizationSettings();
    }

    @Override
    public void setAuthorizationSettings(ResourceServerRepresentation authorizationSettings) {
        delegate.setAuthorizationSettings(authorizationSettings);
    }

    @Override
    public Map<String, Boolean> getAccess() {
        return delegate.getAccess();
    }

    @Override
    public void setAccess(Map<String, Boolean> access) {
        delegate.setAccess(access);
    }

    @Override
    public String getOrigin() {
        return delegate.getOrigin();
    }

    @Override
    public void setOrigin(String origin) {
        delegate.setOrigin(origin);
    }

    @Override
    public Boolean isImplicitFlowEnabled() {
        return TypedClientSimpleAttribute.IMPLICIT_FLOW_ENABLED
                .orDefault(clientType, delegate::isImplicitFlowEnabled, Boolean.class);
    }

    @Override
    public void setImplicitFlowEnabled(Boolean implicitFlowEnabled) {
        delegate.setImplicitFlowEnabled(implicitFlowEnabled);
    }

    @Override
    public Boolean isServiceAccountsEnabled() {
        return TypedClientSimpleAttribute.SERVICE_ACCOUNTS_ENABLED
                .orDefault(clientType, delegate::isServiceAccountsEnabled, Boolean.class);
    }

    @Override
    public void setServiceAccountsEnabled(Boolean serviceAccountsEnabled) {
        delegate.setServiceAccountsEnabled(serviceAccountsEnabled);
    }

    @Override
    public Boolean getAuthorizationServicesEnabled() {
        return delegate.getAuthorizationServicesEnabled();
    }

    @Override
    public void setAuthorizationServicesEnabled(Boolean authorizationServicesEnabled) {
        delegate.setAuthorizationServicesEnabled(authorizationServicesEnabled);
    }

    @Override
    public Boolean isDirectGrantsOnly() {
        return delegate.isDirectGrantsOnly();
    }

    @Override
    public void setDirectGrantsOnly(Boolean directGrantsOnly) {
        delegate.setDirectGrantsOnly(directGrantsOnly);
    }

    @Override
    public String getProtocol() {
        return TypedClientSimpleAttribute.PROTOCOL
                .orDefault(clientType, delegate::getProtocol, String.class);
    }

    @Override
    public void setProtocol(String protocol) {
        delegate.setProtocol(protocol);
    }

    @Override
    public Boolean isPublicClient() {
        return TypedClientSimpleAttribute.PUBLIC_CLIENT
                .orDefault(clientType, delegate::isPublicClient, Boolean.class);
    }

    @Override
    public void setPublicClient(Boolean publicClient) {
        delegate.setPublicClient(publicClient);
    }

    @Override
    public Boolean isFullScopeAllowed() {
        return delegate.isFullScopeAllowed();
    }

    @Override
    public void setFullScopeAllowed(Boolean fullScopeAllowed) {
        delegate.setFullScopeAllowed(fullScopeAllowed);
    }

    @Override
    public List<String> getWebOrigins() {
        Set<String> webOrigins = TypedClientSimpleAttribute.WEB_ORIGINS
                .orDefault(clientType, () -> CollectionUtil.collectionToSet(delegate.getWebOrigins()), Set.class);

        return webOrigins == null ? null : new ArrayList<>(webOrigins);
    }

    @Override
    public void setWebOrigins(List<String> webOrigins) {
        delegate.setWebOrigins(webOrigins);
    }

    @Override
    public String[] getDefaultRoles() {
        return delegate.getDefaultRoles();
    }

    @Override
    public void setDefaultRoles(String[] defaultRoles) {
        delegate.setDefaultRoles(defaultRoles);
    }

    @Override
    public Integer getNotBefore() {
        return delegate.getNotBefore();
    }

    @Override
    public void setNotBefore(Integer notBefore) {
        delegate.setNotBefore(notBefore);
    }

    @Override
    public List<String> getRedirectUris() {
        Set<String> redirectUris = TypedClientSimpleAttribute.REDIRECT_URIS
            .orDefault(clientType, () -> CollectionUtil.collectionToSet(delegate.getRedirectUris()), Set.class);

        return redirectUris == null ? null : new ArrayList<>(redirectUris);
    }

    @Override
    public void setRedirectUris(List<String> redirectUris) {
        delegate.setRedirectUris(redirectUris);
    }

    private String getAttribute(String name) {
        TypedClientExtendedAttribute attribute = TypedClientExtendedAttribute.getAttributesByName().get(name);
        if (attribute != null) {
            return attribute.orDefault(clientType, () -> getAttributesSafe().get(name), String.class);
        } else {
            return delegate.getAttributes().get(name);
        }
    }

    private Map<String, String> getAttributesSafe() {
        if (delegate.getAttributes() == null) {
            return new HashMap<>();
        }
        return delegate.getAttributes();
    }

    @Override
    public Map<String, String> getAttributes() {
        // Start with attributes set on the delegate.
        Map<String, String> attributes = new HashMap<>(getAttributesSafe());

        // Get extended client type attributes and values from the client type configuration.
        Set<String> extendedClientTypeAttributes =
                clientType.getConfiguration().entrySet().stream()
                        .map(Map.Entry::getKey)
                        .filter(entry -> TypedClientExtendedAttribute.getAttributesByName().containsKey(entry))
                        .collect(Collectors.toSet());

        // Augment client type attributes on top of attributes on the delegate.
        for (String entry : extendedClientTypeAttributes) {
            attributes.put(entry, getAttribute(entry));
        }

        return attributes;
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {
        delegate.setAttributes(attributes);
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        return delegate.getAuthenticationFlowBindingOverrides();
    }

    @Override
    public void setAuthenticationFlowBindingOverrides(Map<String, String> authenticationFlowBindingOverrides) {
        delegate.setAuthenticationFlowBindingOverrides(authenticationFlowBindingOverrides);
    }

    @Override
    public Integer getNodeReRegistrationTimeout() {
        return delegate.getNodeReRegistrationTimeout();
    }

    @Override
    public void setNodeReRegistrationTimeout(Integer nodeReRegistrationTimeout) {
        delegate.setNodeReRegistrationTimeout(nodeReRegistrationTimeout);
    }

    @Override
    public Map<String, Integer> getRegisteredNodes() {
        return delegate.getRegisteredNodes();
    }

    @Override
    public void setRegisteredNodes(Map<String, Integer> registeredNodes) {
        delegate.setRegisteredNodes(registeredNodes);
    }
}
