package org.keycloak.broker.oidc;

import org.keycloak.broker.oidc.util.SimpleHttp;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.adapters.action.AdminAction;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakIdentityProvider extends OIDCIdentityProvider {

    public KeycloakIdentityProvider(OIDCIdentityProviderConfig config) {
        super(config);
    }

    protected class KeycloakEndpoint extends OIDCEndpoint {
        public KeycloakEndpoint(AuthenticationCallback callback, RealmModel realm) {
            super(callback, realm);
        }

        @POST
        @Path(AdapterConstants.K_LOGOUT)
        public Response backchannelLogout(String input) {
            JWSInput token = new JWSInput(input);
            if (!token.verify(getConfig().getSigningCertificate())) {
                return Response.status(400).build();            }
            LogoutAction action = null;
            try {
                action = JsonSerialization.readValue(token.getContent(), LogoutAction.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!validateAction(action)) return Response.status(400).build();
            if (action.getAdapterSessionIds() != null) {

            }
            throw new RuntimeException("not impelmented yet");
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


    }
}
