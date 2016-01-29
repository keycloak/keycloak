package org.keycloak.adapters.saml.profile.webbrowsersso;

import org.keycloak.adapters.saml.OnSessionCreated;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.profile.SamlInvocationContext;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.saml.common.constants.GeneralConstants;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlEndpoint extends WebBrowserSsoAuthenticationHandler {
    public SamlEndpoint(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        super(facade, deployment, sessionStore);
    }

    @Override
    public AuthOutcome handle(OnSessionCreated onCreateSession) {
        String samlRequest = facade.getRequest().getFirstParam(GeneralConstants.SAML_REQUEST_KEY);
        String samlResponse = facade.getRequest().getFirstParam(GeneralConstants.SAML_RESPONSE_KEY);
        String relayState = facade.getRequest().getFirstParam(GeneralConstants.RELAY_STATE);
        if (samlRequest != null) {
            return handleSamlRequest(samlRequest, relayState);
        } else if (samlResponse != null) {
            return handleSamlResponse(samlResponse, relayState, onCreateSession);
        }
        return AuthOutcome.NOT_ATTEMPTED;

    }
}
