package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.util.TokenUtil;

import org.apache.http.client.methods.CloseableHttpResponse;

public class ParRequest extends AbstractHttpPostRequest<ParRequest, ParResponse> {

    public ParRequest(AbstractOAuthClient<?> client) {
        super(client);
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getPushedAuthorizationRequest();
    }

    public ParRequest signedJwt(String signedJwt) {
        parameter(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT);
        parameter(OAuth2Constants.CLIENT_ASSERTION, signedJwt);
        return this;
    }

    public ParRequest nonce(String nonce) {
        parameter(OIDCLoginProtocol.NONCE_PARAM, nonce);
        return this;
    }

    public ParRequest state(String state) {
        parameter(OIDCLoginProtocol.STATE_PARAM, state);
        return this;
    }

    public ParRequest dpopJkt(String dpopJkt) {
        parameter(OIDCLoginProtocol.DPOP_JKT, dpopJkt);
        return this;
    }

    public ParRequest codeChallenge(PkceGenerator pkceGenerator) {
        if (pkceGenerator != null) {
            codeChallenge(pkceGenerator.getCodeChallenge(), pkceGenerator.getCodeChallengeMethod());
        }
        return this;
    }

    public ParRequest codeChallenge(String codeChallenge, String codeChallengeMethod) {
        parameter(OAuth2Constants.CODE_CHALLENGE, codeChallenge);
        parameter(OAuth2Constants.CODE_CHALLENGE_METHOD, codeChallengeMethod);
        return this;
    }

    public ParRequest dpopProof(String dpopProof) {
        header(TokenUtil.TOKEN_TYPE_DPOP, dpopProof);
        return this;
    }

    public ParRequest request(String request) {
        parameter(OIDCLoginProtocol.REQUEST_PARAM, request);
        return this;
    }

    public ParRequest requestUri(String requestUri) {
        parameter(OIDCLoginProtocol.REQUEST_URI_PARAM, requestUri);
        return this;
    }

    @Override
    protected void initRequest() {
        parameter(OAuth2Constants.RESPONSE_TYPE, client.config().getResponseType());
        parameter(OIDCLoginProtocol.RESPONSE_MODE_PARAM, client.config().getResponseMode());
        parameter(OAuth2Constants.REDIRECT_URI, client.config().getRedirectUri());
        parameter(OAuth2Constants.SCOPE, client.config().getScope());
    }

    @Override
    protected void authorization() {
        parameter(OAuth2Constants.CLIENT_ID, client.config().getClientId());
        parameter(OAuth2Constants.CLIENT_SECRET, client.config().getClientSecret());
    }

    @Override
    protected ParResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new ParResponse(response);
    }
}
