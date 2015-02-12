package org.keycloak.protocol.saml;

import org.jboss.logging.Logger;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.ClientConnection;
import org.keycloak.VerificationException;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OpenIDConnectService;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.util.StreamUtil;
import org.picketlink.common.constants.GeneralConstants;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.identity.federation.core.saml.v2.common.SAMLDocumentHolder;
import org.picketlink.identity.federation.saml.v2.SAML2Object;
import org.picketlink.identity.federation.saml.v2.protocol.AuthnRequestType;
import org.picketlink.identity.federation.saml.v2.protocol.LogoutRequestType;
import org.picketlink.identity.federation.saml.v2.protocol.NameIDPolicyType;
import org.picketlink.identity.federation.saml.v2.protocol.RequestAbstractType;
import org.picketlink.identity.federation.web.util.RedirectBindingUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.io.InputStream;
import java.net.URI;
import java.security.PublicKey;
import java.security.Signature;

/**
 * Resource class for the oauth/openid connect token service
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlService {

    protected static final Logger logger = Logger.getLogger(SamlService.class);

    protected RealmModel realm;
    private EventBuilder event;
    protected AuthenticationManager authManager;

    @Context
    protected Providers providers;
    @Context
    protected SecurityContext securityContext;
    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpHeaders headers;
    @Context
    protected HttpRequest request;
    @Context
    protected HttpResponse response;
    @Context
    protected KeycloakSession session;
    @Context
    protected ClientConnection clientConnection;

    /*
    @Context
    protected ResourceContext resourceContext;
    */

    public SamlService(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        this.realm = realm;
        this.event = event;
        this.authManager = authManager;
    }

    public abstract class BindingProtocol {
        protected Response basicChecks(String samlRequest, String samlResponse) {
            if (!checkSsl()) {
                event.event(EventType.LOGIN);
                event.error(Errors.SSL_REQUIRED);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "HTTPS required");
            }
            if (!realm.isEnabled()) {
                event.event(EventType.LOGIN_ERROR);
                event.error(Errors.REALM_DISABLED);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Realm not enabled");
            }

            if (samlRequest == null && samlResponse == null) {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_TOKEN);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid Request");

            }
            return null;
        }

        protected Response handleSamlResponse(String samlResponse, String relayState) {
            AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm, uriInfo, clientConnection, headers, false);
            if (authResult == null) {
                logger.warn("Unknown saml response.");
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_TOKEN);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid Request");
            }
            // assume this is a logout response
            UserSessionModel userSession = authResult.getSession();
            if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
                logger.warn("Unknown saml response.");
                logger.warn("UserSession is not tagged as logging out.");
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_TOKEN);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid Request");
            }
            logger.debug("logout response");
            return authManager.browserLogout(session, realm, userSession, uriInfo, clientConnection);
        }

        protected Response handleSamlRequest(String samlRequest, String relayState) {
            SAMLDocumentHolder documentHolder = extractDocument(samlRequest);
            if (documentHolder == null) {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_TOKEN);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid Request");
            }

            SAML2Object samlObject = documentHolder.getSamlObject();

            RequestAbstractType requestAbstractType = (RequestAbstractType)samlObject;
            String issuer = requestAbstractType.getIssuer().getValue();
            ClientModel client = realm.findClient(issuer);

            if (client == null) {
                event.event(EventType.LOGIN);
                event.error(Errors.CLIENT_NOT_FOUND);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Unknown login requester.");
            }

            if (!client.isEnabled()) {
                event.event(EventType.LOGIN);
                event.error(Errors.CLIENT_DISABLED);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Login requester not enabled.");
            }
            if ((client instanceof ApplicationModel) && ((ApplicationModel)client).isBearerOnly()) {
                event.event(EventType.LOGIN);
                event.error(Errors.NOT_ALLOWED);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Bearer-only applications are not allowed to initiate browser login");
            }
            if (client.isDirectGrantsOnly()) {
                event.event(EventType.LOGIN);
                event.error(Errors.NOT_ALLOWED);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "direct-grants-only clients are not allowed to initiate browser login");
            }

            try {
                verifySignature(documentHolder, client);
            } catch (VerificationException e) {
                SamlService.logger.error("request validation failed", e);
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_SIGNATURE);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid requester.");
            }
            logger.debug("verified request");
            if (samlObject instanceof AuthnRequestType) {
                logger.debug("** login request");
                event.event(EventType.LOGIN);
                // Get the SAML Request Message
                AuthnRequestType authn = (AuthnRequestType) samlObject;
                return loginRequest(relayState, authn, client);
            } else if (samlObject instanceof LogoutRequestType) {
                logger.debug("** logout request");
                event.event(EventType.LOGOUT);
                LogoutRequestType logout = (LogoutRequestType) samlObject;
                return logoutRequest(logout, client, relayState);

            } else {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_TOKEN);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid Request");
            }
        }

        protected abstract void verifySignature(SAMLDocumentHolder documentHolder, ClientModel client) throws VerificationException;

        protected abstract SAMLDocumentHolder extractDocument(String samlRequest);

        protected Response loginRequest(String relayState, AuthnRequestType requestAbstractType, ClientModel client) {

            String bindingType = getBindingType(requestAbstractType);
            if ("true".equals(client.getAttribute(SamlProtocol.SAML_FORCE_POST_BINDING))) bindingType = SamlProtocol.SAML_POST_BINDING;
            String redirect = null;
            URI redirectUri = requestAbstractType.getAssertionConsumerServiceURL();
            if (redirectUri != null && !"null".equals(redirectUri)) {  // "null" is for testing purposes
                redirect = OpenIDConnectService.verifyRedirectUri(uriInfo, redirectUri.toString(), realm, client);
            } else {
                if (bindingType.equals(SamlProtocol.SAML_POST_BINDING)) {
                    redirect = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE);
                } else {
                    redirect = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE);
                }
                if (redirect == null && client instanceof ApplicationModel) {
                    redirect = ((ApplicationModel)client).getManagementUrl();
                }

            }

            if (redirect == null) {
                event.error(Errors.INVALID_REDIRECT_URI);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid redirect_uri.");
            }


            ClientSessionModel clientSession = session.sessions().createClientSession(realm, client);
            clientSession.setAuthMethod(SamlProtocol.LOGIN_PROTOCOL);
            clientSession.setRedirectUri(redirect);
            clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE);
            clientSession.setNote(ClientSessionCode.ACTION_KEY, KeycloakModelUtils.generateCodeSecret());
            clientSession.setNote(SamlProtocol.SAML_BINDING, bindingType);
            clientSession.setNote(GeneralConstants.RELAY_STATE, relayState);
            clientSession.setNote(SamlProtocol.SAML_REQUEST_ID, requestAbstractType.getID());

            // Handle NameIDPolicy from SP
            NameIDPolicyType nameIdPolicy = requestAbstractType.getNameIDPolicy();
            if(nameIdPolicy != null && !SamlProtocol.forceNameIdFormat(client)) {
                String nameIdFormat = nameIdPolicy.getFormat().toString();
                // TODO: Handle AllowCreate too, relevant for persistent NameID.
                if(isSupportedNameIdFormat(nameIdFormat)) {
                    clientSession.setNote(GeneralConstants.NAMEID_FORMAT, nameIdFormat);
                } else {
                    event.error(Errors.INVALID_TOKEN);
                    return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Unsupported NameIDFormat.");
                }
            }

            Response response = authManager.checkNonFormAuthentication(session, clientSession, realm, uriInfo, request, clientConnection, headers, event);
            if (response != null) return response;

            LoginFormsProvider forms = Flows.forms(session, realm, clientSession.getClient(), uriInfo)
                    .setClientSessionCode(new ClientSessionCode(realm, clientSession).getCode());

            String rememberMeUsername = AuthenticationManager.getRememberMeUsername(realm, headers);

            if (rememberMeUsername != null) {
                MultivaluedMap<String, String> formData = new MultivaluedMapImpl<String, String>();
                formData.add(AuthenticationManager.FORM_USERNAME, rememberMeUsername);
                formData.add("rememberMe", "on");

                forms.setFormData(formData);
            }

            return forms.createLogin();
        }

        private String getBindingType(AuthnRequestType requestAbstractType) {
            URI requestedProtocolBinding = requestAbstractType.getProtocolBinding();

            if (requestedProtocolBinding != null) {
                if (JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get().equals(requestedProtocolBinding.toString())) {
                    return SamlProtocol.SAML_POST_BINDING;
                } else {
                    return SamlProtocol.SAML_REDIRECT_BINDING;
                }
            }

            return getBindingType();
        }

        private boolean isSupportedNameIdFormat(String nameIdFormat) {
            if (nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get()) ||
                    nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT.get()) ||
                    nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get()) ||
                    nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get())) {
                return true;
            }
            return false;
        }

        protected abstract String getBindingType();

        protected Response logoutRequest(LogoutRequestType logoutRequest, ClientModel client, String relayState) {
            // authenticate identity cookie, but ignore an access token timeout as we're logging out anyways.


            AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm, uriInfo, clientConnection, headers, false);
            if (authResult != null) {
                String logoutBinding = getBindingType();
                if ("true".equals(client.getAttribute(SamlProtocol.SAML_FORCE_POST_BINDING))) logoutBinding = SamlProtocol.SAML_POST_BINDING;
                String bindingUri = SamlProtocol.getLogoutServiceUrl(uriInfo, client, logoutBinding);
                UserSessionModel userSession = authResult.getSession();
                userSession.setNote(SamlProtocol.SAML_LOGOUT_BINDING_URI, bindingUri);
                if (SamlProtocol.requiresRealmSignature(client)) {
                    userSession.setNote(SamlProtocol.SAML_LOGOUT_SIGNATURE_ALGORITHM, SamlProtocol.getSignatureAlgorithm(client).toString());

                }
                if (relayState != null) userSession.setNote(SamlProtocol.SAML_LOGOUT_RELAY_STATE, relayState);
                userSession.setNote(SamlProtocol.SAML_LOGOUT_REQUEST_ID, logoutRequest.getID());
                userSession.setNote(SamlProtocol.SAML_LOGOUT_BINDING, logoutBinding);
                userSession.setNote(SamlProtocol.SAML_LOGOUT_ISSUER, logoutRequest.getIssuer().getValue());
                userSession.setNote(AuthenticationManager.KEYCLOAK_LOGOUT_PROTOCOL, SamlProtocol.LOGIN_PROTOCOL);
                // remove client from logout requests
                for (ClientSessionModel clientSession : userSession.getClientSessions()) {
                    if (clientSession.getClient().getId().equals(client.getId())) {
                        clientSession.setAction(ClientSessionModel.Action.LOGGED_OUT);
                    }
                }
                logger.debug("browser Logout");
                return authManager.browserLogout(session, realm, userSession, uriInfo, clientConnection);
            }


            String redirectUri = null;

            if (client instanceof ApplicationModel) {
                redirectUri = ((ApplicationModel)client).getBaseUrl();
            }

            if (redirectUri != null) {
                redirectUri = OpenIDConnectService.verifyRedirectUri(uriInfo, redirectUri, realm, client);
                if (redirectUri == null) {
                    return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid redirect uri.");
                }
            }
            if (redirectUri != null) {
                return Response.status(302).location(UriBuilder.fromUri(redirectUri).build()).build();
            } else {
                return Response.ok().build();
            }

        }

        private Response logout(UserSessionModel userSession) {
            Response response = authManager.browserLogout(session, realm, userSession, uriInfo, clientConnection);
            if (response == null) event.user(userSession.getUser()).session(userSession).success();
            return response;
        }

        private boolean checkSsl() {
            if (uriInfo.getBaseUri().getScheme().equals("https")) {
                return true;
            } else {
                return !realm.getSslRequired().isRequired(clientConnection);
            }
        }
    }


    protected class PostBindingProtocol extends BindingProtocol {


        @Override
        protected void verifySignature(SAMLDocumentHolder documentHolder, ClientModel client) throws VerificationException {
            SamlProtocolUtils.verifyDocumentSignature(client, documentHolder.getSamlDocument());
        }

        @Override
        protected SAMLDocumentHolder extractDocument(String samlRequest) {
            return SAMLRequestParser.parsePostBinding(samlRequest);
        }

        @Override
        protected String getBindingType() {
            return SamlProtocol.SAML_POST_BINDING;
        }


        public Response execute(String samlRequest, String samlResponse, String relayState) {
            Response response = basicChecks(samlRequest, samlResponse);
            if (response != null) return response;
            if (samlRequest != null) return handleSamlRequest(samlRequest, relayState);
            else return handleSamlResponse(samlResponse, relayState);
        }

    }

    protected class RedirectBindingProtocol extends BindingProtocol {

        @Override
        protected void verifySignature(SAMLDocumentHolder documentHolder, ClientModel client) throws VerificationException {
            if (!"true".equals(client.getAttribute("saml.client.signature"))) {
                return;
            }
            MultivaluedMap<String, String> encodedParams = uriInfo.getQueryParameters(false);
            String request = encodedParams.getFirst(GeneralConstants.SAML_REQUEST_KEY);
            String algorithm = encodedParams.getFirst(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY);
            String signature = encodedParams.getFirst(GeneralConstants.SAML_SIGNATURE_REQUEST_KEY);

            if (request == null) throw new VerificationException("SAMLRequest as null");
            if (algorithm == null) throw new VerificationException("SigAlg as null");
            if (signature == null) throw new VerificationException("Signature as null");

            // Shibboleth doesn't sign the document for redirect binding.
            // todo maybe a flag?
            // SamlProtocolUtils.verifyDocumentSignature(client, documentHolder.getSamlDocument());

            PublicKey publicKey = SamlProtocolUtils.getSignatureValidationKey(client);


            UriBuilder builder = UriBuilder.fromPath("/")
                    .queryParam(GeneralConstants.SAML_REQUEST_KEY, request);
            if (encodedParams.containsKey(GeneralConstants.RELAY_STATE)) {
                builder.queryParam(GeneralConstants.RELAY_STATE, encodedParams.getFirst(GeneralConstants.RELAY_STATE));
            }
            builder.queryParam(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY, algorithm);
            String rawQuery = builder.build().getRawQuery();

            try {
                byte[] decodedSignature = RedirectBindingUtil.urlBase64Decode(signature);

                SignatureAlgorithm signatureAlgorithm = SamlProtocol.getSignatureAlgorithm(client);
                Signature validator = signatureAlgorithm.createSignature(); // todo plugin signature alg
                validator.initVerify(publicKey);
                validator.update(rawQuery.getBytes("UTF-8"));
                if (!validator.verify(decodedSignature)) {
                    throw new VerificationException("Invalid query param signature");
                }
            } catch (Exception e) {
                throw new VerificationException(e);
            }


        }

        @Override
        protected SAMLDocumentHolder extractDocument(String samlRequest) {
            return SAMLRequestParser.parseRedirectBinding(samlRequest);
        }

        @Override
        protected String getBindingType() {
            return SamlProtocol.SAML_REDIRECT_BINDING;
        }


        public Response execute(String samlRequest, String samlResponse, String relayState) {
            Response response = basicChecks(samlRequest, samlResponse);
            if (response != null) return response;
            if (samlRequest != null) return handleSamlRequest(samlRequest, relayState);
            else return handleSamlResponse(samlResponse, relayState);
        }

    }


    /**
     */
    @GET
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response redirectBinding(@QueryParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                    @QueryParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                    @QueryParam(GeneralConstants.RELAY_STATE) String relayState)  {
        logger.debug("SAML GET");
        return new RedirectBindingProtocol().execute(samlRequest, samlResponse, relayState);
    }


    /**
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postBinding(@FormParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                @FormParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                @FormParam(GeneralConstants.RELAY_STATE) String relayState) {
        logger.debug("SAML POST");
        return new PostBindingProtocol().execute(samlRequest, samlResponse, relayState);
    }

    @GET
    @Path("descriptor")
    @Produces(MediaType.APPLICATION_XML)
    public String getDescriptor() throws Exception {
        InputStream is = getClass().getResourceAsStream("/idp-metadata-template.xml");
        String template = StreamUtil.readString(is);
        template = template.replace("${idp.entityID}", RealmsResource.realmBaseUrl(uriInfo).build(realm.getName()).toString());
        template = template.replace("${idp.sso.HTTP-POST}", RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString());
        template = template.replace("${idp.sso.HTTP-Redirect}", RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString());
        template = template.replace("${idp.sls.HTTP-POST}", RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString());
        template = template.replace("${idp.signing.certificate}", realm.getCertificatePem());
        return template;

    }

}
