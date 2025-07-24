package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Metadata {

    @JsonProperty("openid_provider")
    private OPMetadata openIdProviderMetadata;

    @JsonProperty("federation_entity")
    private OpenIdFederationEntity federationEntity;

    @JsonProperty("openid_relying_party")
    private RPMetadata relyingPartyMetadata;

    @JsonProperty("oauth_authorization_server")
    private OauthAuthorizationServer oauthAuthorizationServer;

    @JsonProperty("oauth_client")
    private OauthClient oauthClient;

    @JsonProperty("oauth_resource")
    private OauthResource oauthResource;

    public OPMetadata getOpenIdProviderMetadata() {
        return openIdProviderMetadata;
    }

    public void setOpenIdProviderMetadata(OPMetadata openIdProviderMetadata) {
        this.openIdProviderMetadata = openIdProviderMetadata;
    }

    public OpenIdFederationEntity getFederationEntity() {
        return federationEntity;
    }

    public void setFederationEntity(OpenIdFederationEntity federationEntity) {
        this.federationEntity = federationEntity;
    }

    public RPMetadata getRelyingPartyMetadata() {
        return relyingPartyMetadata;
    }

    public void setRelyingPartyMetadata(RPMetadata relyingPartyMetadata) {
        this.relyingPartyMetadata = relyingPartyMetadata;
    }

    public OauthAuthorizationServer getOauthAuthorizationServer() {
        return oauthAuthorizationServer;
    }

    public void setOauthAuthorizationServer(OauthAuthorizationServer oauthAuthorizationServer) {
        this.oauthAuthorizationServer = oauthAuthorizationServer;
    }

    public OauthClient getOauthClient() {
        return oauthClient;
    }

    public void setOauthClient(OauthClient oauthClient) {
        this.oauthClient = oauthClient;
    }

    public OauthResource getOauthResource() {
        return oauthResource;
    }

    public void setOauthResource(OauthResource oauthResource) {
        this.oauthResource = oauthResource;
    }
}
