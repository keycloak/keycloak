package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.AdapterConstants;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TokenExchangeRequest extends AbstractHttpPostRequest<TokenExchangeRequest, AccessTokenResponse> {

    private final String subjectToken;
    private final String subjectTokenType;
    private List<String> audience;
    private Map<String, String> additionalParams;

    TokenExchangeRequest(String subjectToken, String subjectTokenType, AbstractOAuthClient<?> client) {
        super(client);
        this.subjectToken = subjectToken;
        this.subjectTokenType = subjectTokenType;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
    }

    public TokenExchangeRequest audience(List<String> audience) {
        this.audience = audience;
        return this;
    }

    public TokenExchangeRequest audience(String... audience) {
        this.audience = Arrays.stream(audience).toList();
        return this;
    }

    /**
     * @deprecated Additional parameters should not be passed as a map, instead specific methods should be added
     * for example <code>requestedTokenType(tokenType)</code>
     */
    @Deprecated
    public TokenExchangeRequest additionalParams(Map<String, String> additionalParams) {
        this.additionalParams = additionalParams;
        return this;
    }

    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE);

        parameter(OAuth2Constants.SUBJECT_TOKEN, subjectToken);
        parameter(OAuth2Constants.SUBJECT_TOKEN_TYPE, subjectTokenType != null ? subjectTokenType : OAuth2Constants.ACCESS_TOKEN_TYPE);

        if (audience != null) {
            audience.forEach(a -> parameter(OAuth2Constants.AUDIENCE, a));
        }

        if (additionalParams != null) {
            additionalParams.forEach(this::parameter);
        }

        parameter(AdapterConstants.CLIENT_SESSION_STATE, client.getClientSessionState());
        parameter(AdapterConstants.CLIENT_SESSION_HOST, client.getClientSessionHost());

        parameter(OAuth2Constants.SCOPE, client.config().getScope(false));
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }

}
