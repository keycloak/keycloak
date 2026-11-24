package org.keycloak.testframework.oauth;

import java.util.HashMap;
import java.util.Map;

/**
 * Default request claims and signing settings. Creating this configuration does not register a request or generate a key.
 */
public class OIDCClientApplicationProviderConfiguration {

    private Map<String, Object> claims = new HashMap<>();
    private String signingAlgorithm = "none";
    private String curve;
    private boolean generateInitialSigningKey;

    public Map<String, Object> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, Object> claims) {
        this.claims = new HashMap<>(claims);
    }

    public String getClientId() {
        return (String) claims.get("client_id");
    }

    public void setClientId(String clientId) {
        claims.put("client_id", clientId);
    }

    public String getResponseType() {
        return (String) claims.get("response_type");
    }

    public void setResponseType(String responseType) {
        claims.put("response_type", responseType);
    }

    public String getScope() {
        return (String) claims.get("scope");
    }

    public void setScope(String scope) {
        claims.put("scope", scope);
    }

    public String getRedirectUri() {
        return (String) claims.get("redirect_uri");
    }

    public void setRedirectUri(String redirectUri) {
        claims.put("redirect_uri", redirectUri);
    }

    public String getSigningAlgorithm() {
        return signingAlgorithm;
    }

    public void setSigningAlgorithm(String signingAlgorithm) {
        this.signingAlgorithm = signingAlgorithm;
    }

    public String getCurve() {
        return curve;
    }

    public void setCurve(String curve) {
        this.curve = curve;
    }

    public boolean isGenerateInitialSigningKey() {
        return generateInitialSigningKey;
    }

    public void setGenerateInitialSigningKey(boolean generateInitialSigningKey) {
        this.generateInitialSigningKey = generateInitialSigningKey;
    }
}
