package org.keycloak.protocol.saml;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.flows.Flows;
import org.picketlink.common.constants.GeneralConstants;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.identity.federation.core.saml.v2.constants.X500SAMLProfileConstants;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlLogin implements LoginProtocol {
    protected static final Logger logger = Logger.getLogger(SamlLogin.class);
    public static final String LOGIN_PROTOCOL = "saml";
    public static final String SAML_BINDING = "saml_binding";
    public static final String SAML_POST_BINDING = "post";

    protected KeycloakSession session;

    protected RealmModel realm;

    protected HttpRequest request;

    protected UriInfo uriInfo;

    protected ClientConnection clientConnection;


    @Override
    public SamlLogin setSession(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public SamlLogin setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public SamlLogin setRequest(HttpRequest request) {
        this.request = request;
        return this;
    }

    @Override
    public SamlLogin setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    @Override
    public SamlLogin setClientConnection(ClientConnection clientConnection) {
        this.clientConnection = clientConnection;
        return this;
    }

    @Override
    public Response cancelLogin(ClientSessionModel clientSession) {
        return getErrorResponse(clientSession, JBossSAMLURIConstants.STATUS_REQUEST_DENIED.get());
    }

    @Override
    public Response invalidSessionError(ClientSessionModel clientSession) {
        return getErrorResponse(clientSession, JBossSAMLURIConstants.STATUS_AUTHNFAILED.get());
    }

    protected String getResponseIssuer(RealmModel realm) {
        return RealmsResource.realmBaseUrl(uriInfo).build(realm.getName()).toString();
    }

    protected Response getErrorResponse(ClientSessionModel clientSession, String status) {
        String relayState = clientSession.getNote(GeneralConstants.RELAY_STATE);
        String redirectUri = clientSession.getRedirectUri();
        SAML2PostBindingResponseBuilder builder = new SAML2PostBindingResponseBuilder();
        String responseIssuer = getResponseIssuer(realm);
        builder .relayState(relayState)
                .destination(redirectUri)
                .responseIssuer(responseIssuer)
                .requestIssuer(clientSession.getClient().getClientId());
        try {
            return builder.error(status);
        } catch (Exception e) {
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Failed to process response");
        }
    }

    @Override
    public Response authenticated(UserSessionModel userSession, ClientSessionCode accessCode) {
        ClientSessionModel clientSession = accessCode.getClientSession();
        if (SamlLogin.SAML_POST_BINDING.equals(clientSession.getNote(SamlLogin.SAML_BINDING))) {
            return postBinding(userSession, clientSession);
        }
        throw new RuntimeException("still need to implement redirect binding");
    }

    protected Response postBinding(UserSessionModel userSession, ClientSessionModel clientSession) {
        String requestID = clientSession.getNote("REQUEST_ID");
        String relayState = clientSession.getNote(GeneralConstants.RELAY_STATE);
        String redirectUri = clientSession.getRedirectUri();
        String responseIssuer = getResponseIssuer(realm);

        SAML2PostBindingResponseBuilder builder = new SAML2PostBindingResponseBuilder();
        builder.requestID(requestID)
               .relayState(relayState)
               .destination(redirectUri)
               .responseIssuer(responseIssuer)
               .requestIssuer(clientSession.getClient().getClientId())
               .userPrincipal(userSession.getUser().getUsername()) // todo userId instead?  There is no username claim it seems
               .attribute(X500SAMLProfileConstants.USERID.getFriendlyName(), userSession.getUser().getId())
               .authMethod(JBossSAMLURIConstants.AC_UNSPECIFIED.get());
        initClaims(builder, clientSession.getClient(), userSession.getUser());
        if (clientSession.getRoles() != null) {
            for (String roleId : clientSession.getRoles()) {
                // todo need a role mapping
                RoleModel roleModel = clientSession.getRealm().getRoleById(roleId);
                builder.roles(roleModel.getName());
            }
        }

        try {
            return builder.build();
        } catch (Exception e) {
            logger.error("failed", e);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Failed to process response");
        }
    }

    public void initClaims(SAML2PostBindingResponseBuilder builder, ClientModel model, UserModel user) {
        if (ClaimMask.hasEmail(model.getAllowedClaimsMask())) {
            builder.attribute(X500SAMLProfileConstants.EMAIL_ADDRESS.getFriendlyName(), user.getEmail());
        }
        if (ClaimMask.hasName(model.getAllowedClaimsMask())) {
            builder.attribute(X500SAMLProfileConstants.GIVEN_NAME.getFriendlyName(), user.getFirstName());
            builder.attribute(X500SAMLProfileConstants.SURNAME.getFriendlyName(), user.getLastName());
        }
    }


    @Override
    public Response consentDenied(ClientSessionModel clientSession) {
        return getErrorResponse(clientSession, JBossSAMLURIConstants.STATUS_REQUEST_DENIED.get());
    }

    @Override
    public void close() {

    }
}
