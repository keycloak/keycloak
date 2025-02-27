package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;

import java.io.IOException;
import java.util.Map;

public class BackchannelAuthenticationRequest extends AbstractHttpPostRequest<BackchannelAuthenticationRequest, AuthenticationRequestAcknowledgement> {

    private final String userid;
    private final String bindingMessage;
    private final String acrValues;
    private final String clientNotificationToken;
    private final Map<String, String> additionalParams;

    BackchannelAuthenticationRequest(String userid, String bindingMessage, String acrValues, String clientNotificationToken, Map<String, String> additionalParams, OAuthClient client) {
        super(client);
        this.userid = userid;
        this.bindingMessage = bindingMessage;
        this.acrValues = acrValues;
        this.clientNotificationToken = clientNotificationToken;
        this.additionalParams = additionalParams;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getBackchannelAuthentication();
    }

    protected void initRequest() {
        parameter(OIDCLoginProtocol.LOGIN_HINT_PARAM, userid);
        parameter(CibaGrantType.BINDING_MESSAGE, bindingMessage);
        parameter(OAuth2Constants.ACR_VALUES, acrValues);
        parameter(CibaGrantType.CLIENT_NOTIFICATION_TOKEN, clientNotificationToken);
        parameter(OIDCLoginProtocol.REQUEST_URI_PARAM, client.getRequestUri());
        parameter(OIDCLoginProtocol.REQUEST_PARAM, client.getRequest());
        parameter(OIDCLoginProtocol.CLAIMS_PARAM, client.getClaims());

        if (additionalParams != null) {
            additionalParams.forEach(this::parameter);
        }

        scope();
    }

    @Override
    protected AuthenticationRequestAcknowledgement toResponse(CloseableHttpResponse response) throws IOException {
        return new AuthenticationRequestAcknowledgement(response);
    }

}
