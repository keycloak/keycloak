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

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.BaseIDAbstractType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.NameIDPolicyType;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;
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
import org.keycloak.utils.MediaType;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.PublicKey;
import java.util.Iterator;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.SPMetadataDescriptor;
import org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.sessions.AuthenticationSessionModel;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Resource class for the saml connect token service
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlService extends AuthorizationEndpointBase {

    protected static final Logger logger = Logger.getLogger(SamlService.class);

    private final DestinationValidator destinationValidator;

    public SamlService(RealmModel realm, EventBuilder event, DestinationValidator destinationValidator) {
        super(realm, event);
        this.destinationValidator = destinationValidator;
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
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.HTTPS_REQUIRED);
            }
            if (!realm.isEnabled()) {
                event.event(EventType.LOGIN_ERROR);
                event.error(Errors.REALM_DISABLED);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.REALM_NOT_ENABLED);
            }

            if (samlRequest == null && samlResponse == null) {
                event.event(EventType.LOGIN);
                event.error(Errors.SAML_TOKEN_NOT_FOUND);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);

            }
            return null;
        }

        protected Response handleSamlResponse(String samlResponse, String relayState) {
            event.event(EventType.LOGOUT);
            SAMLDocumentHolder holder = extractResponseDocument(samlResponse);

            if (! (holder.getSamlObject() instanceof StatusResponseType)) {
                event.detail(Details.REASON, "invalid_saml_response");
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }

            StatusResponseType statusResponse = (StatusResponseType) holder.getSamlObject();
            // validate destination
            if (! destinationValidator.validate(session.getContext().getUri().getAbsolutePath(), statusResponse.getDestination())) {
                event.detail(Details.REASON, "invalid_destination");
                event.error(Errors.INVALID_SAML_LOGOUT_RESPONSE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }

            AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm, false);
            if (authResult == null) {
                logger.warn("Unknown saml response.");
                event.event(EventType.LOGOUT);
                event.error(Errors.INVALID_TOKEN);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }
            // assume this is a logout response
            UserSessionModel userSession = authResult.getSession();
            if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
                logger.warn("Unknown saml response.");
                logger.warn("UserSession is not tagged as logging out.");
                event.event(EventType.LOGOUT);
                event.error(Errors.INVALID_SAML_LOGOUT_RESPONSE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }
            String issuer = statusResponse.getIssuer().getValue();
            ClientModel client = realm.getClientByClientId(issuer);
            if (client == null) {
                event.event(EventType.LOGOUT);
                event.client(issuer);
                event.error(Errors.CLIENT_NOT_FOUND);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.CLIENT_NOT_FOUND);
            }

            if (!isClientProtocolCorrect(client)) {
                event.event(EventType.LOGOUT);
                event.error(Errors.INVALID_CLIENT);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, "Wrong client protocol.");
            }

            session.getContext().setClient(client);
            logger.debug("logout response");
            Response response = authManager.browserLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers, null);
            event.success();
            return response;
        }

        protected Response handleSamlRequest(String samlRequest, String relayState) {
            SAMLDocumentHolder documentHolder = extractRequestDocument(samlRequest);
            if (documentHolder == null) {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_TOKEN);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }

            SAML2Object samlObject = documentHolder.getSamlObject();

            if (! (samlObject instanceof RequestAbstractType)) {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_SAML_AUTHN_REQUEST);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }

            RequestAbstractType requestAbstractType = (RequestAbstractType) samlObject;
            final NameIDType issuerNameId = requestAbstractType.getIssuer();
            String issuer = requestAbstractType.getIssuer() == null ? null : issuerNameId.getValue();
            ClientModel client = realm.getClientByClientId(issuer);

            if (client == null) {
                event.event(EventType.LOGIN);
                event.client(issuer);
                event.error(Errors.CLIENT_NOT_FOUND);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.UNKNOWN_LOGIN_REQUESTER);
            }

            if (!client.isEnabled()) {
                event.event(EventType.LOGIN);
                event.error(Errors.CLIENT_DISABLED);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.LOGIN_REQUESTER_NOT_ENABLED);
            }
            if (client.isBearerOnly()) {
                event.event(EventType.LOGIN);
                event.error(Errors.NOT_ALLOWED);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.BEARER_ONLY);
            }
            if (!client.isStandardFlowEnabled()) {
                event.event(EventType.LOGIN);
                event.error(Errors.NOT_ALLOWED);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.STANDARD_FLOW_DISABLED);
            }
            if (!isClientProtocolCorrect(client)) {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_CLIENT);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, "Wrong client protocol.");
            }

            session.getContext().setClient(client);

            try {
                verifySignature(documentHolder, client);
            } catch (VerificationException e) {
                SamlService.logger.error("request validation failed", e);
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_SIGNATURE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
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
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }
        }

        protected abstract void verifySignature(SAMLDocumentHolder documentHolder, ClientModel client) throws VerificationException;

        protected abstract SAMLDocumentHolder extractRequestDocument(String samlRequest);

        protected abstract SAMLDocumentHolder extractResponseDocument(String response);

        protected Response loginRequest(String relayState, AuthnRequestType requestAbstractType, ClientModel client) {
            SamlClient samlClient = new SamlClient(client);
            // validate destination
            if (requestAbstractType.getDestination() == null && samlClient.requiresClientSignature()) {
                event.detail(Details.REASON, "invalid_destination");
                event.error(Errors.INVALID_SAML_AUTHN_REQUEST);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }
            if (! destinationValidator.validate(session.getContext().getUri().getAbsolutePath(), requestAbstractType.getDestination())) {
                event.detail(Details.REASON, "invalid_destination");
                event.error(Errors.INVALID_SAML_AUTHN_REQUEST);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }
            String bindingType = getBindingType(requestAbstractType);
            if (samlClient.forcePostBinding())
                bindingType = SamlProtocol.SAML_POST_BINDING;
            String redirect;
            URI redirectUri = requestAbstractType.getAssertionConsumerServiceURL();
            if (redirectUri != null && ! "null".equals(redirectUri.toString())) { // "null" is for testing purposes
                redirect = RedirectUtils.verifyRedirectUri(session, redirectUri.toString(), client);
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
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REDIRECT_URI);
            }

            AuthenticationSessionModel authSession = createAuthenticationSession(client, relayState);

            authSession.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
            authSession.setRedirectUri(redirect);
            authSession.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
            authSession.setClientNote(SamlProtocol.SAML_BINDING, bindingType);
            authSession.setClientNote(GeneralConstants.RELAY_STATE, relayState);
            authSession.setClientNote(SamlProtocol.SAML_REQUEST_ID, requestAbstractType.getID());

            // Handle NameIDPolicy from SP
            NameIDPolicyType nameIdPolicy = requestAbstractType.getNameIDPolicy();
            final URI nameIdFormatUri = nameIdPolicy == null ? null : nameIdPolicy.getFormat();
            if (nameIdFormatUri != null && ! samlClient.forceNameIDFormat()) {
                String nameIdFormat = nameIdFormatUri.toString();
                // TODO: Handle AllowCreate too, relevant for persistent NameID.
                if (isSupportedNameIdFormat(nameIdFormat)) {
                    authSession.setClientNote(GeneralConstants.NAMEID_FORMAT, nameIdFormat);
                } else {
                    event.detail(Details.REASON, "unsupported_nameid_format");
                    event.error(Errors.INVALID_SAML_AUTHN_REQUEST);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.UNSUPPORTED_NAME_ID_FORMAT);
                }
            }

            //Reading subject/nameID in the saml request
            SubjectType subject = requestAbstractType.getSubject();
            if (subject != null) {
                SubjectType.STSubType subType = subject.getSubType();
                if (subType != null) {
                    BaseIDAbstractType baseID = subject.getSubType().getBaseID();
                    if (baseID instanceof NameIDType) {
                        NameIDType nameID = (NameIDType) baseID;
                        authSession.setClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM, nameID.getValue());
                    }

                }
            }

            if (null != requestAbstractType.isForceAuthn()
                && requestAbstractType.isForceAuthn()) {
                authSession.setAuthNote(SamlProtocol.SAML_LOGIN_REQUEST_FORCEAUTHN, SamlProtocol.SAML_FORCEAUTHN_REQUIREMENT);
            }
            

            for(Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext();) {
                requestAbstractType = it.next().beforeProcessingLoginRequest(requestAbstractType, authSession);
            }

            //If unset we fall back to default "false"
            final boolean isPassive = (null != requestAbstractType.isIsPassive() && requestAbstractType.isIsPassive().booleanValue());
            return newBrowserAuthentication(authSession, isPassive, redirectToAuthentication);
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
            if (logoutRequest.getDestination() == null && samlClient.requiresClientSignature()) {
                event.detail(Details.REASON, "invalid_destination");
                event.error(Errors.INVALID_SAML_LOGOUT_REQUEST);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }
            if (! destinationValidator.validate(logoutRequest.getDestination(), session.getContext().getUri().getAbsolutePath())) {
                event.detail(Details.REASON, "invalid_destination");
                event.error(Errors.INVALID_SAML_LOGOUT_REQUEST);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }

            // authenticate identity cookie, but ignore an access token timeout as we're logging out anyways.
            AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm, false);
            if (authResult != null) {
                String logoutBinding = getBindingType();
                String postBindingUri = SamlProtocol.getLogoutServiceUrl(session, client, SamlProtocol.SAML_POST_BINDING);
                if (samlClient.forcePostBinding() && postBindingUri != null && ! postBindingUri.trim().isEmpty())
                    logoutBinding = SamlProtocol.SAML_POST_BINDING;
                boolean postBinding = Objects.equals(SamlProtocol.SAML_POST_BINDING, logoutBinding);

                String bindingUri = SamlProtocol.getLogoutServiceUrl(session, client, logoutBinding);
                UserSessionModel userSession = authResult.getSession();
                userSession.setNote(SamlProtocol.SAML_LOGOUT_BINDING_URI, bindingUri);
                if (samlClient.requiresRealmSignature()) {
                    userSession.setNote(SamlProtocol.SAML_LOGOUT_SIGNATURE_ALGORITHM, samlClient.getSignatureAlgorithm().toString());

                }
                if (relayState != null)
                    userSession.setNote(SamlProtocol.SAML_LOGOUT_RELAY_STATE, relayState);
                userSession.setNote(SamlProtocol.SAML_LOGOUT_REQUEST_ID, logoutRequest.getID());
                userSession.setNote(SamlProtocol.SAML_LOGOUT_BINDING, logoutBinding);
                userSession.setNote(SamlProtocol.SAML_LOGOUT_ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO, Boolean.toString((! postBinding) && samlClient.addExtensionsElementWithKeyInfo()));
                userSession.setNote(SamlProtocol.SAML_SERVER_SIGNATURE_KEYINFO_KEY_NAME_TRANSFORMER, samlClient.getXmlSigKeyInfoKeyNameTransformer().name());
                userSession.setNote(SamlProtocol.SAML_LOGOUT_CANONICALIZATION, samlClient.getCanonicalizationMethod());
                userSession.setNote(AuthenticationManager.KEYCLOAK_LOGOUT_PROTOCOL, SamlProtocol.LOGIN_PROTOCOL);
                // remove client from logout requests
                AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
                if (clientSession != null) {
                    clientSession.setAction(AuthenticationSessionModel.Action.LOGGED_OUT.name());
                }

                for(Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext();) {
                    logoutRequest = it.next().beforeProcessingLogoutRequest(logoutRequest, userSession, clientSession);
                }
                
                logger.debug("browser Logout");
                return authManager.browserLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers, null);
            } else if (logoutRequest.getSessionIndex() != null) {
                for (String sessionIndex : logoutRequest.getSessionIndex()) {

                    AuthenticatedClientSessionModel clientSession = SamlSessionUtils.getClientSession(session, realm, sessionIndex);
                    if (clientSession == null)
                        continue;
                    UserSessionModel userSession = clientSession.getUserSession();
                    if (clientSession.getClient().getClientId().equals(client.getClientId())) {
                        // remove requesting client from logout
                        clientSession.setAction(AuthenticationSessionModel.Action.LOGGED_OUT.name());
                    }

                    for(Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext();) {
                        logoutRequest = it.next().beforeProcessingLogoutRequest(logoutRequest, userSession, clientSession);
                    }

                    try {
                        authManager.backchannelLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers, true);
                    } catch (Exception e) {
                        logger.warn("Failure with backchannel logout", e);
                    }

                }

            }

            // default

            String logoutBinding = getBindingType();
            String logoutBindingUri = SamlProtocol.getLogoutServiceUrl(session, client, logoutBinding);
            String logoutRelayState = relayState;
            SAML2LogoutResponseBuilder builder = new SAML2LogoutResponseBuilder();
            builder.logoutRequestID(logoutRequest.getID());
            builder.destination(logoutBindingUri);
            builder.issuer(RealmsResource.realmBaseUrl(session.getContext().getUri()).build(realm.getName()).toString());
            JaxrsSAML2BindingBuilder binding = new JaxrsSAML2BindingBuilder(session).relayState(logoutRelayState);
            boolean postBinding = SamlProtocol.SAML_POST_BINDING.equals(logoutBinding);
            if (samlClient.requiresRealmSignature()) {
                SignatureAlgorithm algorithm = samlClient.getSignatureAlgorithm();
                KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);
                binding.signatureAlgorithm(algorithm).signWith(keys.getKid(), keys.getPrivateKey(), keys.getPublicKey(), keys.getCertificate()).signDocument();
                if (! postBinding && samlClient.addExtensionsElementWithKeyInfo()) {    // Only include extension if REDIRECT binding and signing whole SAML protocol message
                    builder.addExtension(new KeycloakKeySamlExtensionGenerator(keys.getKid()));
                }
            }
            try {
                if (postBinding) {
                    return binding.postBinding(builder.buildDocument()).response(logoutBindingUri);
                } else {
                    return binding.redirectBinding(builder.buildDocument()).response(logoutBindingUri);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private boolean checkSsl() {
            if (session.getContext().getUri().getBaseUri().getScheme().equals("https")) {
                return true;
            } else {
                return !realm.getSslRequired().isRequired(clientConnection);
            }
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

    }

    protected class RedirectBindingProtocol extends BindingProtocol {

        @Override
        protected void verifySignature(SAMLDocumentHolder documentHolder, ClientModel client) throws VerificationException {
            SamlClient samlClient = new SamlClient(client);
            if (!samlClient.requiresClientSignature()) {
                return;
            }
            PublicKey publicKey = SamlProtocolUtils.getSignatureValidationKey(client);
            KeyLocator clientKeyLocator = new HardcodedKeyLocator(publicKey);
            SamlProtocolUtils.verifyRedirectSignature(documentHolder, clientKeyLocator, session.getContext().getUri(), GeneralConstants.SAML_REQUEST_KEY);
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

    }

    protected Response newBrowserAuthentication(AuthenticationSessionModel authSession, boolean isPassive, boolean redirectToAuthentication) {
        SamlProtocol samlProtocol = new SamlProtocol().setEventBuilder(event).setHttpHeaders(headers).setRealm(realm).setSession(session).setUriInfo(session.getContext().getUri());
        return newBrowserAuthentication(authSession, isPassive, redirectToAuthentication, samlProtocol);
    }

    protected Response newBrowserAuthentication(AuthenticationSessionModel authSession, boolean isPassive, boolean redirectToAuthentication, SamlProtocol samlProtocol) {
        return handleBrowserAuthenticationRequest(authSession, samlProtocol, isPassive, redirectToAuthentication);
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
        return getIDPMetadataDescriptor(session.getContext().getUri(), session, realm);

    }

    public static String getIDPMetadataDescriptor(UriInfo uriInfo, KeycloakSession session, RealmModel realm) {
        InputStream is = SamlService.class.getResourceAsStream("/idp-metadata-template.xml");
        String template;
        try {
            template = StreamUtil.readString(is, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            logger.error("Cannot generate IdP metadata", ex);
            return "";
        }
        Properties props = new Properties();
        props.put("idp.entityID", RealmsResource.realmBaseUrl(uriInfo).build(realm.getName()).toString());
        props.put("idp.sso.HTTP-POST", RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString());
        props.put("idp.sso.HTTP-Redirect", RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString());
        props.put("idp.sls.HTTP-POST", RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString());
        StringBuilder keysString = new StringBuilder();
        Set<KeyWrapper> keys = new TreeSet<>((o1, o2) -> o1.getStatus() == o2.getStatus() // Status can be only PASSIVE OR ACTIVE, push PASSIVE to end of list
          ? (int) (o2.getProviderPriority() - o1.getProviderPriority())
          : (o1.getStatus() == KeyStatus.PASSIVE ? 1 : -1));
        keys.addAll(session.keys().getKeys(realm, KeyUse.SIG, Algorithm.RS256));
        for (KeyWrapper key : keys) {
            addKeyInfo(keysString, key, KeyTypes.SIGNING.value());
        }
        props.put("idp.signing.certificates", keysString.toString());
        return StringPropertyReplacer.replaceProperties(template, props);
    }

    private static void addKeyInfo(StringBuilder target, KeyWrapper key, String purpose) {
        if (key == null) {
            return;
        }

        target.append(SPMetadataDescriptor.xmlKeyInfo("                        ",
          key.getKid(), PemUtils.encodeCertificate(key.getCertificate()), purpose, false));
    }

    private boolean isClientProtocolCorrect(ClientModel clientModel) {
        if (SamlProtocol.LOGIN_PROTOCOL.equals(clientModel.getProtocol())) {
            return true;
        }

        return false;
    }

    @GET
    @Path("clients/{client}")
    @Produces(MediaType.TEXT_HTML_UTF_8)
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
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.CLIENT_NOT_FOUND);
        }
        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.CLIENT_DISABLED);
        }
        if (!isClientProtocolCorrect(client)) {
            event.error(Errors.INVALID_CLIENT);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, "Wrong client protocol.");
        }

        session.getContext().setClient(client);

        AuthenticationSessionModel authSession = getOrCreateLoginSessionForIdpInitiatedSso(this.session, this.realm, client, relayState);
        if (authSession == null) {
            logger.error("SAML assertion consumer url not set up");
            event.error(Errors.INVALID_REDIRECT_URI);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REDIRECT_URI);
        }

        return newBrowserAuthentication(authSession, false, false);
    }

    /**
     * Checks the client configuration to return the redirect URL and the binding type.
     * POST is preferred, only if the SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE
     * and management URL are empty REDIRECT is chosen.
     *
     * @param client Client to create client session for
     * @return a two string array [samlUrl, bindingType] or null if error
     */
    private String[] getUrlAndBindingForIdpInitiatedSso(ClientModel client) {
        String postUrl = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE);
        String getUrl = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE);
        if (postUrl != null && !postUrl.trim().isEmpty()) {
            // first the POST binding URL
            return new String[] {postUrl.trim(), SamlProtocol.SAML_POST_BINDING};
        } else if (client.getManagementUrl() != null && !client.getManagementUrl().trim().isEmpty()) {
            // second the management URL and POST
            return new String[] {client.getManagementUrl().trim(), SamlProtocol.SAML_POST_BINDING};
        } else if (getUrl != null && !getUrl.trim().isEmpty()){
            // last option REDIRECT binding and URL
            return new String[] {getUrl.trim(), SamlProtocol.SAML_REDIRECT_BINDING};
        } else {
            // error
            return null;
        }
    }

    /**
     * Creates a client session object for SAML IdP-initiated SSO session.
     * The session takes the parameters from from client definition,
     * namely binding type and redirect URL.
     *
     * @param session KC session
     * @param realm Realm to create client session in
     * @param client Client to create client session for
     * @param relayState Optional relay state - free field as per SAML specification
     * @return The auth session model or null if there is no SAML url is found
     */
    public AuthenticationSessionModel getOrCreateLoginSessionForIdpInitiatedSso(KeycloakSession session, RealmModel realm, ClientModel client, String relayState) {
        String[] bindingProperties = getUrlAndBindingForIdpInitiatedSso(client);
        if (bindingProperties == null) {
            return null;
        }
        String redirect = bindingProperties[0];
        String bindingType = bindingProperties[1];

        AuthenticationSessionModel authSession = createAuthenticationSession(client, null);

        authSession.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        authSession.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
        authSession.setClientNote(SamlProtocol.SAML_BINDING, bindingType);
        authSession.setClientNote(SamlProtocol.SAML_IDP_INITIATED_LOGIN, "true");
        authSession.setRedirectUri(redirect);

        if (relayState == null) {
            relayState = client.getAttribute(SamlProtocol.SAML_IDP_INITIATED_SSO_RELAY_STATE);
        }
        if (relayState != null && !relayState.trim().equals("")) {
            authSession.setClientNote(GeneralConstants.RELAY_STATE, relayState);
        }

        return authSession;
    }


    @POST
    @NoCache
    @Consumes({"application/soap+xml",MediaType.TEXT_XML})
    public Response soapBinding(InputStream inputStream) {
        SamlEcpProfileService bindingService = new SamlEcpProfileService(realm, event, destinationValidator);

        ResteasyProviderFactory.getInstance().injectProperties(bindingService);

        return bindingService.authenticate(inputStream);
    }
}
