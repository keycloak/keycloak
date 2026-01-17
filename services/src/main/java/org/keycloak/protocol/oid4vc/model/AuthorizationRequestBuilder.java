package org.keycloak.protocol.oid4vc.model;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.common.util.Base64Url;

/**
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
public class AuthorizationRequestBuilder {

    private String clientId;
    private String codeVerifier;

    private String clientState;
    private String codeChallengeMethod;
    private String issuer;
    private String responseType;
    private String responseMode;
    private String redirectUri;
    private String responseUri;

    private boolean buildAuthorizationDetails = true;

    private final List<String> scopes = new ArrayList<>();
    private final List<String> credentialConfigurationIds = new ArrayList<>();

    private String codeChallenge;

    // =====================================================================================
    // Fluent builder methods
    // =====================================================================================

    public AuthorizationRequestBuilder withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public AuthorizationRequestBuilder withClientState(String clientState) {
        this.clientState = clientState;
        return this;
    }

    public AuthorizationRequestBuilder withCodeChallengeMethod(String method) {
        if (!"S256".equals(method))
            throw new IllegalArgumentException("Unsupported code challenge method: " + method);
        this.codeChallengeMethod = method;
        return this;
    }

    public AuthorizationRequestBuilder withCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
        return this;
    }

    public AuthorizationRequestBuilder withIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public AuthorizationRequestBuilder withCredentialConfigurationIds(String... ids) {
        this.credentialConfigurationIds.addAll(Arrays.asList(ids));
        return this;
    }

    public AuthorizationRequestBuilder withRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    public AuthorizationRequestBuilder withResponseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

    public AuthorizationRequestBuilder withResponseMode(String responseMode) {
        this.responseMode = responseMode;
        return this;
    }

    public AuthorizationRequestBuilder withResponseUri(String responseUri) {
        this.responseUri = responseUri;
        return this;
    }

    public AuthorizationRequestBuilder withScopes(List<String> scopes) {
        this.scopes.addAll(scopes);
        return this;
    }

    public AuthorizationRequestBuilder withAuthorizationDetails(boolean flag) {
        this.buildAuthorizationDetails = flag;
        return this;
    }

    public AuthorizationRequest build() {

        if (clientId == null || clientId.isBlank())
            throw new IllegalArgumentException("No client_id");

        if (responseType == null)
            responseType = "code";

        // Build PKCE code challenge
        if (codeChallengeMethod != null) {
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                byte[] hash = sha256.digest(codeVerifier.getBytes());
                codeChallenge = Base64Url.encode(hash);
            } catch (Exception ex) {
                throw new RuntimeException("PKCE hash failure", ex);
            }
        }

        // direct_post response mode requires response_uri
        if ("direct_post".equals(responseMode)) {
            if (redirectUri != null)
                throw new IllegalStateException("redirect_uri must be null for direct_post");
            if (responseUri == null)
                throw new IllegalStateException("response_uri required for direct_post");
        }

        List<AuthorizationDetail> authDetails = null;

        if (buildAuthorizationDetails && !credentialConfigurationIds.isEmpty()) {
            authDetails = credentialConfigurationIds.stream()
                    .map(cfgId -> {
                        AuthorizationDetail ad = new AuthorizationDetail();
                        ad.setType("openid_credential");
                        ad.setCredentialConfigurationId(cfgId);
                        if (issuer != null) {
                            ad.setLocations(List.of(issuer));
                        }
                        return ad;
                    })
                    .collect(Collectors.toList());
        }

        // Default scope openid if none provided
        if (scopes.isEmpty())
            scopes.add("openid");

        AuthorizationRequest authRequest = new AuthorizationRequest();

        // Basic params
        authRequest.setClientId(clientId);
        authRequest.setResponseType(responseType);
        authRequest.setResponseMode(responseMode);
        authRequest.setRedirectUri(redirectUri);
        authRequest.setResponseUri(responseUri);
        authRequest.setNonce(null);
        authRequest.setState(clientState);

        // PKCE fields
        authRequest.setCodeChallenge(codeChallenge);
        authRequest.setCodeChallengeMethod(codeChallengeMethod);

        // Scope list → space-separated string
        authRequest.setScope(String.join(" ", scopes));

        // OIDC4VCI extensions
        authRequest.setAuthorizationDetails(authDetails);
        authRequest.setClientMetadata(null); // filled by caller if needed

        return authRequest;
    }
}
