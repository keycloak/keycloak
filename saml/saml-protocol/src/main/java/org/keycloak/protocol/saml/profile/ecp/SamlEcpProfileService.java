package org.keycloak.protocol.saml.profile.ecp;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.protocol.saml.profile.ecp.util.Soap;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class SamlEcpProfileService extends SamlService {

    public SamlEcpProfileService(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        super(realm, event, authManager);
    }

    public Response authenticate(InputStream inputStream) {
        try {
            return new PostBindingProtocol() {
                @Override
                protected String getBindingType(AuthnRequestType requestAbstractType) {
                    return SamlProtocol.SAML_SOAP_BINDING;
                }

                @Override
                protected Response loginRequest(String relayState, AuthnRequestType requestAbstractType, ClientModel client) {
                    // force passive authentication when executing this profile
                    requestAbstractType.setIsPassive(true);
                    requestAbstractType.setDestination(uriInfo.getAbsolutePath());
                    return super.loginRequest(relayState, requestAbstractType, client);
                }
            }.execute(Soap.toSamlHttpPostMessage(inputStream), null, null);
        } catch (Exception e) {
            String reason = "Some error occurred while processing the AuthnRequest.";
            String detail = e.getMessage();

            if (detail == null) {
                detail = reason;
            }

            return Soap.createFault().reason(reason).detail(detail).build();
        }
    }

    @Override
    protected String getLoginProtocol() {
        return SamlEcpProfileProtocolFactory.ID;
    }

    @Override
    protected AuthenticationFlowModel getAuthenticationFlow() {
        for (AuthenticationFlowModel flowModel : realm.getAuthenticationFlows()) {
            if (flowModel.getAlias().equals(DefaultAuthenticationFlows.SAML_ECP_FLOW)) {
                return flowModel;
            }
        }

        throw new RuntimeException("Could not resolve authentication flow for SAML ECP Profile.");
    }
}
