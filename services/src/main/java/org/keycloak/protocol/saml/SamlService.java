/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.saml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.PublicKey;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.NameIDPolicyType;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.protocol.saml.profile.ecp.SamlEcpProfileService;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.util.CacheControlUtil;

/**
 * Resource class for the oauth/openid connect token service
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlService extends AuthorizationEndpointBase {

    protected static final Logger logger = Logger.getLogger(SamlService.class);

    public SamlService(RealmModel realm, EventBuilder event) {
        super(realm, event);
    }

    public abstract class BindingProtocol {

        // this is to support back button on browser
        // if true, we redirect to authenticate URL otherwise back button behavior has bad side effects
        // and we want to turn it off.
        protected boolean redirectToAuthentication;

        protected Response basicChecks(String samlRequest, String samlResponse) {
            if (!checkSsl()) {
                event.event(EventType.LOGIN);
                event.error(Errors.SSL_REQUIRED);
                return ErrorPage.error(session, Messages.HTTPS_REQUIRED);
            }
            if (!realm.isEnabled()) {
                event.event(EventType.LOGIN_ERROR);
                event.error(Errors.REALM_DISABLED);
                return ErrorPage.error(session, Messages.REALM_NOT_ENABLED);
            }

            if (samlRequest == null && samlResponse == null) {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_TOKEN);
                return ErrorPage.error(session, Messages.INVALID_REQUEST);

            }
            return null;
        }

        protected Response handleSamlResponse(String samlResponse, String relayState) {
            event.event(EventType.LOGOUT);
            SAMLDocumentHolder holder = extractResponseDocument(samlResponse);
            StatusResponseType statusResponse = (StatusResponseType) holder.getSamlObject();
            // validate destination
            if (statusResponse.getDestination() != null && !uriInfo.getAbsolutePath().toString().equals(statusResponse.getDestination())) {
                event.detail(Details.REASON, "invalid_destination");
                event.error(Errors.INVALID_SAML_LOGOUT_RESPONSE);
                return ErrorPage.error(session, Messages.INVALID_REQUEST);
            }

            AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm, false);
            if (authResult == null) {
                logger.warn("Unknown saml response.");
                event.event(EventType.LOGOUT);
                event.error(Errors.INVALID_TOKEN);
                return ErrorPage.error(session, Messages.INVALID_REQUEST);
            }
            // assume this is a logout response
            UserSessionModel userSession = authResult.getSession();
            if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
                logger.warn("Unknown saml response.");
                logger.warn("UserSession is not tagged as logging out.");
                event.event(EventType.LOGOUT);
                event.error(Errors.INVALID_SAML_LOGOUT_RESPONSE);
                return ErrorPage.error(session, Messages.INVALID_REQUEST);
            }
            logger.debug("logout response");
            Response response = authManager.browserLogout(session, realm, userSession, uriInfo, clientConnection, headers);
            event.success();
            return response;
        }

        protected Response handleSamlRequest(String samlRequest, String relayState) {
            SAMLDocumentHolder documentHolder = extractRequestDocument(samlRequest);
            if (documentHolder == null) {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_TOKEN);
                return ErrorPage.error(session, Messages.INVALID_REQUEST);
            }

            SAML2Object samlObject = documentHolder.getSamlObject();

            RequestAbstractType requestAbstractType = (RequestAbstractType) samlObject;
            String issuer = requestAbstractType.getIssuer().getValue();
            ClientModel client = realm.getClientByClientId(issuer);

            if (client == null) {
                event.event(EventType.LOGIN);
                event.client(issuer);
                event.error(Errors.CLIENT_NOT_FOUND);
                return ErrorPage.error(session, Messages.UNKNOWN_LOGIN_REQUESTER);
            }

            if (!client.isEnabled()) {
                event.event(EventType.LOGIN);
                event.error(Errors.CLIENT_DISABLED);
                return ErrorPage.error(session, Messages.LOGIN_REQUESTER_NOT_ENABLED);
            }
            if (client.isBearerOnly()) {
                event.event(EventType.LOGIN);
                event.error(Errors.NOT_ALLOWED);
                return ErrorPage.error(session, Messages.BEARER_ONLY);
            }
            if (!client.isStandardFlowEnabled()) {
                event.event(EventType.LOGIN);
                event.error(Errors.NOT_ALLOWED);
                return ErrorPage.error(session, Messages.STANDARD_FLOW_DISABLED);
            }

            session.getContext().setClient(client);

            try {
                verifySignature(documentHolder, client);
            } catch (VerificationException e) {
                SamlService.logger.error("request validation failed", e);
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_SIGNATURE);
                return ErrorPage.error(session, Messages.INVALID_REQUESTER);
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
                return ErrorPage.error(session, Messages.INVALID_REQUEST);
            }
        }

        protected abstract void verifySignature(SAMLDocumentHolder documentHolder, ClientModel client) throws VerificationException;

        protected abstract SAMLDocumentHolder extractRequestDocument(String samlRequest);

        protected abstract SAMLDocumentHolder extractResponseDocument(String response);

        protected Response loginRequest(String relayState, AuthnRequestType requestAbstractType, ClientModel client) {
            SamlClient samlClient = new SamlClient(client);
            // validate destination
            if (requestAbstractType.getDestination() != null && !uriInfo.getAbsolutePath().equals(requestAbstractType.getDestination())) {
                event.detail(Details.REASON, "invalid_destination");
                event.error(Errors.INVALID_SAML_AUTHN_REQUEST);
                return ErrorPage.error(session, Messages.INVALID_REQUEST);
            }
            String bindingType = getBindingType(requestAbstractType);
            if (samlClient.forcePostBinding())
                bindingType = SamlProtocol.SAML_POST_BINDING;
            String redirect = null;
            URI redirectUri = requestAbstractType.getAssertionConsumerServiceURL();
            if (redirectUri != null && !"null".equals(redirectUri)) { // "null" is for testing purposes
                redirect = RedirectUtils.verifyRedirectUri(uriInfo, redirectUri.toString(), realm, client);
            } else {
                if (bindingType.equals(SamlProtocol.SAML_POST_BINDING)) {
                    redirect = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE);
                } else {
                    redirect = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE);
                }
                if (redirect == null) {
                    redirect = client.getManagementUrl();
                }

            }

            if (redirect == null) {
                event.error(Errors.INVALID_REDIRECT_URI);
                return ErrorPage.error(session, Messages.INVALID_REDIRECT_URI);
            }

            ClientSessionModel clientSession = session.sessions().createClientSession(realm, client);
            clientSession.setAuthMethod(SamlProtocol.LOGIN_PROTOCOL);
            clientSession.setRedirectUri(redirect);
            clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
            clientSession.setNote(SamlProtocol.SAML_BINDING, bindingType);
            clientSession.setNote(GeneralConstants.RELAY_STATE, relayState);
            clientSession.setNote(SamlProtocol.SAML_REQUEST_ID, requestAbstractType.getID());

            // Handle NameIDPolicy from SP
            NameIDPolicyType nameIdPolicy = requestAbstractType.getNameIDPolicy();
            if (nameIdPolicy != null && !samlClient.forceNameIDFormat()) {
                String nameIdFormat = nameIdPolicy.getFormat().toString();
                // TODO: Handle AllowCreate too, relevant for persistent NameID.
                if (isSupportedNameIdFormat(nameIdFormat)) {
                    clientSession.setNote(GeneralConstants.NAMEID_FORMAT, nameIdFormat);
                } else {
                    event.detail(Details.REASON, "unsupported_nameid_format");
                    event.error(Errors.INVALID_SAML_AUTHN_REQUEST);
                    return ErrorPage.error(session, Messages.UNSUPPORTED_NAME_ID_FORMAT);
                }
            }

            return newBrowserAuthentication(clientSession, requestAbstractType.isIsPassive(), redirectToAuthentication);
        }

        protected String getBindingType(AuthnRequestType requestAbstractType) {
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
            if (nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get()) || nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT.get()) || nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get())
                    || nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get())) {
                return true;
            }
            return false;
        }

        protected abstract String getBindingType();

        protected Response logoutRequest(LogoutRequestType logoutRequest, ClientModel client, String relayState) {
            SamlClient samlClient = new SamlClient(client);
            // validate destination
            if (logoutRequest.getDestination() != null && !uriInfo.getAbsolutePath().equals(logoutRequest.getDestination())) {
                event.detail(Details.REASON, "invalid_destination");
                event.error(Errors.INVALID_SAML_LOGOUT_REQUEST);
                return ErrorPage.error(session, Messages.INVALID_REQUEST);
            }

            // authenticate identity cookie, but ignore an access token timeout as we're logging out anyways.
            AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm, false);
            if (authResult != null) {
                String logoutBinding = getBindingType();
                if ("true".equals(samlClient.forcePostBinding()))
                    logoutBinding = SamlProtocol.SAML_POST_BINDING;
                String bindingUri = SamlProtocol.getLogoutServiceUrl(uriInfo, client, logoutBinding);
                UserSessionModel userSession = authResult.getSession();
                userSession.setNote(SamlProtocol.SAML_LOGOUT_BINDING_URI, bindingUri);
                if (samlClient.requiresRealmSignature()) {
                    userSession.setNote(SamlProtocol.SAML_LOGOUT_SIGNATURE_ALGORITHM, samlClient.getSignatureAlgorithm().toString());

                }
                if (relayState != null)
                    userSession.setNote(SamlProtocol.SAML_LOGOUT_RELAY_STATE, relayState);
                userSession.setNote(SamlProtocol.SAML_LOGOUT_REQUEST_ID, logoutRequest.getID());
                userSession.setNote(SamlProtocol.SAML_LOGOUT_BINDING, logoutBinding);
                userSession.setNote(SamlProtocol.SAML_LOGOUT_CANONICALIZATION, samlClient.getCanonicalizationMethod());
                userSession.setNote(AuthenticationManager.KEYCLOAK_LOGOUT_PROTOCOL, SamlProtocol.LOGIN_PROTOCOL);
                // remove client from logout requests
                for (ClientSessionModel clientSession : userSession.getClientSessions()) {
                    if (clientSession.getClient().getId().equals(client.getId())) {
                        clientSession.setAction(ClientSessionModel.Action.LOGGED_OUT.name());
                    }
                }
                logger.debug("browser Logout");
                return authManager.browserLogout(session, realm, userSession, uriInfo, clientConnection, headers);
            } else if (logoutRequest.getSessionIndex() != null) {
                for (String sessionIndex : logoutRequest.getSessionIndex()) {
                    ClientSessionModel clientSession = session.sessions().getClientSession(realm, sessionIndex);
                    if (clientSession == null)
                        continue;
                    UserSessionModel userSession = clientSession.getUserSession();
                    if (clientSession.getClient().getClientId().equals(client.getClientId())) {
                        // remove requesting client from logout
                        clientSession.setAction(ClientSessionModel.Action.LOGGED_OUT.name());

                        // Remove also other clientSessions of this client as there could be more in this UserSession
                        if (userSession != null) {
                            for (ClientSessionModel clientSession2 : userSession.getClientSessions()) {
                                if (clientSession2.getClient().getId().equals(client.getId())) {
                                    clientSession2.setAction(ClientSessionModel.Action.LOGGED_OUT.name());
                                }
                            }
                        }
                    }

                    try {
                        authManager.backchannelLogout(session, realm, userSession, uriInfo, clientConnection, headers, true);
                    } catch (Exception e) {
                        logger.warn("Failure with backchannel logout", e);
                    }

                }

            }

            // default

            String logoutBinding = getBindingType();
            String logoutBindingUri = SamlProtocol.getLogoutServiceUrl(uriInfo, client, logoutBinding);
            String logoutRelayState = relayState;
            SAML2LogoutResponseBuilder builder = new SAML2LogoutResponseBuilder();
            builder.logoutRequestID(logoutRequest.getID());
            builder.destination(logoutBindingUri);
            builder.issuer(RealmsResource.realmBaseUrl(uriInfo).build(realm.getName()).toString());
            JaxrsSAML2BindingBuilder binding = new JaxrsSAML2BindingBuilder().relayState(logoutRelayState);
            if (samlClient.requiresRealmSignature()) {
                SignatureAlgorithm algorithm = samlClient.getSignatureAlgorithm();
                binding.signatureAlgorithm(algorithm).signWith(realm.getPrivateKey(), realm.getPublicKey(), realm.getCertificate()).signDocument();

            }
            try {
                if (SamlProtocol.SAML_POST_BINDING.equals(logoutBinding)) {
                    return binding.postBinding(builder.buildDocument()).response(logoutBindingUri);
                } else {
                    return binding.redirectBinding(builder.buildDocument()).response(logoutBindingUri);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
        protected SAMLDocumentHolder extractRequestDocument(String samlRequest) {
            return SAMLRequestParser.parseRequestPostBinding(samlRequest);
        }

        @Override
        protected SAMLDocumentHolder extractResponseDocument(String response) {
            return SAMLRequestParser.parseResponsePostBinding(response);
        }

        @Override
        protected String getBindingType() {
            return SamlProtocol.SAML_POST_BINDING;
        }

        public Response execute(String samlRequest, String samlResponse, String relayState) {
            Response response = basicChecks(samlRequest, samlResponse);
            if (response != null)
                return response;
            if (samlRequest != null)
                return handleSamlRequest(samlRequest, relayState);
            else
                return handleSamlResponse(samlResponse, relayState);
        }

    }

    protected class RedirectBindingProtocol extends BindingProtocol {

        @Override
        protected void verifySignature(SAMLDocumentHolder documentHolder, ClientModel client) throws VerificationException {
            SamlClient samlClient = new SamlClient(client);
            if (!samlClient.requiresClientSignature()) {
                return;
            }
            PublicKey publicKey = SamlProtocolUtils.getSignatureValidationKey(client);
            SamlProtocolUtils.verifyRedirectSignature(publicKey, uriInfo, GeneralConstants.SAML_REQUEST_KEY);
        }

        @Override
        protected SAMLDocumentHolder extractRequestDocument(String samlRequest) {
            return SAMLRequestParser.parseRequestRedirectBinding(samlRequest);
        }

        @Override
        protected SAMLDocumentHolder extractResponseDocument(String response) {
            return SAMLRequestParser.parseResponseRedirectBinding(response);
        }

        @Override
        protected String getBindingType() {
            return SamlProtocol.SAML_REDIRECT_BINDING;
        }

        public Response execute(String samlRequest, String samlResponse, String relayState) {
            Response response = basicChecks(samlRequest, samlResponse);
            if (response != null)
                return response;
            if (samlRequest != null)
                return handleSamlRequest(samlRequest, relayState);
            else
                return handleSamlResponse(samlResponse, relayState);
        }

    }

    protected Response newBrowserAuthentication(ClientSessionModel clientSession, boolean isPassive, boolean redirectToAuthentication) {
        SamlProtocol samlProtocol = new SamlProtocol().setEventBuilder(event).setHttpHeaders(headers).setRealm(realm).setSession(session).setUriInfo(uriInfo);
        return newBrowserAuthentication(clientSession, isPassive, redirectToAuthentication, samlProtocol);
    }

    protected Response newBrowserAuthentication(ClientSessionModel clientSession, boolean isPassive, boolean redirectToAuthentication, SamlProtocol samlProtocol) {
        return handleBrowserAuthenticationRequest(clientSession, samlProtocol, isPassive, redirectToAuthentication);
    }

    /**
     */
    @GET
    public Response redirectBinding(@QueryParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest, @QueryParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse, @QueryParam(GeneralConstants.RELAY_STATE) String relayState) {
        logger.debug("SAML GET");
        CacheControlUtil.noBackButtonCacheControlHeader();
        return new RedirectBindingProtocol().execute(samlRequest, samlResponse, relayState);
    }

    /**
     */
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postBinding(@FormParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest, @FormParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse, @FormParam(GeneralConstants.RELAY_STATE) String relayState) {
        logger.debug("SAML POST");
        PostBindingProtocol postBindingProtocol = new PostBindingProtocol();
        // this is to support back button on browser
        // if true, we redirect to authenticate URL otherwise back button behavior has bad side effects
        // and we want to turn it off.
        postBindingProtocol.redirectToAuthentication = true;
        return postBindingProtocol.execute(samlRequest, samlResponse, relayState);
    }

    @GET
    @Path("descriptor")
    @Produces(MediaType.APPLICATION_XML)
    @NoCache
    public String getDescriptor() throws Exception {
        return getIDPMetadataDescriptor(uriInfo, realm);

    }

    public static String getIDPMetadataDescriptor(UriInfo uriInfo, RealmModel realm) throws IOException {
        InputStream is = SamlService.class.getResourceAsStream("/idp-metadata-template.xml");
        String template = StreamUtil.readString(is);
        template = template.replace("${idp.entityID}", RealmsResource.realmBaseUrl(uriInfo).build(realm.getName()).toString());
        template = template.replace("${idp.sso.HTTP-POST}", RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString());
        template = template.replace("${idp.sso.HTTP-Redirect}", RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString());
        template = template.replace("${idp.sls.HTTP-POST}", RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString());
        template = template.replace("${idp.signing.certificate}", realm.getCertificatePem());
        return template;
    }

    @GET
    @Path("clients/{client}")
    @Produces(MediaType.TEXT_HTML)
    public Response idpInitiatedSSO(@PathParam("client") String clientUrlName, @QueryParam("RelayState") String relayState) {
        event.event(EventType.LOGIN);
        CacheControlUtil.noBackButtonCacheControlHeader();
        ClientModel client = null;
        for (ClientModel c : realm.getClients()) {
            String urlName = c.getAttribute(SamlProtocol.SAML_IDP_INITIATED_SSO_URL_NAME);
            if (urlName == null)
                continue;
            if (urlName.equals(clientUrlName)) {
                client = c;
                break;
            }
        }
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            return ErrorPage.error(session, Messages.CLIENT_NOT_FOUND);
        }
        if (client.getManagementUrl() == null && client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE) == null && client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE) == null) {
            logger.error("SAML assertion consumer url not set up");
            event.error(Errors.INVALID_REDIRECT_URI);
            return ErrorPage.error(session, Messages.INVALID_REDIRECT_URI);
        }

        String bindingType = SamlProtocol.SAML_POST_BINDING;
        if (client.getManagementUrl() == null && client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE) == null && client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE) != null) {
            bindingType = SamlProtocol.SAML_REDIRECT_BINDING;
        }

        String redirect = null;
        if (bindingType.equals(SamlProtocol.SAML_REDIRECT_BINDING)) {
            redirect = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE);
        } else {
            redirect = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE);
        }
        if (redirect == null) {
            redirect = client.getManagementUrl();
        }

        ClientSessionModel clientSession = session.sessions().createClientSession(realm, client);
        clientSession.setAuthMethod(SamlProtocol.LOGIN_PROTOCOL);
        clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
        clientSession.setNote(SamlProtocol.SAML_BINDING, SamlProtocol.SAML_POST_BINDING);
        clientSession.setNote(SamlProtocol.SAML_IDP_INITIATED_LOGIN, "true");
        clientSession.setRedirectUri(redirect);

        if (relayState == null) {
            relayState = client.getAttribute(SamlProtocol.SAML_IDP_INITIATED_SSO_RELAY_STATE);
        }
        if (relayState != null && !relayState.trim().equals("")) {
            clientSession.setNote(GeneralConstants.RELAY_STATE, relayState);
        }

        return newBrowserAuthentication(clientSession, false, false);

    }

    @POST
    @NoCache
    @Consumes({"application/soap+xml",MediaType.TEXT_XML})
    public Response soapBinding(InputStream inputStream) {
        SamlEcpProfileService bindingService = new SamlEcpProfileService(realm, event);

        ResteasyProviderFactory.getInstance().injectProperties(bindingService);

        return bindingService.authenticate(inputStream);
    }

}
