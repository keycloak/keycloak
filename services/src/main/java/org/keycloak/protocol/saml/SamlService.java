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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.broker.saml.SAMLDataMarshaller;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.Resteasy;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.BaseIDAbstractType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.protocol.ArtifactResolveType;
import org.keycloak.dom.saml.v2.protocol.ArtifactResponseType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.NameIDPolicyType;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;
import org.keycloak.protocol.saml.profile.ecp.SamlEcpProfileService;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.protocol.saml.util.ArtifactBindingUtils;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.SAML2NameIDBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLRequestWriter;
import org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.transaction.AsyncResponseTransaction;
import org.keycloak.utils.MediaType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.*;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;
import javax.xml.parsers.ParserConfigurationException;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.utils.LockObjectsForModification.lockUserSessionsForModification;


/**
 * Resource class for the saml connect token service
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlService extends AuthorizationEndpointBase {

    protected static final Logger logger = Logger.getLogger(SamlService.class);
    public static final String ARTIFACT_RESOLUTION_SERVICE_PATH = "resolve";

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

        protected Response basicChecks(String samlRequest, String samlResponse, String artifact) {
            logger.tracef("basicChecks(%s, %s, %s)%s", samlRequest, samlResponse, artifact, getShortStackTrace());
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

            if (samlRequest == null && samlResponse == null && artifact == null) {
                event.event(EventType.LOGIN);
                event.error(Errors.SAML_TOKEN_NOT_FOUND);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);

            }
            return null;
        }
        
        protected boolean isDestinationRequired() {
            return true;
        }

        protected Response handleSamlResponse(String samlResponse, String relayState) {
            event.event(EventType.LOGOUT);
            SAMLDocumentHolder holder = extractResponseDocument(samlResponse);

            if (! (holder.getSamlObject() instanceof StatusResponseType)) {
                event.detail(Details.REASON, Errors.INVALID_SAML_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }

            StatusResponseType statusResponse = (StatusResponseType) holder.getSamlObject();
            // validate destination
            if (isDestinationRequired() &&
                    statusResponse.getDestination() == null && containsUnencryptedSignature(holder)) {
                event.detail(Details.REASON, Errors.MISSING_REQUIRED_DESTINATION);
                event.error(Errors.INVALID_SAML_LOGOUT_RESPONSE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }
            if (! destinationValidator.validate(this.getExpectedDestinationUri(session), statusResponse.getDestination())) {
                event.detail(Details.REASON, Errors.INVALID_DESTINATION);
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
            Response response = authManager.browserLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers);
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

            if (samlObject instanceof AuthnRequestType) {
                logger.debug("** login request");
                event.event(EventType.LOGIN);
            } else if (samlObject instanceof LogoutRequestType) {
                logger.debug("** logout request");
                event.event(EventType.LOGOUT);
            } else {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_TOKEN);
                event.detail(Details.REASON, "Unhandled SAML document type: " + (samlObject == null ? "<null>" : samlObject.getClass().getSimpleName()));
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }

            RequestAbstractType requestAbstractType = (RequestAbstractType) samlObject;
            final NameIDType issuerNameId = requestAbstractType.getIssuer();
            String issuer = requestAbstractType.getIssuer() == null ? null : issuerNameId.getValue();
            ClientModel client = realm.getClientByClientId(issuer);

            Response error = checkClientValidity(client);
            if (error != null) {
                return error;
            }

            session.getContext().setClient(client);

            SamlClient samlClient = new SamlClient(client);
            try {
                if (samlClient.requiresClientSignature()) {
                    verifySignature(documentHolder, client);
                }
            } catch (VerificationException e) {
                SamlService.logger.error("request validation failed", e);
                event.error(Errors.INVALID_SIGNATURE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
            }
            logger.debug("verified request");

            if (isDestinationRequired() &&
                    requestAbstractType.getDestination() == null && containsUnencryptedSignature(documentHolder)) {
                event.detail(Details.REASON, Errors.MISSING_REQUIRED_DESTINATION);
                event.error(Errors.INVALID_REQUEST);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }

            if (samlObject instanceof AuthnRequestType) {
                // Get the SAML Request Message
                AuthnRequestType authn = (AuthnRequestType) samlObject;
                return loginRequest(relayState, authn, client);
            } else if (samlObject instanceof LogoutRequestType) {
                LogoutRequestType logout = (LogoutRequestType) samlObject;
                return logoutRequest(logout, client, relayState);
            } else {
                throw new IllegalStateException("Invalid SAML object");
            }
        }

        /**
         * Handle a received artifact message. This means finding the client based on the content of the artifact,
         * sending an ArtifactResolve, receiving an ArtifactResponse, and handling its content based on the "standard"
         * workflows.
         *
         * @param artifact the received artifact
         * @param relayState the current relay state
         * @return a Response based on the content of the ArtifactResponse's content
         */
        protected void handleArtifact(AsyncResponse asyncResponse, String artifact, String relayState) {
            logger.tracef("Keycloak obtained artifact %s. %s", artifact, getShortStackTrace());
            //Find client
            ClientModel client;
            try {
                client = getArtifactResolver(artifact).selectSourceClient(session, artifact);

                Response error = checkClientValidity(client);
                if (error != null) {
                    asyncResponse.resume(error);
                    return;
                }

            } catch (ArtifactResolverProcessingException e) {
                event.event(EventType.LOGIN);
                event.detail(Details.REASON, e.getMessage());
                event.error(Errors.INVALID_SAML_ARTIFACT);
                asyncResponse.resume(ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST));
                return;
            }

            try {
                //send artifact resolve
                Document doc = createArtifactResolve(client.getClientId(), artifact);
                BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();
                SamlClient samlClient = new SamlClient(client);
                if (samlClient.requiresRealmSignature()) {
                    KeyManager keyManager = session.keys();
                    KeyManager.ActiveRsaKey keys = keyManager.getActiveRsaKey(realm);
                    String keyName = samlClient.getXmlSigKeyInfoKeyNameTransformer().getKeyName(keys.getKid(), keys.getCertificate());
                    String canonicalization = samlClient.getCanonicalizationMethod();
                    if (canonicalization != null) {
                        binding.canonicalizationMethod(canonicalization);
                    }
                    binding.signatureAlgorithm(samlClient.getSignatureAlgorithm()).signWith(keyName, keys.getPrivateKey(), keys.getPublicKey(), keys.getCertificate()).signDocument(doc);
                }
                String clientArtifactBindingURL = client.getAttribute(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE);

                if (clientArtifactBindingURL == null || clientArtifactBindingURL.isEmpty()) {
                    throw new ConfigurationException("There is no configured artifact resolution service for the client " + client.getClientId());
                }

                URI clientArtifactBindingURI = new URI(clientArtifactBindingURL);

                ExecutorService executor = session.getProvider(ExecutorsProvider.class).getExecutor("saml-artifact-pool");

                ArtifactResolutionRunnable artifactResolutionRunnable = new ArtifactResolutionRunnable(getBindingType(), asyncResponse, doc, clientArtifactBindingURI, relayState, session.getContext().getConnection());
                ScheduledTaskRunner task = new ScheduledTaskRunner(session.getKeycloakSessionFactory(), artifactResolutionRunnable);
                executor.execute(task);

                logger.tracef("ArtifactResolutionRunnable scheduled, current transaction will be rolled back");
                // Current transaction must be ignored due to asyncResponse.
                session.getTransactionManager().rollback();
            } catch (URISyntaxException | ProcessingException | ParsingException | ConfigurationException e) {
                event.event(EventType.LOGIN);
                event.detail(Details.REASON, e.getMessage());
                event.error(Errors.IDENTITY_PROVIDER_ERROR);
                asyncResponse.resume(ErrorPage.error(session, null, Response.Status.INTERNAL_SERVER_ERROR, Messages.UNEXPECTED_ERROR_HANDLING_REQUEST));
                return;
            }
        }

        protected abstract String encodeSamlDocument(Document samlDocument) throws ProcessingException;

        protected abstract void verifySignature(SAMLDocumentHolder documentHolder, ClientModel client) throws VerificationException;

        protected abstract boolean containsUnencryptedSignature(SAMLDocumentHolder documentHolder);

        protected abstract SAMLDocumentHolder extractRequestDocument(String samlRequest);

        protected abstract SAMLDocumentHolder extractResponseDocument(String response);

        protected Response loginRequest(String relayState, AuthnRequestType requestAbstractType, ClientModel client) {
            SamlClient samlClient = new SamlClient(client);

            if (! validateDestination(requestAbstractType, samlClient, Errors.INVALID_SAML_AUTHN_REQUEST)) {
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
                if ((requestAbstractType.getProtocolBinding() != null
                        && JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri()
                            .equals(requestAbstractType.getProtocolBinding()))
                        || samlClient.forceArtifactBinding()) {
                    redirect = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE);
                } else if (bindingType.equals(SamlProtocol.SAML_POST_BINDING)) {
                    redirect = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE);
                } else {
                    redirect = client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE);
                }
                if (redirect == null || redirect.trim().isEmpty()) {
                    redirect = client.getManagementUrl();
                }

            }

            if (redirect == null) {
                event.error(Errors.INVALID_REDIRECT_URI);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REDIRECT_URI);
            }

            AuthenticationSessionModel authSession = createAuthenticationSession(client, relayState);

            // determine if artifact binding should be used to answer the login request
            if ((requestAbstractType.getProtocolBinding() != null
                    && JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri()
                        .equals(requestAbstractType.getProtocolBinding()))
                    || new SamlClient(client).forceArtifactBinding()) {
                authSession.setClientNote(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.get(), "true");
            }

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
                    event.detail(Details.REASON, Errors.UNSUPPORTED_NAMEID_FORMAT);
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
                } else if (JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.get().equals(requestedProtocolBinding.toString())) {
                    return getBindingType();
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
            if (! validateDestination(logoutRequest, samlClient, Errors.INVALID_SAML_LOGOUT_REQUEST)) {
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }

            // authenticate identity cookie, but ignore an access token timeout as we're logging out anyways.
            AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm, false);
            if (authResult != null) {
                String logoutBinding = getBindingType();
                String postBindingUri = SamlProtocol.getLogoutServiceUrl(session, client, SamlProtocol.SAML_POST_BINDING, false);
                if (samlClient.forcePostBinding() && postBindingUri != null && ! postBindingUri.trim().isEmpty())
                    logoutBinding = SamlProtocol.SAML_POST_BINDING;
                boolean postBinding = Objects.equals(SamlProtocol.SAML_POST_BINDING, logoutBinding);

                String bindingUri = SamlProtocol.getLogoutServiceUrl(session, client, logoutBinding, false);
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

                    //artifact binding state must be attached to the user session upon logout, as authenticated session
                    //no longer exists when the LogoutResponse message is sent
                    if ("true".equals(clientSession.getNote(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.get()))
                            && SamlProtocol.useArtifactForLogout(client)){
                        clientSession.setAction(AuthenticationSessionModel.Action.LOGGING_OUT.name());
                        userSession.setNote(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.get(), "true");
                        userSession.setNote(SamlProtocol.SAML_LOGOUT_INITIATOR_CLIENT_ID, client.getId());
                    }
                }

                for(Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext();) {
                    logoutRequest = it.next().beforeProcessingLogoutRequest(logoutRequest, userSession, clientSession);
                }

                logger.debug("browser Logout");
                return authManager.browserLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers);
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
            String logoutBindingUri = SamlProtocol.getLogoutServiceUrl(session, client, logoutBinding, true);
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

        private boolean validateDestination(RequestAbstractType req, SamlClient samlClient, String errorCode) {
            if (!isDestinationRequired() && req.getDestination() == null) {
                return true;
            }
            // validate destination
            if (req.getDestination() == null && samlClient.requiresClientSignature()) {
                event.detail(Details.REASON, "missing_destination_required");
                event.error(errorCode);
                return false;
            }
            if (! destinationValidator.validate(this.getExpectedDestinationUri(session), req.getDestination())) {
                event.detail(Details.REASON, Errors.INVALID_DESTINATION);
                event.error(errorCode);
                return false;
            }
            return true;
        }

        private boolean checkSsl() {
            if (session.getContext().getUri().getBaseUri().getScheme().equals("https")) {
                return true;
            } else {
                return !realm.getSslRequired().isRequired(clientConnection);
            }
        }

        public Response execute(String samlRequest, String samlResponse, String relayState, String artifact) {
            Response response = basicChecks(samlRequest, samlResponse, artifact);
            if (response != null)
                return response;

            if (samlRequest != null)
                return handleSamlRequest(samlRequest, relayState);
            else
                return handleSamlResponse(samlResponse, relayState);
        }

        public void execute(AsyncResponse asyncReponse, String samlRequest, String samlResponse, String relayState, String artifact) {
            Response response = basicChecks(samlRequest, samlResponse, artifact);

            if (response != null){
                asyncReponse.resume(response);
                return;
            }

            if (artifact != null) {
                handleArtifact(asyncReponse, artifact, relayState);
                return;
            }
            if (samlRequest != null) {
                asyncReponse.resume(handleSamlRequest(samlRequest, relayState));
                return;
            } else {
                asyncReponse.resume(handleSamlResponse(samlResponse, relayState));
            }
        }

        /**
         * KEYCLOAK-12616, KEYCLOAK-12944: construct the expected destination URI using the configured base URI.
         *
         * @param session a reference to the {@link KeycloakSession}.
         * @return the constructed {@link URI}.
         */
        protected URI getExpectedDestinationUri(final KeycloakSession session) {
            final String realmName = session.getContext().getRealm().getName();
            final URI baseUri = session.getContext().getUri().getBaseUri();
            return Urls.samlRequestEndpoint(baseUri, realmName);
        }
    }

    protected class PostBindingProtocol extends BindingProtocol {

        @Override
        protected String encodeSamlDocument(Document samlDocument) throws ProcessingException {
            try {
                return PostBindingUtil.base64Encode(DocumentUtil.asString(samlDocument));
            } catch (IOException e) {
                throw new ProcessingException(e);
            }
        }

        @Override
        protected void verifySignature(SAMLDocumentHolder documentHolder, ClientModel client) throws VerificationException {
            SamlProtocolUtils.verifyDocumentSignature(client, documentHolder.getSamlDocument());
        }

        @Override
        protected boolean containsUnencryptedSignature(SAMLDocumentHolder documentHolder) {
            Document signedDoc = documentHolder.getSamlDocument();
            NodeList nl = signedDoc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            return nl != null && nl.getLength() > 0;
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
        protected String encodeSamlDocument(Document samlDocument) throws ProcessingException {
            try {
                return RedirectBindingUtil.deflateBase64Encode(DocumentUtil.asString(samlDocument).getBytes(GeneralConstants.SAML_CHARSET_NAME));
            } catch (IOException e) {
                throw new ProcessingException(e);
            }
        }

        @Override
        protected void verifySignature(SAMLDocumentHolder documentHolder, ClientModel client) throws VerificationException {
            PublicKey publicKey = SamlProtocolUtils.getSignatureValidationKey(client);
            KeyLocator clientKeyLocator = new HardcodedKeyLocator(publicKey);
            SamlProtocolUtils.verifyRedirectSignature(documentHolder, clientKeyLocator, session.getContext().getUri(), GeneralConstants.SAML_REQUEST_KEY);
        }

        @Override
        protected boolean containsUnencryptedSignature(SAMLDocumentHolder documentHolder) {
            KeycloakUriInfo uriInformation = session.getContext().getUri();
            MultivaluedMap<String, String> encodedParams = uriInformation.getQueryParameters(false);
            String algorithm = encodedParams.getFirst(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY);
            return algorithm != null;
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

    public RedirectBindingProtocol newRedirectBindingProtocol() {
        return new RedirectBindingProtocol();
    }

    public PostBindingProtocol newPostBindingProtocol() {
        return new PostBindingProtocol();
    }

    /**
     */
    @GET
    public void redirectBinding(@Suspended AsyncResponse asyncResponse, @QueryParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest, @QueryParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse, @QueryParam(GeneralConstants.RELAY_STATE) String relayState, @QueryParam(GeneralConstants.SAML_ARTIFACT_KEY) String artifact) {
        logger.debug("SAML GET");
        CacheControlUtil.noBackButtonCacheControlHeader();

        new RedirectBindingProtocol().execute(asyncResponse, samlRequest, samlResponse, relayState, artifact);
    }

    /**
     */
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void postBinding(@Suspended AsyncResponse asyncResponse, @FormParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest, @FormParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse, @FormParam(GeneralConstants.RELAY_STATE) String relayState, @FormParam(GeneralConstants.SAML_ARTIFACT_KEY) String artifact) {
        logger.debug("SAML POST");
        PostBindingProtocol postBindingProtocol = new PostBindingProtocol();
        // this is to support back button on browser
        // if true, we redirect to authenticate URL otherwise back button behavior has bad side effects
        // and we want to turn it off.
        postBindingProtocol.redirectToAuthentication = true;
        postBindingProtocol.execute(asyncResponse, samlRequest, samlResponse, relayState, artifact);
    }

    @GET
    @Path("descriptor")
    @Produces(MediaType.APPLICATION_XML)
    @NoCache
    public String getDescriptor() throws Exception {
        return getIDPMetadataDescriptor(session.getContext().getUri(), session, realm);

    }

    public static String getIDPMetadataDescriptor(UriInfo uriInfo, KeycloakSession session, RealmModel realm) {
        try {
            List<Element> signingKeys = session.keys().getKeysStream(realm, KeyUse.SIG, Algorithm.RS256)
                    .sorted(SamlService::compareKeys)
                    .map(key -> {
                        try {
                            return IDPMetadataDescriptor
                                    .buildKeyInfoElement(key.getKid(), PemUtils.encodeCertificate(key.getCertificate()));
                        } catch (ParserConfigurationException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            return IDPMetadataDescriptor.getIDPDescriptor(
                RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL),
                RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL),
                RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL),
                RealmsResource.protocolUrl(uriInfo).path(SamlService.ARTIFACT_RESOLUTION_SERVICE_PATH)
                        .build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL),
                RealmsResource.realmBaseUrl(uriInfo).build(realm.getName()).toString(),
                true, 
                signingKeys);
        } catch (Exception ex) {
            logger.error("Cannot generate IdP metadata", ex);
            return "";
        }
    }

    public static int compareKeys(KeyWrapper o1, KeyWrapper o2) {
        return o1.getStatus() == o2.getStatus() // Status can be only PASSIVE OR ACTIVE, push PASSIVE to end of list
                ? (int) (o2.getProviderPriority() - o1.getProviderPriority())
                : (o1.getStatus() == KeyStatus.PASSIVE ? 1 : -1);
    }

    private boolean isClientProtocolCorrect(ClientModel clientModel) {
        if (SamlProtocol.LOGIN_PROTOCOL.equals(clientModel.getProtocol())) {
            return true;
        }

        return false;
    }

    private Response checkClientValidity(ClientModel client) {
        if (client == null) {
            event.event(EventType.LOGIN);
            event.detail(Details.REASON, "Cannot_match_source_hash");
            event.error(Errors.CLIENT_NOT_FOUND);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
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

        return null;
    }

    @GET
    @Path("clients/{client}")
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response idpInitiatedSSO(@PathParam("client") String clientUrlName, @QueryParam("RelayState") String relayState) {
        event.event(EventType.LOGIN);
        CacheControlUtil.noBackButtonCacheControlHeader();
        ClientModel client = session.clients()
                .searchClientsByAttributes(realm, Collections.singletonMap(SamlProtocol.SAML_IDP_INITIATED_SSO_URL_NAME, clientUrlName), 0, 1)
                .findFirst().orElse(null);

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

    /**
     * Handles SOAP messages. Chooses the correct response path depending on whether the message is of type ECP or Artifact
     * @param inputStream the data of the request.
     * @return The response to the SOAP message
     */
    @POST
    @Path(ARTIFACT_RESOLUTION_SERVICE_PATH)
    @NoCache
    @Consumes({"application/soap+xml", MediaType.TEXT_XML})
    public Response artifactResolutionService(InputStream inputStream) {
        Document soapBodyContents = Soap.extractSoapMessage(inputStream);
        ArtifactResolveType artifactResolveType = null;
        SAMLDocumentHolder samlDocumentHolder = null;
        try {
            samlDocumentHolder = SAML2Request.getSAML2ObjectFromDocument(soapBodyContents);
            if (samlDocumentHolder.getSamlObject() instanceof ArtifactResolveType) {
                logger.debug("Received artifact resolve message");
                artifactResolveType = (ArtifactResolveType)samlDocumentHolder.getSamlObject();
            }
        } catch (Exception e) {
            logger.errorf("Artifact resolution endpoint obtained request that contained no " +
                    "ArtifactResolve message: %s", DocumentUtil.asString(soapBodyContents));
            return Soap.createFault().reason("").detail("").build();
        }

        if (artifactResolveType == null) {
            logger.errorf("Artifact resolution endpoint obtained request that contained no " +
                    "ArtifactResolve message: %s", DocumentUtil.asString(soapBodyContents));
            return Soap.createFault().reason("").detail("").build();
        }
        
        try {
            return artifactResolve(artifactResolveType, samlDocumentHolder);
        } catch (Exception e) {
            try {
                return emptyArtifactResponseMessage(artifactResolveType, null, JBossSAMLURIConstants.STATUS_REQUEST_DENIED.getUri());
            } catch (ConfigurationException | ProcessingException configurationException) {
                String reason = "An error occurred while trying to return the artifactResponse";
                String detail = e.getMessage();

                if (detail == null) {
                    detail = "";
                }

                logger.errorf("Failure during ArtifactResolve reason: %s, detail: %s", reason, detail);
                return Soap.createFault().reason(reason).detail(detail).build();
            }
        }
    }


    /**
     * Handles SOAP messages. Chooses the correct response path depending on whether the message is of type ECP
     * @param inputStream the data of the request.
     * @return The response to the SOAP message
     */
    @POST
    @NoCache
    @Consumes({"application/soap+xml",MediaType.TEXT_XML})
    public Response soapBinding(InputStream inputStream) {
        SamlEcpProfileService bindingService = new SamlEcpProfileService(realm, event, destinationValidator);

        ResteasyProviderFactory.getInstance().injectProperties(bindingService);

        return bindingService.authenticate(inputStream);
    }

    private ClientModel getAndCheckClientModel(String clientSessionId, String clientId) throws ProcessingException {
        ClientModel client = session.clients().getClientById(realm, clientSessionId);

        if (client == null) {
            throw new ProcessingException(Errors.CLIENT_NOT_FOUND);
        }
        if (!client.isEnabled()) {
            throw new ProcessingException(Errors.CLIENT_DISABLED);
        }
        if (client.isBearerOnly()) {
            throw new ProcessingException(Errors.NOT_ALLOWED);
        }
        if (!client.isStandardFlowEnabled()) {
            throw new ProcessingException(Errors.NOT_ALLOWED);
        }
        if (!client.getClientId().equals(clientId)) {
            logger.errorf("Resolve message with wrong issuer. Artifact was issued for client %s, " +
                            "however ArtifactResolveMessage came from client %s.", client.getClientId(), clientId);
            throw new ProcessingException(Errors.INVALID_SAML_ARTIFACT);
        }

        return client;
    }


    private SingleUseObjectProvider getSingleUseStore() {
        return session.getProvider(SingleUseObjectProvider.class);
    }

    /**
     * Takes an artifact resolve message and returns the artifact response, if the artifact is found belonging to a session
     * of the issuer.
     * @param artifactResolveMessage The artifact resolve message sent by the client
     * @param artifactResolveHolder the document containing the artifact resolve message sent by the client
     * @return a Response containing the SOAP message with the ArifactResponse
     * @throws ParsingException
     * @throws ConfigurationException
     * @throws ProcessingException
     */
    public Response artifactResolve(ArtifactResolveType artifactResolveMessage, SAMLDocumentHolder artifactResolveHolder) throws ParsingException, ConfigurationException, ProcessingException {
        logger.debug("Received artifactResolve message for artifact " + artifactResolveMessage.getArtifact() + "\n" +
                "Message: \n" + DocumentUtil.getDocumentAsString(artifactResolveHolder.getSamlDocument()));

        String artifact = artifactResolveMessage.getArtifact(); // Artifact from resolve request
        if (artifact == null) {
            logger.errorf("Artifact to resolve was null");
            return emptyArtifactResponseMessage(artifactResolveMessage, null, JBossSAMLURIConstants.STATUS_REQUEST_DENIED.getUri());
        }
        
        ArtifactResolver artifactResolver = getArtifactResolver(artifact);

        if (artifactResolver == null) {
            logger.errorf("Cannot find ArtifactResolver for artifact %s", artifact);
            return emptyArtifactResponseMessage(artifactResolveMessage, null, JBossSAMLURIConstants.STATUS_REQUEST_DENIED.getUri());
        }

        // Obtain details of session that issued artifact and check if it corresponds to issuer of Resolve message
        Map<String, String> sessionMapping = getSingleUseStore().get(artifact);

        if (sessionMapping == null) {
            logger.errorf("No data stored for artifact %s", artifact);
            return emptyArtifactResponseMessage(artifactResolveMessage, null);
        }

        UserSessionModel userSessionModel = lockUserSessionsForModification(session, () -> session.sessions().getUserSession(realm, sessionMapping.get(SamlProtocol.USER_SESSION_ID)));
        if (userSessionModel == null) {
            logger.errorf("UserSession with id: %s, that corresponds to artifact: %s does not exist.", sessionMapping.get(SamlProtocol.USER_SESSION_ID), artifact);
            return emptyArtifactResponseMessage(artifactResolveMessage, null);
        }

        AuthenticatedClientSessionModel clientSessionModel = userSessionModel.getAuthenticatedClientSessions().get(sessionMapping.get(SamlProtocol.CLIENT_SESSION_ID));
        if (clientSessionModel == null) {
            logger.errorf("ClientSession with id: %s, that corresponds to artifact: %s and UserSession: %s does not exist.",
                    sessionMapping.get(SamlProtocol.CLIENT_SESSION_ID), artifact, sessionMapping.get(SamlProtocol.USER_SESSION_ID));
            return emptyArtifactResponseMessage(artifactResolveMessage, null);
        }

        ClientModel clientModel = getAndCheckClientModel(sessionMapping.get(SamlProtocol.CLIENT_SESSION_ID), artifactResolveMessage.getIssuer().getValue());
        SamlClient samlClient = new SamlClient(clientModel);

        // Check signature within ArtifactResolve request if client requires it
        if (samlClient.requiresClientSignature()) {
            try {
                SamlProtocolUtils.verifyDocumentSignature(clientModel, artifactResolveHolder.getSamlDocument());
            } catch (VerificationException e) {
                SamlService.logger.error("request validation failed", e);
                return emptyArtifactResponseMessage(artifactResolveMessage, clientModel);
            }
        }

        // Obtain artifactResponse from clientSessionModel
        String artifactResponseString;
        try {
            artifactResponseString = artifactResolver.resolveArtifact(clientSessionModel, artifact);
        } catch (ArtifactResolverProcessingException e) {
            logger.errorf(e, "Failed to resolve artifact: %s.", artifact);
            return emptyArtifactResponseMessage(artifactResolveMessage, clientModel);
        }

        // Artifact is successfully resolved, we can remove session mapping from storage
        if (getSingleUseStore().remove(artifact) == null) {
            logger.debugf("Artifact %s was already removed", artifact);
        }

        Document artifactResponseDocument = null;
        ArtifactResponseType artifactResponseType = null;
        try {
            SAMLDataMarshaller marshaller = new SAMLDataMarshaller();
            artifactResponseType = marshaller.deserialize(artifactResponseString, ArtifactResponseType.class);
            artifactResponseDocument = SamlProtocolUtils.convert(artifactResponseType);
        }  catch (ParsingException | ConfigurationException | ProcessingException e) {
            logger.errorf(e,"Failed to obtain document from ArtifactResponseString: %s.", artifactResponseString);
            return emptyArtifactResponseMessage(artifactResolveMessage, clientModel);
        }

        // If clientSession is in LOGGING_OUT action, now we can move it to LOGGED_OUT
        if (CommonClientSessionModel.Action.LOGGING_OUT.name().equals(clientSessionModel.getAction())) {
            clientSessionModel.setAction(CommonClientSessionModel.Action.LOGGED_OUT.name());

            // If Keycloak sent LogoutResponse we need to also remove UserSession
            if (artifactResponseType.getAny() instanceof StatusResponseType
                    && artifactResponseString.contains(JBossSAMLConstants.LOGOUT_RESPONSE.get())) {
                if (!UserSessionModel.State.LOGGED_OUT_UNCONFIRMED.equals(userSessionModel.getState())) {
                    logger.warnf("Keycloak issued LogoutResponse for clientSession %s, however user session %s was not in LOGGED_OUT_UNCONFIRMED state.",
                            clientSessionModel.getId(), userSessionModel.getId());
                }
                AuthenticationManager.finishUnconfirmedUserSession(session, realm, userSessionModel);
            }
        }

        return artifactResponseMessage(artifactResolveMessage, artifactResponseDocument, clientModel);
    }
    
    private Response emptyArtifactResponseMessage(ArtifactResolveType artifactResolveMessage, ClientModel clientModel) throws ProcessingException, ConfigurationException {
        return emptyArtifactResponseMessage(artifactResolveMessage, clientModel, JBossSAMLURIConstants.STATUS_SUCCESS.getUri());
    }

    private Response emptyArtifactResponseMessage(ArtifactResolveType artifactResolveMessage, ClientModel clientModel, URI responseStatusCode) throws ProcessingException, ConfigurationException {
        ArtifactResponseType artifactResponse = SamlProtocolUtils.buildArtifactResponse(null, SAML2NameIDBuilder.value(
                RealmsResource.realmBaseUrl(session.getContext().getUri()).build(realm.getName()).toString()).build(), responseStatusCode);

        Document artifactResponseDocument;
        try {
            artifactResponseDocument = SamlProtocolUtils.convert(artifactResponse);
        }  catch (ParsingException | ConfigurationException | ProcessingException e) {
            logger.errorf("Failed to obtain document from ArtifactResponse: %s.", artifactResponse);
            throw new ProcessingException(Errors.INVALID_SAML_ARTIFACT_RESPONSE, e);
        }

        return artifactResponseMessage(artifactResolveMessage, artifactResponseDocument, clientModel);
    }
    
    private Response artifactResponseMessage(ArtifactResolveType artifactResolveMessage, Document artifactResponseDocument, ClientModel clientModel) throws ProcessingException, ConfigurationException {
        // Add "inResponseTo" to artifactResponse
        if (artifactResolveMessage.getID() != null && !artifactResolveMessage.getID().trim().isEmpty()){
            Element artifactResponseElement = artifactResponseDocument.getDocumentElement();
            artifactResponseElement.setAttribute("InResponseTo", artifactResolveMessage.getID());
        }
        JaxrsSAML2BindingBuilder bindingBuilder = new JaxrsSAML2BindingBuilder(session);
        
        if (clientModel != null) {
            SamlClient samlClient = new SamlClient(clientModel);

            // Sign document/assertion if necessary, necessary to do this here, as the "inResponseTo" can only be set at this point
            if (samlClient.requiresRealmSignature() || samlClient.requiresAssertionSignature()) {
                KeyManager keyManager = session.keys();
                KeyManager.ActiveRsaKey keys = keyManager.getActiveRsaKey(realm);
                String keyName = samlClient.getXmlSigKeyInfoKeyNameTransformer().getKeyName(keys.getKid(), keys.getCertificate());
                String canonicalization = samlClient.getCanonicalizationMethod();
                if (canonicalization != null) {
                    bindingBuilder.canonicalizationMethod(canonicalization);
                }
                bindingBuilder.signatureAlgorithm(samlClient.getSignatureAlgorithm()).signWith(keyName, keys.getPrivateKey(), keys.getPublicKey(), keys.getCertificate());

                if (samlClient.requiresRealmSignature()) bindingBuilder.signDocument();
                if (samlClient.requiresAssertionSignature()) bindingBuilder.signAssertions();
            }

            // Encrypt assertion if client requires it
            if (samlClient.requiresEncryption()) {
                PublicKey publicKey = null;
                try {
                    publicKey = SamlProtocolUtils.getEncryptionKey(clientModel);
                } catch (Exception e) {
                    logger.error("Failed to obtain encryption key for client", e);
                    return emptyArtifactResponseMessage(artifactResolveMessage, null);
                }
                bindingBuilder.encrypt(publicKey);
            }
        }

        bindingBuilder.postBinding(artifactResponseDocument);

        Soap.SoapMessageBuilder messageBuilder = Soap.createMessage();
        messageBuilder.addToBody(artifactResponseDocument);

        if (logger.isDebugEnabled()) {
            String artifactResponse = DocumentUtil.asString(artifactResponseDocument);
            logger.debugf("Sending artifactResponse message for artifact %s. Message: \n %s", artifactResolveMessage.getArtifact(), artifactResponse);
        }

        return messageBuilder.build();
    }

    /**
     * Creates an ArtifactResolve document with the given issuer and artifact
     * @param issuer the value to set as "issuer"
     * @param artifact the value to set as "artifact"
     * @return the Document of the created ArtifactResolve message
     * @throws ProcessingException
     * @throws ParsingException
     * @throws ConfigurationException
     */
    private Document createArtifactResolve(String issuer, String artifact) throws ProcessingException, ParsingException, ConfigurationException {
        ArtifactResolveType artifactResolve = new ArtifactResolveType(IDGenerator.create("ID_"),
                XMLTimeUtil.getIssueInstant());
        NameIDType nameIDType = new NameIDType();
        nameIDType.setValue(issuer);
        artifactResolve.setIssuer(nameIDType);
        artifactResolve.setArtifact(artifact);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLStreamWriter xmlStreamWriter = StaxUtil.getXMLStreamWriter(bos);
        new SAMLRequestWriter(xmlStreamWriter).write(artifactResolve);
        return DocumentUtil.getDocument(new ByteArrayInputStream(bos.toByteArray()));
    }

    private ArtifactResolver getArtifactResolver(String artifact) {
        ArtifactResolver artifactResolver = session.getProvider(ArtifactResolver.class, ArtifactBindingUtils.artifactToResolverProviderId(artifact));
        return artifactResolver != null ? artifactResolver : session.getProvider(ArtifactResolver.class);
    }

    private class ArtifactResolutionRunnable implements ScheduledTask{

        private AsyncResponse asyncResponse;
        private URI clientArtifactBindingURI;
        private String relayState;
        private Document doc;
        private UriInfo uri;
        private String realmId;
        private HttpHeaders httpHeaders;
        private ClientConnection connection;
        private org.jboss.resteasy.spi.HttpResponse response;
        private HttpRequest request;
        private String bindingType;

        public ArtifactResolutionRunnable(String bindingType, AsyncResponse asyncResponse, Document doc, URI clientArtifactBindingURI, String relayState, ClientConnection connection){
            this.asyncResponse = asyncResponse;
            this.doc = doc;
            this.clientArtifactBindingURI = clientArtifactBindingURI;
            this.relayState = relayState;
            this.uri = session.getContext().getUri();
            this.realmId = realm.getId();
            this.httpHeaders = new ResteasyHttpHeaders(headers.getRequestHeaders());
            this.connection = connection;
            this.response = Resteasy.getContextData(org.jboss.resteasy.spi.HttpResponse.class);
            this.request = Resteasy.getContextData(HttpRequest.class);
            this.bindingType = bindingType;
        }


        public void run(KeycloakSession session){
            // Initialize context
            Resteasy.pushContext(UriInfo.class, uri);

            KeycloakTransaction tx = session.getTransactionManager();
            Resteasy.pushContext(KeycloakTransaction.class, tx);

            Resteasy.pushContext(KeycloakSession.class, session);
            Resteasy.pushContext(HttpHeaders.class, httpHeaders);
            Resteasy.pushContext(org.jboss.resteasy.spi.HttpResponse.class, response);
            Resteasy.pushContext(HttpRequest.class, request);

            Resteasy.pushContext(ClientConnection.class, connection);

            RealmManager realmManager = new RealmManager(session);
            RealmModel realm = realmManager.getRealm(realmId);
            if (realm == null) {
                throw new NotFoundException("Realm does not exist");
            }
            session.getContext().setRealm(realm);

            EventBuilder event = new EventBuilder(realm, session, clientConnection);

            // Call Artifact Resolution Service
            HttpClientProvider httpClientProvider = session.getProvider(HttpClientProvider.class);
            CloseableHttpClient httpClient = httpClientProvider.getHttpClient();
            HttpPost httpPost = Soap.createMessage().addToBody(doc).buildHttpPost(clientArtifactBindingURI);

            if (logger.isTraceEnabled()) {
                logger.tracef("Resolving artifact %s", DocumentUtil.asString(doc));
            }

            try (CloseableHttpResponse result = httpClient.execute(httpPost)) {
                try {
                    if (result.getStatusLine().getStatusCode() != Response.Status.OK.getStatusCode()) {
                        throw new ProcessingException(String.format("Artifact resolution failed with status: %d", result.getStatusLine().getStatusCode()));
                    }

                    Document soapBodyContents = Soap.extractSoapMessage(result.getEntity().getContent());
                    SAMLDocumentHolder samlDoc = SAML2Request.getSAML2ObjectFromDocument(soapBodyContents);
                    if (!(samlDoc.getSamlObject() instanceof ArtifactResponseType)) {
                        throw new ProcessingException("Message received from ArtifactResolveService is not an ArtifactResponseMessage");
                    }

                    if (logger.isTraceEnabled()) {
                        logger.tracef("Resolved object: %s" + DocumentUtil.asString(samlDoc.getSamlDocument()));
                    }
                    
                    ArtifactResponseType art = (ArtifactResponseType) samlDoc.getSamlObject();

                    if (art.getAny() == null) {
                        AsyncResponseTransaction.finishAsyncResponseInTransaction(session, asyncResponse,
                                ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.ARTIFACT_RESOLUTION_SERVICE_INVALID_RESPONSE));
                        return;
                    }

                    LoginProtocolFactory factory = (LoginProtocolFactory) session.getKeycloakSessionFactory().getProviderFactory(LoginProtocol.class, "saml");
                    if (factory == null) {
                        logger.debugf("protocol %s not found", "saml");
                        throw new NotFoundException("Protocol not found");
                    }

                    SamlService endpoint = (SamlService) factory.createProtocolEndpoint(realm, event);
                    ResteasyProviderFactory.getInstance().injectProperties(endpoint);
                    BindingProtocol protocol;
                    if (SamlProtocol.SAML_POST_BINDING.equals(bindingType)) {
                        protocol = endpoint.newPostBindingProtocol();
                    } else if (SamlProtocol.SAML_REDIRECT_BINDING.equals(bindingType)) {
                        protocol = endpoint.newRedirectBindingProtocol();
                    } else {
                        throw new ConfigurationException("Invalid binding protocol: " + bindingType);
                    }

                    if (art.getAny() instanceof ResponseType) {
                        Document clientMessage = SAML2Request.convert((ResponseType) art.getAny());
                        String response = protocol.encodeSamlDocument(clientMessage);

                        AsyncResponseTransaction.finishAsyncResponseInTransaction(session, asyncResponse,
                                protocol.handleSamlResponse(response, relayState));
                    } else if (art.getAny() instanceof RequestAbstractType) {
                        Document clientMessage = SAML2Request.convert((RequestAbstractType) art.getAny());
                        String request = protocol.encodeSamlDocument(clientMessage);
                        AsyncResponseTransaction.finishAsyncResponseInTransaction(session, asyncResponse,
                                protocol.handleSamlRequest(request, relayState));
                    } else {
                        throw new ProcessingException("Cannot recognise message contained in ArtifactResponse");
                    }

                } finally {
                    EntityUtils.consumeQuietly(result.getEntity());
                }

            } catch (IOException | ProcessingException | ParsingException e) {
                event.event(EventType.LOGIN);
                event.detail(Details.REASON, e.getMessage());
                event.error(Errors.IDENTITY_PROVIDER_ERROR);

                AsyncResponseTransaction.finishAsyncResponseInTransaction(session, asyncResponse,
                        ErrorPage.error(session, null, Response.Status.INTERNAL_SERVER_ERROR, Messages.ARTIFACT_RESOLUTION_SERVICE_INVALID_RESPONSE));
            } catch(ConfigurationException e) {
                event.event(EventType.LOGIN);
                event.detail(Details.REASON, e.getMessage());
                event.error(Errors.IDENTITY_PROVIDER_ERROR);
                AsyncResponseTransaction.finishAsyncResponseInTransaction(session, asyncResponse,
                        ErrorPage.error(session, null, Response.Status.INTERNAL_SERVER_ERROR, Messages.UNEXPECTED_ERROR_HANDLING_REQUEST));
            }
        }
    }

}

