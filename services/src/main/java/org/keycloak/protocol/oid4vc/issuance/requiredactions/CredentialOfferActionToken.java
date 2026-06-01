package org.keycloak.protocol.oid4vc.issuance.requiredactions;

import org.keycloak.authentication.actiontoken.DefaultActionToken;
import org.keycloak.representations.idm.oid4vc.CredentialOfferActionConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CredentialOfferActionToken extends DefaultActionToken {

    public static final String TOKEN_TYPE = "credential-offer";
    private static final String JSON_FIELD_REQUIRED_ACTIONS = "acconf";
    private static final String JSON_FIELD_REDIRECT_URI = "reduri";

    @JsonProperty(JSON_FIELD_REQUIRED_ACTIONS)
    private CredentialOfferActionConfig actionConfig;

    @JsonProperty(JSON_FIELD_REDIRECT_URI)
    private String redirectUri;

    public CredentialOfferActionToken(String userId, int absoluteExpirationInSecs, CredentialOfferActionConfig actionConfig, String redirectUri, String clientId) {
        super(userId, TOKEN_TYPE, absoluteExpirationInSecs, null);
        setActionConfig(actionConfig);
        setRedirectUri(redirectUri);
        this.issuedFor = clientId;
    }

    public CredentialOfferActionToken(String userId, String email, int absoluteExpirationInSecs, CredentialOfferActionConfig actionConfig,  String redirectUri, String clientId) {
        this(userId, absoluteExpirationInSecs, actionConfig, redirectUri, clientId);
        setEmail(email);
    }

    private CredentialOfferActionToken() {
    }

    public CredentialOfferActionConfig getActionConfig() {
        return actionConfig;
    }

    public void setActionConfig(CredentialOfferActionConfig actionConfig) {
        this.actionConfig = actionConfig;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
