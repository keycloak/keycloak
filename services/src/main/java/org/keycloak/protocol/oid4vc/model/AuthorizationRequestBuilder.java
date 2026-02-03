package org.keycloak.protocol.oid4vc.model;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.keycloak.protocol.oidc.utils.PkceGenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import static org.keycloak.OAuth2Constants.SCOPE_OPENID;

/**
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
public class AuthorizationRequestBuilder {

    private String clientId;
    private String nonce;
    private String redirectUri;
    private String responseType;
    private String responseMode;
    private String responseUri;
    private String request;
    private List<String> scope;
    private String state;

    private PkceGenerator pkce;
    private String codeChallenge;
    private String codeChallengeMethod;

    private List<OID4VCAuthorizationDetail> authorizationDetails;

    public static AuthorizationRequestBuilder fromUri(String uri) throws URISyntaxException {
        Map<String, String> params = new URIBuilder(uri).getQueryParams().stream()
                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue, (a, b) -> a));
        return fromHttpParameters(params);
    }

    public static AuthorizationRequestBuilder fromHttpParameters(Map<String, String> params) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            AuthorizationRequestBuilder builder = new AuthorizationRequestBuilder()
                    .withClientId(params.get("client_id"))
                    .withRedirectUri(params.get("redirect_uri"))
                    .withResponseType(params.get("response_type"))
                    .withResponseMode(params.get("response_mode"))
                    .withResponseUri(params.get("response_uri"))
                    .withRequest(params.get("request"))
                    .withNonce(params.get("nonce"))
                    .withState(params.get("state"))
                    .withCodeChallenge(params.get("code_challenge"))
                    .withCodeChallengeMethod(params.get("code_challenge_method"));

            if (params.containsKey("scope")) {
                builder.scope = Optional.ofNullable(params.get("scope"))
                        .map(s -> Arrays.asList(s.split("\\s")))
                        .orElse(null);
            }

            if (params.containsKey("authorization_details")) {
                builder.authorizationDetails = Arrays.asList(mapper.readValue(
                        params.get("authorization_details"),
                        mapper.getTypeFactory().constructCollectionType(List.class, OID4VCAuthorizationDetail[].class)
                ));
            }

            return builder;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid AuthorizationRequest", e);
        }
    }

    // =====================================================================================
    // Fluent builder methods
    // =====================================================================================

    public AuthorizationRequestBuilder withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public AuthorizationRequestBuilder withState(String clientState) {
        this.state = clientState;
        return this;
    }

    public AuthorizationRequestBuilder withCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
        return this;
    }

    public AuthorizationRequestBuilder withCodeChallengeMethod(String method) {
        this.codeChallengeMethod = method;
        return this;
    }

    public AuthorizationRequestBuilder withCodeChallenge(PkceGenerator pkce) {
        this.pkce = pkce;
        this.codeChallenge = pkce.getCodeChallenge();
        this.codeChallengeMethod = pkce.getCodeChallengeMethod();
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

    public AuthorizationRequestBuilder withRequest(String request) {
        this.request = request;
        return this;
    }

    public AuthorizationRequestBuilder withScope(String... scope) {
        this.scope = scope != null ? Arrays.asList(scope) : null;
        return this;
    }

    public AuthorizationRequestBuilder withNonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    public AuthorizationRequestBuilder withAuthorizationDetail(OID4VCAuthorizationDetail... authDetail) {
        this.authorizationDetails = authDetail != null ? Arrays.asList(authDetail) : null;
        return this;
    }

    public AuthorizationRequest build() {

        if (responseType == null)
            responseType = "code";

        // direct_post response mode requires response_uri
        if ("direct_post".equals(responseMode)) {
            if (redirectUri != null)
                throw new IllegalStateException("redirect_uri must be null for direct_post");
            if (responseUri == null)
                throw new IllegalStateException("response_uri required for direct_post");
        }

        // Default scope openid if none provided
        if (scope == null || scope.isEmpty())
            scope = List.of(SCOPE_OPENID);

        AuthorizationRequest authRequest = new AuthorizationRequest();

        // Basic params
        authRequest.setClientId(clientId);
        authRequest.setRedirectUri(redirectUri);
        authRequest.setResponseType(responseType);
        authRequest.setResponseMode(responseMode);
        authRequest.setResponseUri(responseUri);
        authRequest.setRequest(request);
        authRequest.setNonce(nonce);
        authRequest.setState(state);

        // PKCE fields
        authRequest.setCodeChallenge(codeChallenge);
        authRequest.setCodeChallengeMethod(codeChallengeMethod);

        // Scope list → space-separated string
        authRequest.setScope(String.join(" ", scope));

        // OIDC4VCI extensions
        authRequest.setAuthorizationDetails(authorizationDetails);

        return authRequest;
    }
}
