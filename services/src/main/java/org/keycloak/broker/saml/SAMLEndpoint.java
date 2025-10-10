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

package org.keycloak.broker.saml;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationDataType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.protocol.ArtifactResponseType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusCodeType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusType;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.OAuthErrorException;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.protocol.saml.SamlSessionUtils;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;
import org.keycloak.protocol.saml.SAMLDecryptionKeysLocator;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.constants.X500SAMLProfileConstants;
import org.keycloak.saml.processing.core.saml.v2.util.ArtifactResponseUtil;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.keycloak.crypto.KeyUse;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.keys.PublicKeyStorageUtils;
import org.keycloak.protocol.saml.SamlMetadataKeyLocator;
import org.keycloak.protocol.saml.SamlMetadataPublicKeyLoader;
import org.keycloak.protocol.saml.SamlPrincipalType;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator;
import org.keycloak.saml.validators.ConditionsValidator;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.URI;
import java.security.cert.CertificateException;

import java.util.Collections;
import jakarta.ws.rs.core.MultivaluedMap;
import javax.xml.crypto.dsig.XMLSignature;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SAMLEndpoint {
    protected static final Logger logger = Logger.getLogger(SAMLEndpoint.class);
    public static final String SAML_FEDERATED_SESSION_INDEX = "SAML_FEDERATED_SESSION_INDEX";
    @Deprecated // in favor of SAML_FEDERATED_SUBJECT_NAMEID
    public static final String SAML_FEDERATED_SUBJECT = "SAML_FEDERATED_SUBJECT";
    @Deprecated // in favor of SAML_FEDERATED_SUBJECT_NAMEID
    public static final String SAML_FEDERATED_SUBJECT_NAMEFORMAT = "SAML_FEDERATED_SUBJECT_NAMEFORMAT";
    public static final String SAML_FEDERATED_SUBJECT_NAMEID = "SAML_FEDERATED_SUBJECT_NAME_ID";
    public static final String SAML_LOGIN_RESPONSE = "SAML_LOGIN_RESPONSE";
    public static final String SAML_ASSERTION = "SAML_ASSERTION";
    public static final String SAML_AUTHN_STATEMENT = "SAML_AUTHN_STATEMENT";
    protected final RealmModel realm;
    protected EventBuilder event;
    protected final SAMLIdentityProviderConfig config;
    protected final IdentityProvider.AuthenticationCallback callback;
    protected final SAMLIdentityProvider provider;
    private final DestinationValidator destinationValidator;

    private final KeycloakSession session;

    private final ClientConnection clientConnection;

    private final HttpHeaders headers;


    public SAMLEndpoint(KeycloakSession session, SAMLIdentityProvider provider, SAMLIdentityProviderConfig config, IdentityProvider.AuthenticationCallback callback, DestinationValidator destinationValidator) {
        this.realm = session.getContext().getRealm();
        this.config = config;
        this.callback = callback;
        this.provider = provider;
        this.destinationValidator = destinationValidator;
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        this.headers = session.getContext().getRequestHeaders();
    }

    @GET
    @NoCache
    @Path("descriptor")
    public Response getSPDescriptor() {
        return provider.export(session.getContext().getUri(), realm, null);
    }

    @GET
    public Response redirectBinding(@QueryParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                    @QueryParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                    @QueryParam(GeneralConstants.SAML_ARTIFACT_KEY) String samlArt,
                                    @QueryParam(GeneralConstants.RELAY_STATE) String relayState)  {
        if (Objects.isNull(samlArt)) {
            return new RedirectBinding().execute(samlRequest, samlResponse, null, relayState, null);
        }
        return new ArtifactBinding().execute(samlRequest, samlResponse, samlArt, relayState, null);
    }


    /**
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postBinding(@FormParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                @FormParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                @FormParam(GeneralConstants.SAML_ARTIFACT_KEY) String samlArt,
                                @FormParam(GeneralConstants.RELAY_STATE) String relayState) {
        if (Objects.isNull(samlArt)) {
            return new PostBinding().execute(samlRequest, samlResponse, null, relayState, null);
        }
        return new ArtifactBinding().execute(samlRequest, samlResponse, samlArt, relayState, null);
    }

    @Path("clients/{client_id}")
    @GET
    public Response redirectBindingIdpInitiated(@QueryParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                                @QueryParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                                @QueryParam(GeneralConstants.RELAY_STATE) String relayState,
                                                @PathParam("client_id") String clientId)  {
        return new RedirectBinding().execute(samlRequest, samlResponse, null, relayState, clientId);
    }


    /**
     */
    @Path("clients/{client_id}")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postBindingIdpInitiated(@FormParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                            @FormParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                            @FormParam(GeneralConstants.RELAY_STATE) String relayState,
                                            @PathParam("client_id") String clientId) {
        return new PostBinding().execute(samlRequest, samlResponse, null, relayState, clientId);
    }

    protected abstract class Binding {
        private boolean checkSsl() {
            if (session.getContext().getUri().getBaseUri().getScheme().equals("https")) {
                return true;
            } else {
                return !realm.getSslRequired().isRequired(clientConnection);
            }
        }

        protected Response basicChecks(String samlRequest, String samlResponse, String samlArt) {
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

            if (samlRequest == null && samlResponse == null&& samlArt == null) {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_REQUEST);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);

            }
            return null;
        }

        protected abstract String getBindingType();
        protected abstract boolean containsUnencryptedSignature(SAMLDocumentHolder documentHolder);
        protected abstract void verifySignature(String key, SAMLDocumentHolder documentHolder) throws VerificationException;
        protected abstract SAMLDocumentHolder extractRequestDocument(String samlRequest);
        protected abstract SAMLDocumentHolder extractResponseDocument(String response);

        protected boolean isDestinationRequired() {
            return true;
        }

        protected KeyLocator getIDPKeyLocator() {
            if (StringUtil.isNotBlank(config.getMetadataDescriptorUrl()) && config.isUseMetadataDescriptorUrl()) {
                String modelKey = PublicKeyStorageUtils.getIdpModelCacheKey(realm.getId(), config.getInternalId());
                PublicKeyLoader keyLoader = new SamlMetadataPublicKeyLoader(session, config.getMetadataDescriptorUrl());
                PublicKeyStorageProvider keyStorage = session.getProvider(PublicKeyStorageProvider.class);
                return new SamlMetadataKeyLocator(modelKey, keyLoader, KeyUse.SIG, keyStorage);
            }

            List<Key> keys = new LinkedList<>();
            for (String signingCertificate : config.getSigningCertificates()) {
                X509Certificate cert = null;
                try {
                    cert = XMLSignatureUtil.getX509CertificateFromKeyInfoString(signingCertificate.replaceAll("\\s", ""));
                    cert.checkValidity();
                    keys.add(cert.getPublicKey());
                } catch (CertificateException e) {
                    logger.warnf("Ignoring invalid certificate: %s", cert);
                } catch (ProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            return new HardcodedKeyLocator(keys);
        }

        public Response execute(String samlRequest, String samlResponse, String samlArt, String relayState, String clientId) {
            event = new EventBuilder(realm, session, clientConnection);
            Response response = basicChecks(samlRequest, samlResponse, samlArt);
            if (response != null) return response;
            if (samlRequest != null) return handleSamlRequest(samlRequest, relayState);
            if (samlArt != null) return handleSamlArt(samlArt, relayState, clientId);
            else return handleSamlResponse(samlResponse, relayState, clientId);
        }

        protected Response handleSamlRequest(String samlRequest, String relayState) {
            SAMLDocumentHolder holder = extractRequestDocument(samlRequest);
            RequestAbstractType requestAbstractType = (RequestAbstractType) holder.getSamlObject();
            // validate destination
            if (isDestinationRequired() &&
                    requestAbstractType.getDestination() == null && containsUnencryptedSignature(holder)) {
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.detail(Details.REASON, Errors.MISSING_REQUIRED_DESTINATION);
                event.error(Errors.INVALID_REQUEST);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }
            if (! destinationValidator.validate(getExpectedDestination(config.getAlias(), null), requestAbstractType.getDestination())) {
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.detail(Details.REASON, Errors.INVALID_DESTINATION);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }
            if (config.isValidateSignature()) {
                try {
                    verifySignature(GeneralConstants.SAML_REQUEST_KEY, holder);
                } catch (VerificationException e) {
                    logger.error("validation failed", e);
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SIGNATURE);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
                }
            }

            if (requestAbstractType instanceof LogoutRequestType) {
                logger.debug("** logout request");
                event.event(EventType.LOGOUT);
                LogoutRequestType logout = (LogoutRequestType) requestAbstractType;
                return logoutRequest(logout, relayState);

            } else {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_TOKEN);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }
        }

        protected Response logoutRequest(LogoutRequestType request, String relayState) {
            if (request.getNameID() == null && request.getBaseID() == null && request.getEncryptedID() == null){
                logger.error("SAML IdP Logout request must contain at least one of BaseID, NameID and EncryptedID");
                event.error(Errors.INVALID_SAML_LOGOUT_REQUEST);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_LOGOUT_FAILURE);
            }

            if (request.getSessionIndex() == null || request.getSessionIndex().isEmpty()) {
                if (request.getNameID() == null){
                    //TODO this need to be implemented
                    logger.error("SAML IdP Logout request contains BaseID or EncryptedID without Session Index");
                    event.error(Errors.INVALID_SAML_LOGOUT_REQUEST);
                    return ErrorPage.error(session, null, Response.Status.NOT_IMPLEMENTED, Messages.IDENTITY_PROVIDER_LOGOUT_FAILURE);
                }
                String brokerUserId = config.getAlias() + "." + request.getNameID().getValue();
                AtomicReference<LogoutRequestType> ref = new AtomicReference<>(request);
                session.sessions().getUserSessionByBrokerUserIdStream(realm, brokerUserId)
                        .filter(userSession -> userSession.getState() != UserSessionModel.State.LOGGING_OUT &&
                                userSession.getState() != UserSessionModel.State.LOGGED_OUT)
                        .collect(Collectors.toList()) // collect to avoid concurrent modification as backchannelLogout removes the user sessions.
                        .forEach(processLogout(ref));
                request = ref.get();

            }  else {
                for (String sessionIndex : request.getSessionIndex()) {
                    String brokerSessionId = config.getAlias()  + "." + sessionIndex;
                    UserSessionModel userSession = session.sessions().getUserSessionByBrokerSessionId(realm, brokerSessionId);
                    if (userSession != null) {
                        if (userSession.getState() == UserSessionModel.State.LOGGING_OUT || userSession.getState() == UserSessionModel.State.LOGGED_OUT) {
                            continue;
                        }

                        for(Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext();) {
                            request = it.next().beforeProcessingLogoutRequest(request, userSession, null);
                        }

                        try {
                            AuthenticationManager.backchannelLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers, false);
                        } catch (Exception e) {
                            logger.warn("failed to do backchannel logout for userSession", e);
                        }
                    }
                }
            }

            String issuerURL = getEntityId(session.getContext().getUri(), realm);
            SAML2LogoutResponseBuilder builder = new SAML2LogoutResponseBuilder();
            builder.logoutRequestID(request.getID());
            builder.destination(config.getSingleLogoutServiceUrl());
            builder.issuer(issuerURL);
            JaxrsSAML2BindingBuilder binding = new JaxrsSAML2BindingBuilder(session)
                        .relayState(relayState);
            boolean postBinding = config.isPostBindingLogout();
            if (config.isWantAuthnRequestsSigned()) {
                KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);
                String keyName = config.getXmlSigKeyInfoKeyNameTransformer().getKeyName(keys.getKid(), keys.getCertificate());
                binding.signWith(keyName, keys.getPrivateKey(), keys.getPublicKey(), keys.getCertificate())
                        .signatureAlgorithm(provider.getSignatureAlgorithm())
                        .signDocument();
                if (! postBinding && config.isAddExtensionsElementWithKeyInfo()) {    // Only include extension if REDIRECT binding and signing whole SAML protocol message
                    builder.addExtension(new KeycloakKeySamlExtensionGenerator(keyName));
                }
            }
            try {
                if (postBinding) {
                    return binding.postBinding(builder.buildDocument()).response(config.getSingleLogoutServiceUrl());
                } else {
                    return binding.redirectBinding(builder.buildDocument()).response(config.getSingleLogoutServiceUrl());
                }
            } catch (ConfigurationException e) {
                throw new RuntimeException(e);
            } catch (ProcessingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        protected Response handleSamlArt(String samlArt, String relayState, String clientId) {
            try {
                // execute the Resolve Artifact request
                SAMLDocumentHolder samlDocumentHolder = provider.resolveArtifact(session, session.getContext().getUri(), realm, relayState, samlArt);

                // validate the type of the SAML object
                if (!(samlDocumentHolder.getSamlObject() instanceof ArtifactResponseType artifactResponse)) {
                    logger.error("artifact binding failed: the SAML object is not an ArtifactResponse");
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.detail(Details.REASON, Errors.INVALID_SAML_ARTIFACT_RESPONSE);
                    event.error(Errors.INVALID_REQUEST);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
                }

                // validate the signature of the ArtifactResponse
                if (config.isValidateSignature()) {
                    try {
                        verifySignature(GeneralConstants.SAML_RESPONSE_KEY, samlDocumentHolder);
                    } catch (VerificationException e) {
                        logger.error("artifact binding failed: the ArtifactResponse signature is invalid", e);
                        event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                        event.error(Errors.INVALID_SIGNATURE);
                        return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_INVALID_SIGNATURE);
                    }
                }

                if (!(artifactResponse.getAny() instanceof ResponseType embeddedResponse)) {
                    logger.error("artifact binding failed: the embedded SAML object is not a Response");
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.detail(Details.REASON, Errors.INVALID_SAML_ARTIFACT_RESPONSE);
                    event.error(Errors.INVALID_REQUEST);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
                }

                // validate the destination of the embedded Response
                if (isDestinationRequired() && embeddedResponse.getDestination() == null && containsUnencryptedSignature(samlDocumentHolder)) {
                    logger.error("artifact binding failed: the embedded Response does not contain a destination");
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.detail(Details.REASON, Errors.MISSING_REQUIRED_DESTINATION);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
                }
                if (!destinationValidator.validate(getExpectedDestination(config.getAlias(), clientId), embeddedResponse.getDestination())) {
                    logger.error("artifact binding failed: the embedded Response has an invalid destination");
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.detail(Details.REASON, Errors.INVALID_DESTINATION);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
                }

                // extract the SAML Response from the original SAML ArtifactResponse
                Optional<String>  optionalEmbeddedResponseAsString = ArtifactResponseUtil.convertResponseToString(
                        samlDocumentHolder.getSamlDocument()
                );

                // leave early if the embedded Response cannot be converted to string
                if(optionalEmbeddedResponseAsString.isEmpty()) {
                    logger.error("artifact binding failed: the embedded Response cannot be converted to string");
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.detail(Details.REASON, Errors.INVALID_SAML_ARTIFACT_RESPONSE);
                    event.error(Errors.INVALID_REQUEST);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
                }

                // convert the embedded SAML response to a base64 serialized string
                String embeddedResponseAsString = optionalEmbeddedResponseAsString.get();
                logger.debugf("embeddedResponseAsString %s", embeddedResponseAsString);
                String embeddedResponseAsBase64 =  PostBindingUtil.base64Encode(embeddedResponseAsString);

                // continue the flow with POST binding
                return execute(null, embeddedResponseAsBase64, null, relayState, clientId);
            } catch (IOException e) {
                logger.error("artifact binding failed", e);
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.detail(Details.REASON, Errors.INVALID_SAML_ARTIFACT_RESPONSE);
                event.error(Errors.INVALID_REQUEST);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }
        }

        private Consumer<UserSessionModel> processLogout(AtomicReference<LogoutRequestType> ref) {
            return userSession -> {
                for(Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext();) {
                    ref.set(it.next().beforeProcessingLogoutRequest(ref.get(), userSession, null));
                }
                try {
                    AuthenticationManager.backchannelLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers, false);
                } catch (Exception e) {
                    logger.warn("failed to do backchannel logout for userSession", e);
                }
            };
        }

        private String getEntityId(UriInfo uriInfo, RealmModel realm) {
            String configEntityId = config.getEntityId();

            if (configEntityId == null || configEntityId.isEmpty())
                return UriBuilder.fromUri(uriInfo.getBaseUri()).path("realms").path(realm.getName()).build().toString();
            else
                return configEntityId;
        }

        protected Response handleLoginResponse(String samlResponse, SAMLDocumentHolder holder, ResponseType responseType, String relayState, String clientId) {

            try {
                AuthenticationSessionModel authSession;
                if (StringUtil.isNotBlank(clientId)) {
                    authSession = samlIdpInitiatedSSO(clientId);
                } else if (StringUtil.isNotBlank(relayState)) {
                    authSession = callback.getAndVerifyAuthenticationSession(relayState);
                } else {
                    logger.error("SAML RelayState parameter was null when it should be returned by the IDP");
                    event.event(EventType.LOGIN);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                }
                session.getContext().setAuthenticationSession(authSession);

                if (isNoPassiveSamlResponse(responseType)) {
                    return callback.error(config, OAuthErrorException.LOGIN_REQUIRED);
                } else if (!isSuccessfulSamlResponse(responseType)) {
                    String statusMessage = responseType.getStatus() == null || responseType.getStatus().getStatusMessage() == null ? Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR : responseType.getStatus().getStatusMessage();
                    if (Constants.AUTHENTICATION_EXPIRED_MESSAGE.equals(statusMessage)) {
                        return callback.retryLogin(provider, authSession);
                    } else {
                        return callback.error(config, statusMessage);
                    }
                }
                if (responseType.getAssertions() == null || responseType.getAssertions().isEmpty()) {
                    return callback.error(config, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                }

                boolean assertionIsEncrypted = AssertionUtil.isAssertionEncrypted(responseType);

                if (config.isWantAssertionsEncrypted() && !assertionIsEncrypted) {
                    logger.error("The assertion is not encrypted, which is required.");
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
                }

                Element assertionElement = null;

                if (assertionIsEncrypted) {
                    try {
                        XMLEncryptionUtil.DecryptionKeyLocator decryptionKeyLocator = new SAMLDecryptionKeysLocator(session, realm, config.getEncryptionAlgorithm());
                        assertionElement = AssertionUtil.decryptAssertion(responseType, decryptionKeyLocator);
                    } catch (ProcessingException ex) {
                        logger.warnf(ex, "Not possible to decrypt SAML assertion. Please check realm keys of usage ENC in the realm '%s' and make sure there is a key able to decrypt the assertion encrypted by identity provider '%s'", realm.getName(), config.getAlias());
                        throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
                    }
                } else {
                    /* We verify the assertion using original document to handle cases where the IdP
                    includes whitespace and/or newlines inside tags. */
                    assertionElement = DocumentUtil.getElement(holder.getSamlDocument(), new QName(JBossSAMLConstants.ASSERTION.get()));
                }

                // Validate the response Issuer
                final String responseIssuer = responseType.getIssuer() != null ? responseType.getIssuer().getValue(): null;
                final boolean responseIssuerValidationSuccess = config.getIdpEntityId() == null ||
                    (responseIssuer != null && responseIssuer.equals(config.getIdpEntityId()));
                if (!responseIssuerValidationSuccess) {
                    logger.errorf("Response Issuer validation failed: expected %s, actual %s", config.getIdpEntityId(), responseIssuer);
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
                }

                // When artifact binding is used, the LoginResponse is embedded in the ArtifactResponse
                // Therefore, the InResponseTo attribute of the LoginResponse cannot be validated
                // Moreover, the LoginResponse is not signed
                boolean isArtifactBinding = SamlProtocol.SAML_ARTIFACT_BINDING.equals(getBindingType());

                // Validate InResponseTo attribute: must match the generated request ID
                String expectedRequestId = authSession.getClientNote(SamlProtocol.SAML_REQUEST_ID_BROKER);
                final boolean inResponseToValidationSuccess = validateInResponseToAttribute(responseType, expectedRequestId);
                if (!isArtifactBinding && !inResponseToValidationSuccess)
                {
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
                }

                boolean signed = AssertionUtil.isSignedElement(assertionElement);
                final boolean assertionSignatureNotExistsWhenRequired = config.isWantAssertionsSigned() && !signed;
                final boolean signatureNotValid = signed && config.isValidateSignature() && !AssertionUtil.isSignatureValid(assertionElement, getIDPKeyLocator());
                final boolean hasNoSignatureWhenRequired = ! signed && config.isValidateSignature() && ! containsUnencryptedSignature(holder);

                if (assertionSignatureNotExistsWhenRequired || signatureNotValid || hasNoSignatureWhenRequired) {
                    logger.error("validation failed");
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SIGNATURE);
                    return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_INVALID_SIGNATURE);
                }

                if (AssertionUtil.isIdEncrypted(responseType)) {
                    try {
                        XMLEncryptionUtil.DecryptionKeyLocator decryptionKeyLocator = new SAMLDecryptionKeysLocator(session, realm, config.getEncryptionAlgorithm());
                        AssertionUtil.decryptId(responseType, decryptionKeyLocator);
                    } catch (ProcessingException ex) {
                        logger.warnf(ex, "Not possible to decrypt SAML encryptedId. Please check realm keys of usage ENC in the realm '%s' and make sure there is a key able to decrypt the encryptedId encrypted by identity provider '%s'", realm.getName(), config.getAlias());
                        throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
                    }
                }

                AssertionType assertion = responseType.getAssertions().get(0).getAssertion();

                // Validate the assertion Issuer
                final String assertionIssuer = assertion.getIssuer() != null ? assertion.getIssuer().getValue(): null;
                final boolean assertionIssuerValidationSuccess = config.getIdpEntityId() == null ||
                    (assertionIssuer != null && assertionIssuer.equals(config.getIdpEntityId()));
                if (!assertionIssuerValidationSuccess) {
                    logger.errorf("Assertion Issuer validation failed: expected %s, actual %s", config.getIdpEntityId(), assertionIssuer);
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
                }

                NameIDType subjectNameID = getSubjectNameID(assertion);
                String principal = getPrincipal(assertion);

                if (principal == null) {
                    logger.errorf("no principal in assertion; expected: %s", expectedPrincipalType());
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
                }

                //Map<String, String> notes = new HashMap<>();
                BrokeredIdentityContext identity = new BrokeredIdentityContext(principal, config);
                identity.getContextData().put(SAML_LOGIN_RESPONSE, responseType);
                identity.getContextData().put(SAML_ASSERTION, assertion);
                identity.setAuthenticationSession(authSession);

                identity.setUsername(principal);

                //SAML Spec 2.2.2 Format is optional
                if (subjectNameID != null && subjectNameID.getFormat() != null && subjectNameID.getFormat().toString().equals(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get())) {
                    identity.setEmail(subjectNameID.getValue());
                }

                if (config.isStoreToken()) {
                    identity.setToken(samlResponse);
                }

                ConditionsValidator.Builder cvb = new ConditionsValidator.Builder(assertion.getID(), assertion.getConditions(), destinationValidator)
                        .clockSkewInMillis(1000 * config.getAllowedClockSkew());
                try {
                    String issuerURL = getEntityId(session.getContext().getUri(), realm);
                    cvb.addAllowedAudience(URI.create(issuerURL));
                    // getDestination has been validated to match request URL already so it matches SAML endpoint
                    if (responseType.getDestination() != null) {
                        cvb.addAllowedAudience(URI.create(responseType.getDestination()));
                    }
                } catch (IllegalArgumentException ex) {
                    // warning has been already emitted in DeploymentBuilder
                }
                if (! cvb.build().isValid()) {
                    logger.error("Assertion expired.");
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.EXPIRED_CODE);
                }

                AuthnStatementType authn = null;
                for (Object statement : assertion.getStatements()) {
                    if (statement instanceof AuthnStatementType) {
                        authn = (AuthnStatementType)statement;
                        identity.getContextData().put(SAML_AUTHN_STATEMENT, authn);
                        break;
                    }
                }
                if (assertion.getAttributeStatements() != null ) {
                    String email = getX500Attribute(assertion, X500SAMLProfileConstants.EMAIL);
                    if (email != null)
                        identity.setEmail(email);
                }

                String brokerUserId = config.getAlias() + "." + principal;
                identity.setBrokerUserId(brokerUserId);
                identity.setIdp(provider);
                if (authn != null && authn.getSessionIndex() != null) {
                    identity.setBrokerSessionId(config.getAlias() + "." + authn.getSessionIndex());
                 }


                return callback.authenticated(identity);
            } catch (WebApplicationException e) {
                return e.getResponse();
            } catch (Exception e) {
                throw new IdentityBrokerException("Could not process response from SAML identity provider.", e);
            }
        }


        /**
         * If there is a client whose SAML IDP-initiated SSO URL name is set to the
         * given {@code clientUrlName}, creates a fresh authentication session for that
         * client and returns a {@link AuthenticationSessionModel} object with that session.
         * Otherwise returns "client not found" response.
         *
         * @param clientUrlName
         * @return see description
         */
        private AuthenticationSessionModel samlIdpInitiatedSSO(final String clientUrlName) {
            event.event(EventType.LOGIN);
            CacheControlUtil.noBackButtonCacheControlHeader(session);
            Optional<ClientModel> oClient = SAMLEndpoint.this.session.clients()
              .searchClientsByAttributes(realm, Collections.singletonMap(SamlProtocol.SAML_IDP_INITIATED_SSO_URL_NAME, clientUrlName), 0, 1)
              .findFirst();

            if (! oClient.isPresent()) {
                event.error(Errors.CLIENT_NOT_FOUND);
                Response response = ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.CLIENT_NOT_FOUND);
                throw new WebApplicationException(response);
            }

            LoginProtocolFactory factory = (LoginProtocolFactory) session.getKeycloakSessionFactory().getProviderFactory(LoginProtocol.class, SamlProtocol.LOGIN_PROTOCOL);
            SamlService samlService = (SamlService) factory.createProtocolEndpoint(SAMLEndpoint.this.session, event);
            AuthenticationSessionModel authSession = samlService.getOrCreateLoginSessionForIdpInitiatedSso(session, SAMLEndpoint.this.realm, oClient.get(), null);
            if (authSession == null) {
                event.error(Errors.INVALID_REDIRECT_URI);
                Response response = ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REDIRECT_URI);
                throw new WebApplicationException(response);
            }

            return authSession;
        }

        private StatusCodeType getSamlResponseStatusCode(ResponseType responseType) {
            return Optional.ofNullable(responseType)
                    .map(StatusResponseType::getStatus)
                    .map(StatusType::getStatusCode)
                    .orElse(null);
        }

        private boolean isSuccessfulSamlResponse(ResponseType responseType) {
            var statusCode = Optional.ofNullable(getSamlResponseStatusCode(responseType))
                    .map(StatusCodeType::getValue)
                    .map(URI::toString)
                    .orElse(null);
            return JBossSAMLURIConstants.STATUS_SUCCESS.get().equals(statusCode);
        }

        private boolean isNoPassiveSamlResponse(ResponseType responseType) {
            var secondaryStatusCode = Optional.ofNullable(getSamlResponseStatusCode(responseType))
                    .map(StatusCodeType::getStatusCode)
                    .map(StatusCodeType::getValue)
                    .map(URI::toString)
                    .orElse(null);
            return JBossSAMLURIConstants.STATUS_NO_PASSIVE.get().equals(secondaryStatusCode);
        }

        public Response handleSamlResponse(String samlResponse, String relayState, String clientId) {
            SAMLDocumentHolder holder = extractResponseDocument(samlResponse);
            if (holder == null) {
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.detail(Details.REASON, Errors.INVALID_SAML_DOCUMENT);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_INVALID_RESPONSE);
            }
            StatusResponseType statusResponse = (StatusResponseType)holder.getSamlObject();
            // validate destination
            if (isDestinationRequired()
                    && statusResponse.getDestination() == null && containsUnencryptedSignature(holder)) {
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.detail(Details.REASON, Errors.MISSING_REQUIRED_DESTINATION);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }
            if (! destinationValidator.validate(getExpectedDestination(config.getAlias(), clientId), statusResponse.getDestination())) {
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.detail(Details.REASON, Errors.INVALID_DESTINATION);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }
            if (config.isValidateSignature()) {
                try {
                    verifySignature(GeneralConstants.SAML_RESPONSE_KEY, holder);
                } catch (VerificationException e) {
                    logger.error("validation failed", e);
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SIGNATURE);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_INVALID_SIGNATURE);
                }
            }
            if (statusResponse instanceof ResponseType) {
                return handleLoginResponse(samlResponse, holder, (ResponseType)statusResponse, relayState, clientId);

            } else {
                // todo need to check that it is actually a LogoutResponse
                return handleLogoutResponse(holder, statusResponse, relayState);
            }
            //throw new RuntimeException("Unknown response type");

        }

        protected Response handleLogoutResponse(SAMLDocumentHolder holder, StatusResponseType responseType, String relayState) {
            if (relayState == null) {
                logger.error("no valid user session");
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }
            UserSessionModel userSession = session.sessions().getUserSession(realm, relayState);
            if (userSession == null) {
                logger.error("no valid user session");
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }
            if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
                logger.error("usersession in different state");
                event.event(EventType.LOGOUT);
                event.error(Errors.USER_SESSION_NOT_FOUND);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.SESSION_NOT_ACTIVE);
            }
            return AuthenticationManager.finishBrowserLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers);
        }

        private String getExpectedDestination(String providerAlias, String clientId) {
            if(clientId != null) {
                return Urls.identityProviderAuthnResponse(session.getContext().getUri().getBaseUri(), providerAlias, realm.getName(), clientId).toString();
            }
            return Urls.identityProviderAuthnResponse(session.getContext().getUri().getBaseUri(), providerAlias, realm.getName()).toString();
        }
    }

    protected class PostBinding extends Binding {
        @Override
        protected boolean containsUnencryptedSignature(SAMLDocumentHolder documentHolder) {
            NodeList nl = documentHolder.getSamlDocument().getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            return (nl != null && nl.getLength() > 0);
        }

        @Override
        protected void verifySignature(String key, SAMLDocumentHolder documentHolder) throws VerificationException {
            if ((! containsUnencryptedSignature(documentHolder)) && (documentHolder.getSamlObject() instanceof ResponseType)) {
                ResponseType responseType = (ResponseType) documentHolder.getSamlObject();
                List<ResponseType.RTChoiceType> assertions = responseType.getAssertions();
                if (! assertions.isEmpty() ) {
                    // Only relax verification if the response is an authnresponse and contains (encrypted/plaintext) assertion.
                    // In that case, signature is validated on assertion element
                    return;
                }
            }
            SamlProtocolUtils.verifyDocumentSignature(documentHolder.getSamlDocument(), getIDPKeyLocator());
        }

        @Override
        protected SAMLDocumentHolder extractRequestDocument(String samlRequest) {
            return SAMLRequestParser.parseRequestPostBinding(samlRequest);
        }
        @Override
        protected SAMLDocumentHolder extractResponseDocument(String response) {
            byte[] samlBytes = PostBindingUtil.base64Decode(response);
            return SAMLRequestParser.parseResponseDocument(samlBytes);
        }

        @Override
        protected String getBindingType() {
            return SamlProtocol.SAML_POST_BINDING;
        }
    }

    protected class RedirectBinding extends Binding {
        @Override
        protected boolean containsUnencryptedSignature(SAMLDocumentHolder documentHolder) {
            MultivaluedMap<String, String> encodedParams = session.getContext().getUri().getQueryParameters(false);
            String algorithm = encodedParams.getFirst(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY);
            String signature = encodedParams.getFirst(GeneralConstants.SAML_SIGNATURE_REQUEST_KEY);
            return algorithm != null && signature != null;
        }

        @Override
        protected void verifySignature(String key, SAMLDocumentHolder documentHolder) throws VerificationException {
            KeyLocator locator = getIDPKeyLocator();
            SamlProtocolUtils.verifyRedirectSignature(documentHolder, locator, session.getContext().getUri(), key);
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

    protected class ArtifactBinding extends Binding {

        private boolean unencryptedSignaturesVerified = false;

        @Override
        protected boolean containsUnencryptedSignature(SAMLDocumentHolder documentHolder) {
            if (unencryptedSignaturesVerified) {
                return true;
            }
            NodeList nl = documentHolder.getSamlDocument().getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            return (nl != null && nl.getLength() > 0);
        }

        @Override
        protected void verifySignature(String key, SAMLDocumentHolder documentHolder) throws VerificationException {
            if (unencryptedSignaturesVerified) {
                // this is the second pass and signatures were already verified in the artifact response time
                return;
            }
            if (!containsUnencryptedSignature(documentHolder)) {
                List<ResponseType.RTChoiceType> assertions = null;
                if (documentHolder.getSamlObject() instanceof ResponseType responseType) {
                    assertions = responseType.getAssertions();
                } else if (documentHolder.getSamlObject() instanceof ArtifactResponseType artifactResponseType
                        && artifactResponseType.getAny() instanceof ResponseType responseType) {
                    assertions = responseType.getAssertions();
                }
                if (assertions != null && !assertions.isEmpty() ) {
                    // Only relax verification if the response is an authnresponse and contains (encrypted/plaintext) assertion.
                    // In that case, signature is validated on assertion element
                    return;
                }
            }
            SamlProtocolUtils.verifyDocumentSignature(documentHolder.getSamlDocument(), getIDPKeyLocator());
            unencryptedSignaturesVerified = true; // mark signatures as verified
        }

        @Override
        protected SAMLDocumentHolder extractRequestDocument(String samlRequest) {
            throw new UnsupportedOperationException("SAML request is not compliant with Artifact binding");
        }
        @Override
        protected SAMLDocumentHolder extractResponseDocument(String response) {
            byte[] samlBytes = PostBindingUtil.base64Decode(response);
            return SAMLRequestParser.parseResponseDocument(samlBytes);
        }

        @Override
        protected String getBindingType() {
            return SamlProtocol.SAML_ARTIFACT_BINDING;
        }
    }

    private String getX500Attribute(AssertionType assertion, X500SAMLProfileConstants attribute) {
        return getFirstMatchingAttribute(assertion, attribute::correspondsTo);
    }

    private String getAttributeByName(AssertionType assertion, String name) {
        return getFirstMatchingAttribute(assertion, attribute -> Objects.equals(attribute.getName(), name));
    }

    private String getAttributeByFriendlyName(AssertionType assertion, String friendlyName) {
        return getFirstMatchingAttribute(assertion, attribute -> Objects.equals(attribute.getFriendlyName(), friendlyName));
    }

    private String getPrincipal(AssertionType assertion) {

        SamlPrincipalType principalType = config.getPrincipalType();

        if (principalType == null || principalType.equals(SamlPrincipalType.SUBJECT)) {
            NameIDType subjectNameID = getSubjectNameID(assertion);
            return subjectNameID != null ? subjectNameID.getValue() : null;
        } else if (principalType.equals(SamlPrincipalType.ATTRIBUTE)) {
            return getAttributeByName(assertion, config.getPrincipalAttribute());
        } else {
            return getAttributeByFriendlyName(assertion, config.getPrincipalAttribute());
        }

    }

    private String getFirstMatchingAttribute(AssertionType assertion, Predicate<AttributeType> predicate) {
        return assertion.getAttributeStatements().stream()
                .map(AttributeStatementType::getAttributes)
                .flatMap(Collection::stream)
                .map(AttributeStatementType.ASTChoiceType::getAttribute)
                .filter(predicate)
                .map(AttributeType::getAttributeValue)
                .flatMap(Collection::stream)
                .findFirst()
                .map(Object::toString)
                .orElse(null);
    }

    private String expectedPrincipalType() {
        SamlPrincipalType principalType = config.getPrincipalType();
        switch (principalType) {
            case SUBJECT:
                return principalType.name();
            case ATTRIBUTE:
            case FRIENDLY_ATTRIBUTE:
                return String.format("%s(%s)", principalType.name(), config.getPrincipalAttribute());
            default:
                return null;
        }
    }

    private NameIDType getSubjectNameID(final AssertionType assertion) {
        SubjectType subject = assertion.getSubject();
        SubjectType.STSubType subType = subject.getSubType();
        return subType != null ? (NameIDType) subType.getBaseID() : null;
    }

    private boolean validateInResponseToAttribute(ResponseType responseType, String expectedRequestId) {
        // If we are not expecting a request ID, don't bother
        if (expectedRequestId == null || expectedRequestId.isEmpty())
            return true;

        // We are expecting a request ID so we are in SP-initiated login, attribute InResponseTo must be present
        if (responseType.getInResponseTo() == null) {
            logger.error("Response Validation Error: InResponseTo attribute was expected but not present in received response");
            return false;
        }

        // Attribute is present, proceed with validation
        // 1) Attribute Response > InResponseTo must not be empty
        String responseInResponseToValue = responseType.getInResponseTo();
        if (responseInResponseToValue.isEmpty()) {
            logger.error("Response Validation Error: InResponseTo attribute was expected but it is empty in received response");
            return false;
        }

        // 2) Attribute Response > InResponseTo must match request ID
        if (!responseInResponseToValue.equals(expectedRequestId)) {
            logger.error("Response Validation Error: received InResponseTo attribute does not match the expected request ID");
            return false;
        }

        // If present, Assertion > Subject > Confirmation > SubjectConfirmationData > InResponseTo must also be validated
        if (responseType.getAssertions().isEmpty())
            return true;

        SubjectType subjectElement = responseType.getAssertions().get(0).getAssertion().getSubject();
        if (subjectElement != null) {
            if (subjectElement.getConfirmation() != null && !subjectElement.getConfirmation().isEmpty())
            {
                SubjectConfirmationType subjectConfirmationElement = subjectElement.getConfirmation().get(0);

                if (subjectConfirmationElement != null) {
                    SubjectConfirmationDataType subjectConfirmationDataElement = subjectConfirmationElement.getSubjectConfirmationData();

                    if (subjectConfirmationDataElement != null) {
                        if (subjectConfirmationDataElement.getInResponseTo() != null) {
                            // 3) Assertion > Subject > Confirmation > SubjectConfirmationData > InResponseTo is empty
                            String subjectConfirmationDataInResponseToValue = subjectConfirmationDataElement.getInResponseTo();
                            if (subjectConfirmationDataInResponseToValue.isEmpty()) {
                                logger.error("Response Validation Error: SubjectConfirmationData InResponseTo attribute was expected but it is empty in received response");
                                return false;
                            }

                            // 4) Assertion > Subject > Confirmation > SubjectConfirmationData > InResponseTo does not match request ID
                            if (!subjectConfirmationDataInResponseToValue.equals(expectedRequestId)) {
                                logger.error("Response Validation Error: received SubjectConfirmationData InResponseTo attribute does not match the expected request ID");
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }
}
