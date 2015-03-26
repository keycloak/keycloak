package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientRepresentation {
    protected String id;
    protected String name;
    protected List<String> redirectUris;
    protected List<String> webOrigins;
    protected Boolean enabled;
    protected String secret;
    @Deprecated
    protected ClaimRepresentation claims;
    protected Integer notBefore;
    protected Boolean publicClient;
    protected String protocol;
    protected Map<String, String> attributes;
    protected Boolean directGrantsOnly;
    protected Boolean fullScopeAllowed;
    protected Boolean frontchannelLogout;
    protected List<ProtocolMapperRepresentation> protocolMappers;
    private List<ClientIdentityProviderMappingRepresentation> identityProviders;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(List<String> webOrigins) {
        this.webOrigins = webOrigins;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public ClaimRepresentation getClaims() {
        return claims;
    }

    public void setClaims(ClaimRepresentation claims) {
        this.claims = claims;
    }

    public Integer getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Integer notBefore) {
        this.notBefore = notBefore;
    }

    public Boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(Boolean publicClient) {
        this.publicClient = publicClient;
    }

    public Boolean isDirectGrantsOnly() {
        return directGrantsOnly;
    }

    public void setDirectGrantsOnly(Boolean directGrantsOnly) {
        this.directGrantsOnly = directGrantsOnly;
    }

    public Boolean isFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public void setFullScopeAllowed(Boolean fullScopeAllowed) {
        this.fullScopeAllowed = fullScopeAllowed;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Boolean isFrontchannelLogout() {
        return frontchannelLogout;
    }

    public void setFrontchannelLogout(Boolean frontchannelLogout) {
        this.frontchannelLogout = frontchannelLogout;
    }

    public List<ClientIdentityProviderMappingRepresentation> getIdentityProviders() {
        return this.identityProviders;
    }

    public void setIdentityProviders(List<ClientIdentityProviderMappingRepresentation> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public List<ProtocolMapperRepresentation> getProtocolMappers() {
        return protocolMappers;
    }

    public void setProtocolMappers(List<ProtocolMapperRepresentation> protocolMappers) {
        this.protocolMappers = protocolMappers;
    }
}
