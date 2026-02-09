package org.keycloak.testsuite.util.oauth;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.keycloak.OAuth2Constants;

import org.apache.http.client.methods.CloseableHttpResponse;

public class TokenExchangeRequest extends AbstractHttpPostRequest<TokenExchangeRequest, AccessTokenResponse> {

    private final String subjectToken;
    private final String subjectTokenType;
    private String requestedTokenType;
    private String requestedSubject;
    private List<String> audience;

    TokenExchangeRequest(String subjectToken, String subjectTokenType, AbstractOAuthClient<?> client) {
        super(client);
        this.subjectToken = subjectToken;
        this.subjectTokenType = subjectTokenType;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
    }

    public TokenExchangeRequest requestedTokenType(String requestedTokenType) {
        this.requestedTokenType = requestedTokenType;
        return this;
    }

    public TokenExchangeRequest requestedSubject(String requestedSubject) {
        this.requestedSubject = requestedSubject;
        return this;
    }

    public TokenExchangeRequest audience(List<String> audience) {
        this.audience = audience;
        return this;
    }

    public TokenExchangeRequest audience(String... audience) {
        this.audience = Arrays.stream(audience).toList();
        return this;
    }

    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE);

        parameter(OAuth2Constants.SUBJECT_TOKEN, subjectToken);
        parameter(OAuth2Constants.SUBJECT_TOKEN_TYPE, subjectTokenType != null ? subjectTokenType : OAuth2Constants.ACCESS_TOKEN_TYPE);

        if (requestedTokenType != null) {
            parameter(OAuth2Constants.REQUESTED_TOKEN_TYPE, requestedTokenType);
        }

        if (requestedSubject != null) {
            parameter(OAuth2Constants.REQUESTED_SUBJECT, requestedSubject);
        }

        if (audience != null) {
            audience.forEach(a -> parameter(OAuth2Constants.AUDIENCE, a));
        }

        parameter(OAuth2Constants.SCOPE, client.config().getScope(false));
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }

}
