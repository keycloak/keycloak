package org.keycloak.broker.oidc;

import org.keycloak.broker.oidc.util.SimpleHttp;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.adapters.action.AdminAction;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakOIDCIdentityProvider extends OIDCIdentityProvider {

    public KeycloakOIDCIdentityProvider(OIDCIdentityProviderConfig config) {
        super(config);
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new KeycloakEndpoint(callback, realm, event);
    }

    protected class KeycloakEndpoint extends OIDCEndpoint {
        public KeycloakEndpoint(AuthenticationCallback callback, RealmModel realm, EventBuilder event) {
            super(callback, realm, event);
        }

        @POST
        @Path(AdapterConstants.K_LOGOUT)
        public Response backchannelLogout(String input) {
            JWSInput token = new JWSInput(input);
            String signingCert = getConfig().getSigningCertificate();
            if (signingCert != null && !signingCert.trim().equals("")) {
                if (!token.verify(getConfig().getSigningCertificate())) {
                    return Response.status(400).build();            }
            }
            LogoutAction action = null;
            try {
                action = JsonSerialization.readValue(token.getContent(), LogoutAction.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!validateAction(action)) return Response.status(400).build();
            if (action.getKeycloakSessionIds() != null) {
                for (String sessionId : action.getKeycloakSessionIds()) {
                    String brokerSessionId = getConfig().getAlias() + "." + sessionId;
                    UserSessionModel userSession = session.sessions().getUserSessionByBrokerSessionId(realm, brokerSessionId);
                    if (userSession != null) {
                        AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, clientConnection, headers);
                    }
                }

            }
            return Response.ok().build();
        }

        protected boolean validateAction(AdminAction action)  {
            if (!action.validate()) {
                logger.warn("admin request failed, not validated" + action.getAction());
                return false;
            }
            if (action.isExpired()) {
                logger.warn("admin request failed, expired token");
                return false;
            }
            if (!getConfig().getClientId().equals(action.getResource())) {
                logger.warn("Resource name does not match");
                return false;

            }
            return true;
        }

        @Override
        public SimpleHttp generateTokenRequest(String authorizationCode) {
            return super.generateTokenRequest(authorizationCode)
                    .param(AdapterConstants.APPLICATION_SESSION_STATE, "n/a");  // hack to get backchannel logout to work

        }



    }
}
