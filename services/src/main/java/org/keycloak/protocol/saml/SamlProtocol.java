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

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.keycloak.broker.saml.SAMLDataMarshaller;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.protocol.ArtifactResponseType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.saml.mappers.NameIdMapperHelper;
import org.keycloak.protocol.saml.mappers.SAMLAttributeStatementMapper;
import org.keycloak.protocol.saml.mappers.SAMLLoginResponseMapper;
import org.keycloak.protocol.saml.mappers.SAMLNameIdMapper;
import org.keycloak.protocol.saml.mappers.SAMLRoleListMapper;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;
import org.keycloak.saml.SAML2ErrorResponseBuilder;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.SAML2NameIDBuilder;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder.NodeGenerator;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.XmlKeyInfoKeyNameTransformer;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.w3c.dom.Document;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlProtocol implements LoginProtocol {
    public static final String ATTRIBUTE_TRUE_VALUE = "true";
    public static final String ATTRIBUTE_FALSE_VALUE = "false";
    public static final String SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE = "saml_assertion_consumer_url_post";
    public static final String SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE = "saml_assertion_consumer_url_redirect";
    public static final String SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE = "saml_artifact_binding_url";
    public static final String SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE = "saml_single_logout_service_url_post";
    public static final String SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE = "saml_single_logout_service_url_artifact";
    public static final String SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE = "saml_single_logout_service_url_redirect";
    public static final String SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE = "saml_artifact_resolution_service_url";
    public static final String LOGIN_PROTOCOL = "saml";
    public static final String SAML_BINDING = "saml_binding";
    public static final String SAML_IDP_INITIATED_LOGIN = "saml_idp_initiated_login";
    public static final String SAML_POST_BINDING = "post";
    public static final String SAML_SOAP_BINDING = "soap";
    public static final String SAML_REDIRECT_BINDING = "get";
    public static final String SAML_REQUEST_ID = "SAML_REQUEST_ID";
    public static final String SAML_REQUEST_ID_BROKER = "SAML_REQUEST_ID_BROKER";
    public static final String SAML_LOGOUT_BINDING = "saml.logout.binding";
    public static final String SAML_LOGOUT_ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO = "saml.logout.addExtensionsElementWithKeyInfo";
    public static final String SAML_SERVER_SIGNATURE_KEYINFO_KEY_NAME_TRANSFORMER = "SAML_SERVER_SIGNATURE_KEYINFO_KEY_NAME_TRANSFORMER";
    public static final String SAML_LOGOUT_REQUEST_ID = "SAML_LOGOUT_REQUEST_ID";
    public static final String SAML_LOGOUT_RELAY_STATE = "SAML_LOGOUT_RELAY_STATE";
    public static final String SAML_LOGOUT_CANONICALIZATION = "SAML_LOGOUT_CANONICALIZATION";
    public static final String SAML_LOGOUT_BINDING_URI = "SAML_LOGOUT_BINDING_URI";
    public static final String SAML_LOGOUT_SIGNATURE_ALGORITHM = "saml.logout.signature.algorithm";
    public static final String SAML_NAME_ID = "SAML_NAME_ID";
    public static final String SAML_NAME_ID_FORMAT = "SAML_NAME_ID_FORMAT";
    public static final String SAML_DEFAULT_NAMEID_FORMAT = JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get();
    public static final String SAML_PERSISTENT_NAME_ID_FOR = "saml.persistent.name.id.for";
    public static final String SAML_IDP_INITIATED_SSO_RELAY_STATE = "saml_idp_initiated_sso_relay_state";
    public static final String SAML_IDP_INITIATED_SSO_URL_NAME = "saml_idp_initiated_sso_url_name";
    public static final String SAML_LOGIN_REQUEST_FORCEAUTHN = "SAML_LOGIN_REQUEST_FORCEAUTHN";
    public static final String SAML_FORCEAUTHN_REQUIREMENT = "true";
    public static final String SAML_LOGOUT_INITIATOR_CLIENT_ID = "SAML_LOGOUT_INITIATOR_CLIENT_ID";
    public static final String USER_SESSION_ID = "userSessionId";
    public static final String CLIENT_SESSION_ID = "clientSessionId";


    protected static final Logger logger = Logger.getLogger(SamlProtocol.class);

    protected KeycloakSession session;

    protected RealmModel realm;

    protected UriInfo uriInfo;

    protected HttpHeaders headers;

    protected EventBuilder event;

    protected ArtifactResolver artifactResolver;
    protected SingleUseObjectProvider singleUseStore;

    @Override
    public SamlProtocol setSession(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public SamlProtocol setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public SamlProtocol setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    @Override
    public SamlProtocol setHttpHeaders(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public SamlProtocol setEventBuilder(EventBuilder event) {
        this.event = event;
        return this;
    }

    private ArtifactResolver getArtifactResolver() {
        if (artifactResolver == null) {
            artifactResolver = session.getProvider(ArtifactResolver.class);
        }
        return artifactResolver;
    }

    private SingleUseObjectProvider getSingleUseStore() {
        if (singleUseStore == null) {
            singleUseStore = session.getProvider(SingleUseObjectProvider.class);
        }
        return singleUseStore;
    }

    @Override
    public Response sendError(AuthenticationSessionModel authSession, Error error) {
        try {
            ClientModel client = authSession.getClient();

            if ("true".equals(authSession.getClientNote(SAML_IDP_INITIATED_LOGIN))) {
                if (error == Error.CANCELLED_BY_USER) {
                    UriBuilder builder = RealmsResource.protocolUrl(uriInfo).path(SamlService.class, "idpInitiatedSSO");
                    Map<String, String> params = new HashMap<>();
                    params.put("realm", realm.getName());
                    params.put("protocol", LOGIN_PROTOCOL);
                    params.put("client", client.getAttribute(SAML_IDP_INITIATED_SSO_URL_NAME));
                    URI redirect = builder.buildFromMap(params);
                    return Response.status(302).location(redirect).build();
                } else {
                    return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, translateErrorToIdpInitiatedErrorMessage(error));
                }
            } else {
                return samlErrorMessage(
                        authSession, new SamlClient(client), isPostBinding(authSession),
                        authSession.getRedirectUri(), translateErrorToSAMLStatus(error), authSession.getClientNote(GeneralConstants.RELAY_STATE)
                );
            }
        } finally {
            new AuthenticationSessionManager(session).removeAuthenticationSession(realm, authSession, true);
        }
    }

    private Response samlErrorMessage(
            AuthenticationSessionModel authSession, SamlClient samlClient, boolean isPostBinding,
            String destination, JBossSAMLURIConstants statusDetail, String relayState) {
        JaxrsSAML2BindingBuilder binding = new JaxrsSAML2BindingBuilder(session).relayState(relayState);
        SAML2ErrorResponseBuilder builder = new SAML2ErrorResponseBuilder().destination(destination).issuer(getResponseIssuer(realm)).status(statusDetail.get());
        KeyManager keyManager = session.keys();
        if (samlClient.requiresRealmSignature()) {
            KeyManager.ActiveRsaKey keys = keyManager.getActiveRsaKey(realm);
            String keyName = samlClient.getXmlSigKeyInfoKeyNameTransformer().getKeyName(keys.getKid(), keys.getCertificate());
            String canonicalization = samlClient.getCanonicalizationMethod();
            if (canonicalization != null) {
                binding.canonicalizationMethod(canonicalization);
            }
            binding.signatureAlgorithm(samlClient.getSignatureAlgorithm()).signWith(keyName, keys.getPrivateKey(), keys.getPublicKey(), keys.getCertificate()).signDocument();
        }

        try {
            // There is no support for encrypting status messages in SAML.
            // Only assertions, attributes, base ID and name ID can be encrypted
            // See Chapter 6 of saml-core-2.0-os.pdf
            Document document = builder.buildDocument();
            return buildErrorResponse(isPostBinding, destination, binding, document);
        } catch (Exception e) {
            return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.FAILED_TO_PROCESS_RESPONSE);
        }
    }

    protected Response buildErrorResponse(boolean isPostBinding, String destination, JaxrsSAML2BindingBuilder binding, Document document) throws ConfigurationException, ProcessingException, IOException {
        if (isPostBinding) {
            return binding.postBinding(document).response(destination);
        } else {
            return binding.redirectBinding(document).response(destination);
        }
    }

    private JBossSAMLURIConstants translateErrorToSAMLStatus(Error error) {
        switch (error) {
        case CANCELLED_BY_USER:
        case CANCELLED_AIA:
        case CONSENT_DENIED:
            return JBossSAMLURIConstants.STATUS_REQUEST_DENIED;
        case PASSIVE_INTERACTION_REQUIRED:
        case PASSIVE_LOGIN_REQUIRED:
            return JBossSAMLURIConstants.STATUS_NO_PASSIVE;
        default:
            logger.warn("Untranslated protocol Error: " + error.name() + " so we return default SAML error");
            return JBossSAMLURIConstants.STATUS_REQUEST_DENIED;
        }
    }

    private String translateErrorToIdpInitiatedErrorMessage(Error error) {
        switch (error) {
        case CONSENT_DENIED:
            return Messages.CONSENT_DENIED;
        case PASSIVE_INTERACTION_REQUIRED:
        case PASSIVE_LOGIN_REQUIRED:
            return Messages.UNEXPECTED_ERROR_HANDLING_REQUEST;
        default:
            logger.warn("Untranslated protocol Error: " + error.name() + " so we return default error message");
            return Messages.UNEXPECTED_ERROR_HANDLING_REQUEST;
        }
    }

    protected String getResponseIssuer(RealmModel realm) {
        return RealmsResource.realmBaseUrl(uriInfo).build(realm.getName()).toString();
    }

    protected boolean isPostBinding(AuthenticationSessionModel authSession) {
        ClientModel client = authSession.getClient();
        SamlClient samlClient = new SamlClient(client);
        return SamlProtocol.SAML_POST_BINDING.equals(authSession.getClientNote(SamlProtocol.SAML_BINDING)) || samlClient.forcePostBinding();
    }

    protected boolean isPostBinding(AuthenticatedClientSessionModel clientSession) {
        ClientModel client = clientSession.getClient();
        SamlClient samlClient = new SamlClient(client);
        return SamlProtocol.SAML_POST_BINDING.equals(clientSession.getNote(SamlProtocol.SAML_BINDING)) || samlClient.forcePostBinding();
    }

    public static boolean isLogoutPostBindingForInitiator(UserSessionModel session) {
        String note = session.getNote(SamlProtocol.SAML_LOGOUT_BINDING);
        return SamlProtocol.SAML_POST_BINDING.equals(note);
    }

    protected boolean isLogoutPostBindingForClient(AuthenticatedClientSessionModel clientSession) {
        ClientModel client = clientSession.getClient();
        SamlClient samlClient = new SamlClient(client);
        String logoutPostUrl = client.getAttribute(SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE);
        String logoutRedirectUrl = client.getAttribute(SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE);

        if (logoutPostUrl == null || logoutPostUrl.trim().isEmpty()) {
            // if we don't have a redirect uri either, return true and default to the admin url + POST binding
            return (logoutRedirectUrl == null || logoutRedirectUrl.trim().isEmpty());
        }

        if (samlClient.forcePostBinding()) {
            return true; // configured to force a post binding and post binding logout url is not null
        }

        String bindingType = clientSession.getNote(SAML_BINDING);

        // if the login binding was POST, return true
        if (SAML_POST_BINDING.equals(bindingType))
            return true;

        // true if we don't have a redirect binding url, so use post binding, false for redirect binding
        return (logoutRedirectUrl == null || logoutRedirectUrl.trim().isEmpty());
    }

    protected String getNameIdFormat(SamlClient samlClient, AuthenticationSessionModel authSession) {
        String nameIdFormat = authSession.getClientNote(GeneralConstants.NAMEID_FORMAT);

        boolean forceFormat = samlClient.forceNameIDFormat();
        String configuredNameIdFormat = samlClient.getNameIDFormat();
        if ((nameIdFormat == null || forceFormat) && configuredNameIdFormat != null) {
            nameIdFormat = configuredNameIdFormat;
        }
        if (nameIdFormat == null)
            return SAML_DEFAULT_NAMEID_FORMAT;
        return nameIdFormat;
    }

    protected String getNameId(String nameIdFormat, CommonClientSessionModel clientSession, UserSessionModel userSession) {
        if (nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get())) {
            final String email = userSession.getUser().getEmail();
            if (email == null) {
                logger.debugf("E-mail of the user %s has to be set for %s NameIDFormat", userSession.getUser().getUsername(), JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get());
            }
            return email;
        } else if (nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT.get())) {
            // "G-" stands for "generated" Add this for the slight possibility of collisions.
            return "G-" + UUID.randomUUID().toString();
        } else if (nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get())) {
            return getPersistentNameId(clientSession, userSession);
        } else if (nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get())) {
            // TODO: Support for persistent NameID (pseudo-random identifier persisted in user object)
            return userSession.getUser().getUsername();
        } else {
            return userSession.getUser().getUsername();
        }
    }

    /**
     * Attempts to retrieve the persistent type NameId as follows:
     *
     * <ol>
     * <li>saml.persistent.name.id.for.$clientId user attribute</li>
     * <li>saml.persistent.name.id.for.* user attribute</li>
     * <li>G-$randomUuid</li>
     * </ol>
     * <p>
     * If a randomUuid is generated, an attribute for the given saml.persistent.name.id.for.$clientId will be generated,
     * otherwise no state change will occur with respect to the user's attributes.
     *
     * @return the user's persistent NameId
     */
    protected String getPersistentNameId(final CommonClientSessionModel clientSession, final UserSessionModel userSession) {
        // attempt to retrieve the UserID for the client-specific attribute
        final UserModel user = userSession.getUser();
        final String clientNameId = String.format("%s.%s", SAML_PERSISTENT_NAME_ID_FOR,
                clientSession.getClient().getClientId());
        String samlPersistentNameId = user.getFirstAttribute(clientNameId);
        if (samlPersistentNameId != null) {
            return samlPersistentNameId;
        }

        // check for a wildcard attribute
        final String wildcardNameId = String.format("%s.*", SAML_PERSISTENT_NAME_ID_FOR);
        samlPersistentNameId = user.getFirstAttribute(wildcardNameId);
        if (samlPersistentNameId != null) {
            return samlPersistentNameId;
        }

        // default to generated.  "G-" stands for "generated"
        samlPersistentNameId = "G-" + UUID.randomUUID().toString();
        user.setSingleAttribute(clientNameId, samlPersistentNameId);
        return samlPersistentNameId;
    }

    @Override
    public Response authenticated(AuthenticationSessionModel authSession, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        ClientModel client = clientSession.getClient();
        SamlClient samlClient = new SamlClient(client);
        String requestID = authSession.getClientNote(SAML_REQUEST_ID);
        String relayState = authSession.getClientNote(GeneralConstants.RELAY_STATE);
        String redirectUri = authSession.getRedirectUri();
        String responseIssuer = getResponseIssuer(realm);
        String nameIdFormat = getNameIdFormat(samlClient, authSession);

        int assertionLifespan = samlClient.getAssertionLifespan();
        SAML2LoginResponseBuilder builder = new SAML2LoginResponseBuilder();
        builder.requestID(requestID)
                .destination(redirectUri)
                .issuer(responseIssuer)
                .assertionExpiration(assertionLifespan <= 0? realm.getAccessCodeLifespan() : assertionLifespan)
                .subjectExpiration(assertionLifespan <= 0? realm.getAccessTokenLifespan() : assertionLifespan)
                .sessionExpiration(realm.getSsoSessionMaxLifespan())
                .requestIssuer(clientSession.getClient().getClientId())
                .authMethod(JBossSAMLURIConstants.AC_UNSPECIFIED.get());

        String sessionIndex = SamlSessionUtils.getSessionIndex(clientSession);
        builder.sessionIndex(sessionIndex);

        if (!samlClient.includeAuthnStatement()) {
            builder.disableAuthnStatement(true);
        }

        builder.includeOneTimeUseCondition(samlClient.includeOneTimeUseCondition());

        List<ProtocolMapperProcessor<SAMLAttributeStatementMapper>> attributeStatementMappers = new LinkedList<>();
        List<ProtocolMapperProcessor<SAMLLoginResponseMapper>> loginResponseMappers = new LinkedList<>();
        AtomicReference<ProtocolMapperProcessor<SAMLRoleListMapper>> roleListMapper = new AtomicReference<>(null);
        List<ProtocolMapperProcessor<SAMLNameIdMapper>> samlNameIdMappers = new LinkedList<>();

        ProtocolMapperUtils.getSortedProtocolMappers(session, clientSessionCtx)
                .forEach(entry -> {
                    ProtocolMapperModel mapping = entry.getKey();
                    ProtocolMapper mapper = entry.getValue();

                    if (mapper instanceof SAMLAttributeStatementMapper) {
                        attributeStatementMappers.add(new ProtocolMapperProcessor<>((SAMLAttributeStatementMapper) mapper, mapping));
                    }
                    if (mapper instanceof SAMLLoginResponseMapper) {
                        loginResponseMappers.add(new ProtocolMapperProcessor<>((SAMLLoginResponseMapper) mapper, mapping));
                    }
                    if (mapper instanceof SAMLRoleListMapper) {
                        roleListMapper.set(new ProtocolMapperProcessor<>((SAMLRoleListMapper) mapper, mapping));
                    }
                    if (mapper instanceof SAMLNameIdMapper) {
                        samlNameIdMappers.add(new ProtocolMapperProcessor<>((SAMLNameIdMapper) mapper, mapping));
                    }
                });

        Document samlDocument = null;
        ResponseType samlModel = null;
        KeyManager keyManager = session.keys();
        KeyManager.ActiveRsaKey keys = keyManager.getActiveRsaKey(realm);
        boolean postBinding = isPostBinding(authSession);
        String keyName = samlClient.getXmlSigKeyInfoKeyNameTransformer().getKeyName(keys.getKid(), keys.getCertificate());
        String nameId = getSAMLNameId(samlNameIdMappers, nameIdFormat, session, userSession, clientSession);

        if (nameId == null) {
            return samlErrorMessage(null, samlClient, isPostBinding(authSession), redirectUri,
                    JBossSAMLURIConstants.STATUS_INVALID_NAMEIDPOLICY, relayState);
        }

        builder.nameIdentifier(nameIdFormat, nameId);

        // save NAME_ID and format in clientSession as they may be persistent or
        // transient or email and not username
        // we'll need to send this back on a logout
        clientSession.setNote(SAML_NAME_ID, nameId);
        clientSession.setNote(SAML_NAME_ID_FORMAT, nameIdFormat);

        try {
            if ((!postBinding) && samlClient.requiresRealmSignature() && samlClient.addExtensionsElementWithKeyInfo()) {
                builder.addExtension(new KeycloakKeySamlExtensionGenerator(keyName));
            }

            samlModel = builder.buildModel();
            final AttributeStatementType attributeStatement = populateAttributeStatements(attributeStatementMappers, session, userSession, clientSession);
            populateRoles(roleListMapper.get(), session, userSession, clientSessionCtx, attributeStatement);

            // SAML Spec 2.7.3 AttributeStatement must contain one or more Attribute or EncryptedAttribute
            if (attributeStatement.getAttributes().size() > 0) {
                AssertionType assertion = samlModel.getAssertions().get(0).getAssertion();
                assertion.addStatement(attributeStatement);
            }

            samlModel = transformLoginResponse(loginResponseMappers, samlModel, session, userSession, clientSessionCtx);
        } catch (Exception e) {
            logger.error("failed", e);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.FAILED_TO_PROCESS_RESPONSE);
        }

        JaxrsSAML2BindingBuilder bindingBuilder = new JaxrsSAML2BindingBuilder(session);
        bindingBuilder.relayState(relayState);

        if ("true".equals(clientSession.getNote(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.get()))) {
            try {
                return buildArtifactAuthenticatedResponse(clientSession, redirectUri, samlModel, bindingBuilder);
            } catch (Exception e) {
                logger.error("failed", e);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.FAILED_TO_PROCESS_RESPONSE);
            }
        }

        if (samlClient.requiresRealmSignature() || samlClient.requiresAssertionSignature()) {
            String canonicalization = samlClient.getCanonicalizationMethod();
            if (canonicalization != null) {
                bindingBuilder.canonicalizationMethod(canonicalization);
            }
            bindingBuilder.signatureAlgorithm(samlClient.getSignatureAlgorithm()).signWith(keyName, keys.getPrivateKey(), keys.getPublicKey(), keys.getCertificate());

            if (samlClient.requiresRealmSignature()) bindingBuilder.signDocument();
            if (samlClient.requiresAssertionSignature()) bindingBuilder.signAssertions();
        }

        if (samlClient.requiresEncryption()) {
            PublicKey publicKey = null;
            try {
                publicKey = SamlProtocolUtils.getEncryptionKey(client);
            } catch (Exception e) {
                logger.error("failed", e);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.FAILED_TO_PROCESS_RESPONSE);
            }
            bindingBuilder.encrypt(publicKey);
        }
        try {
            samlDocument = builder.buildDocument(samlModel);
            return buildAuthenticatedResponse(clientSession, redirectUri, samlDocument, bindingBuilder);
        } catch (Exception e) {
            logger.error("failed", e);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.FAILED_TO_PROCESS_RESPONSE);
        }
    }

    protected Response buildAuthenticatedResponse(AuthenticatedClientSessionModel clientSession, String redirectUri, Document samlDocument, JaxrsSAML2BindingBuilder bindingBuilder) throws ConfigurationException, ProcessingException, IOException {
        if (isPostBinding(clientSession)) {
            return bindingBuilder.postBinding(samlDocument).response(redirectUri);
        } else {
            return bindingBuilder.redirectBinding(samlDocument).response(redirectUri);
        }
    }

    public static class ProtocolMapperProcessor<T> {
        public final T mapper;
        public final ProtocolMapperModel model;

        public ProtocolMapperProcessor(T mapper, ProtocolMapperModel model) {
            this.mapper = mapper;
            this.model = model;
        }
    }

    public AttributeStatementType populateAttributeStatements(List<ProtocolMapperProcessor<SAMLAttributeStatementMapper>> attributeStatementMappers, KeycloakSession session, UserSessionModel userSession,
                                                              AuthenticatedClientSessionModel clientSession) {
        AttributeStatementType attributeStatement = new AttributeStatementType();
        for (ProtocolMapperProcessor<SAMLAttributeStatementMapper> processor : attributeStatementMappers) {
            processor.mapper.transformAttributeStatement(attributeStatement, processor.model, session, userSession, clientSession);
        }

        return attributeStatement;
    }

    public ResponseType transformLoginResponse(List<ProtocolMapperProcessor<SAMLLoginResponseMapper>> mappers, ResponseType response,
            KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        for (ProtocolMapperProcessor<SAMLLoginResponseMapper> processor : mappers) {
            response = processor.mapper.transformLoginResponse(response, processor.model, session, userSession, clientSessionCtx);
        }

        for (Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext(); ) {
            response = (ResponseType) it.next().beforeSendingResponse(response, clientSessionCtx.getClientSession());
        }

        return response;
    }

    public void populateRoles(ProtocolMapperProcessor<SAMLRoleListMapper> roleListMapper, KeycloakSession session, UserSessionModel userSession,
                              ClientSessionContext clientSessionCtx, final AttributeStatementType existingAttributeStatement) {
        if (roleListMapper == null)
            return;
        roleListMapper.mapper.mapRoles(existingAttributeStatement, roleListMapper.model, session, userSession, clientSessionCtx);
    }

    protected String getSAMLNameId(
            List<ProtocolMapperProcessor<SAMLNameIdMapper>> samlNameIdMappers, String nameIdFormat, KeycloakSession session,
                                    UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        for (ProtocolMapperProcessor<SAMLNameIdMapper> nameIdMap : samlNameIdMappers) {
            if(nameIdFormat.equals(nameIdMap.model.getConfig().get(NameIdMapperHelper.MAPPER_NAMEID_FORMAT))) {
                return nameIdMap.mapper.mapperNameId(nameIdFormat, nameIdMap.model, session, userSession, clientSession);
            }
        }
        return getNameId(nameIdFormat, clientSession, userSession);
    }

    public static String getLogoutServiceUrl(KeycloakSession session, ClientModel client, String bindingType, boolean backChannelLogout) {
        String logoutServiceUrl = null;
        // backchannel logout doesn't support sending artifacts
        if (!backChannelLogout && useArtifactForLogout(client)) {
            logoutServiceUrl = client.getAttribute(SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE);
        } else if (SAML_POST_BINDING.equals(bindingType)) {
            logoutServiceUrl = client.getAttribute(SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE);
        } else {
            logoutServiceUrl = client.getAttribute(SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE);
        }

        if (logoutServiceUrl == null)
            logoutServiceUrl = client.getManagementUrl();
        if (logoutServiceUrl == null || logoutServiceUrl.trim().equals(""))
            return null;
        return ResourceAdminManager.resolveUri(session, client.getRootUrl(), logoutServiceUrl);
    }
    
    public static boolean useArtifactForLogout(ClientModel client) {
        return new SamlClient(client).forceArtifactBinding()
                && client.getAttribute(SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE) != null;
    }

    @Override
    public Response frontchannelLogout(UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        ClientModel client = clientSession.getClient();
        SamlClient samlClient = new SamlClient(client);
        try {
            boolean postBinding = isLogoutPostBindingForClient(clientSession);
            String bindingUri = getLogoutServiceUrl(session, client, postBinding ? SAML_POST_BINDING : SAML_REDIRECT_BINDING, false);
            if (bindingUri == null) {
                logger.warnf("Failed to logout client %s, skipping this client.  Please configure the logout service url in the admin console for your client applications.", client.getClientId());
                return null;
            }

            NodeGenerator[] extensions = new NodeGenerator[]{};
            if (!postBinding) {
                if (samlClient.requiresRealmSignature() && samlClient.addExtensionsElementWithKeyInfo()) {
                    KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);
                    String keyName = samlClient.getXmlSigKeyInfoKeyNameTransformer().getKeyName(keys.getKid(), keys.getCertificate());
                    extensions = new NodeGenerator[]{new KeycloakKeySamlExtensionGenerator(keyName)};
                }
            }
            LogoutRequestType logoutRequest = createLogoutRequest(bindingUri, clientSession, client, extensions);
            JaxrsSAML2BindingBuilder binding = createBindingBuilder(samlClient, "true".equals(clientSession.getNote(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.get())));

            //If this session uses artifact binding, send an artifact instead of the LogoutRequest
            if ("true".equals(clientSession.getNote(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.get()))
                    && useArtifactForLogout(client)) {
                clientSession.setAction(CommonClientSessionModel.Action.LOGGING_OUT.name());
                return buildArtifactAuthenticatedResponse(clientSession, bindingUri, logoutRequest, binding);
            }

            Document samlDocument = SAML2Request.convert(logoutRequest);
            if (postBinding) {
                // This is POST binding, hence KeyID is included in dsig:KeyInfo/dsig:KeyName, no need to add <samlp:Extensions> element
                return binding.postBinding(samlDocument).request(bindingUri);
            } else {
                logger.debug("frontchannel redirect binding");
                return binding.redirectBinding(samlDocument).request(bindingUri);
            }
        } catch (ConfigurationException | ProcessingException | IOException | ParsingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response finishBrowserLogout(UserSessionModel userSession, AuthenticationSessionModel logoutSession) {
        logger.debug("finishLogout");
        String logoutBindingUri = userSession.getNote(SAML_LOGOUT_BINDING_URI);
        if (logoutBindingUri == null) {
            logger.error("Can't finish SAML logout as there is no logout binding set.  Please configure the logout service url in the admin console for your client applications.");
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.FAILED_LOGOUT);

        }
        String logoutRelayState = userSession.getNote(SAML_LOGOUT_RELAY_STATE);
        SAML2LogoutResponseBuilder builder = new SAML2LogoutResponseBuilder();
        builder.logoutRequestID(userSession.getNote(SAML_LOGOUT_REQUEST_ID));
        builder.destination(logoutBindingUri);
        builder.issuer(getResponseIssuer(realm));
        JaxrsSAML2BindingBuilder binding = new JaxrsSAML2BindingBuilder(session);
        binding.relayState(logoutRelayState);
        String signingAlgorithm = userSession.getNote(SAML_LOGOUT_SIGNATURE_ALGORITHM);
        boolean postBinding = isLogoutPostBindingForInitiator(userSession);
        if (signingAlgorithm != null) {
            SignatureAlgorithm algorithm = SignatureAlgorithm.valueOf(signingAlgorithm);
            String canonicalization = userSession.getNote(SAML_LOGOUT_CANONICALIZATION);
            if (canonicalization != null) {
                binding.canonicalizationMethod(canonicalization);
            }
            KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);
            XmlKeyInfoKeyNameTransformer transformer = XmlKeyInfoKeyNameTransformer.from(
                    userSession.getNote(SAML_SERVER_SIGNATURE_KEYINFO_KEY_NAME_TRANSFORMER),
                    SamlClient.DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER);
            String keyName = transformer.getKeyName(keys.getKid(), keys.getCertificate());
            binding.signatureAlgorithm(algorithm).signWith(keyName, keys.getPrivateKey(), keys.getPublicKey(), keys.getCertificate()).signDocument();
            boolean addExtension = (!postBinding) && Objects.equals("true", userSession.getNote(SamlProtocol.SAML_LOGOUT_ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO));
            if (addExtension) {    // Only include extension if REDIRECT binding and signing whole SAML protocol message
                builder.addExtension(new KeycloakKeySamlExtensionGenerator(keyName));
            }
        }
        Response response;
        try {
            response = buildLogoutResponse(userSession, logoutBindingUri, builder, binding);
        } catch (ConfigurationException | ProcessingException | IOException e) {
            throw new RuntimeException(e);
        }
        if (logoutBindingUri != null) {
            event.detail(Details.REDIRECT_URI, logoutBindingUri);
        }
        event.event(EventType.LOGOUT)
                .detail(Details.AUTH_METHOD, userSession.getAuthMethod())
                .client(session.getContext().getClient())
                .user(userSession.getUser())
                .session(userSession)
                .detail(Details.USERNAME, userSession.getLoginUsername())
                .detail(Details.RESPONSE_MODE, postBinding ? SamlProtocol.SAML_POST_BINDING : SamlProtocol.SAML_REDIRECT_BINDING)
                .detail(SamlProtocol.SAML_LOGOUT_REQUEST_ID, userSession.getNote(SAML_LOGOUT_REQUEST_ID))
                .success();
        return response;
    }

    protected Response buildLogoutResponse(UserSessionModel userSession, String logoutBindingUri, SAML2LogoutResponseBuilder builder, JaxrsSAML2BindingBuilder binding) throws ConfigurationException, ProcessingException, IOException {

        //if artifact binding is used, send an artifact instead of the LogoutResponse
        if ("true".equals(userSession.getNote(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.get()))) {
            return buildLogoutArtifactResponse(userSession, logoutBindingUri, builder.buildModel(), binding);
        }

        Document samlDocument = builder.buildDocument();

        if (isLogoutPostBindingForInitiator(userSession)) {
            return binding.postBinding(samlDocument).response(logoutBindingUri);
        } else {
            return binding.redirectBinding(samlDocument).response(logoutBindingUri);
        }
    }

    @Override
    public Response backchannelLogout(UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        ClientModel client = clientSession.getClient();
        SamlClient samlClient = new SamlClient(client);
        String logoutUrl = getLogoutServiceUrl(session, client, SAML_POST_BINDING, true);
        if (logoutUrl == null) {
            logger.warnf("Can't do backchannel logout. No SingleLogoutService POST Binding registered for client: %s",
                    client.getClientId());
            return Response.serverError().build();
        }

        String logoutRequestString = null;
        try {
            LogoutRequestType logoutRequest = createLogoutRequest(logoutUrl, clientSession, client);
            JaxrsSAML2BindingBuilder binding = createBindingBuilder(samlClient, false);
            // This is POST binding, hence KeyID is included in dsig:KeyInfo/dsig:KeyName, no need to add <samlp:Extensions> element
            logoutRequestString = binding.postBinding(SAML2Request.convert(logoutRequest)).encoded();
        } catch (Exception e) {
            logger.warn("failed to send saml logout", e);
            return Response.serverError().build();
        }

        CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        for (int i = 0; i < 2; i++) { // follow redirects once
            try {
                List<NameValuePair> formparams = new ArrayList<NameValuePair>();
                formparams.add(new BasicNameValuePair(GeneralConstants.SAML_REQUEST_KEY, logoutRequestString));
                formparams.add(new BasicNameValuePair("BACK_CHANNEL_LOGOUT", "BACK_CHANNEL_LOGOUT")); // for Picketlink
                // todo remove
                // this
                UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
                HttpPost post = new HttpPost(logoutUrl);
                post.setEntity(form);
                try (CloseableHttpResponse response = httpClient.execute(post)) {
                    try {
                        int status = response.getStatusLine().getStatusCode();
                        if (status == 302 && !logoutUrl.endsWith("/")) {
                            String redirect = response.getFirstHeader(HttpHeaders.LOCATION).getValue();
                            String withSlash = logoutUrl + "/";
                            if (withSlash.equals(redirect)) {
                                logoutUrl = withSlash;
                                continue;
                            }
                        }
                    } finally {
                        EntityUtils.consumeQuietly(response.getEntity());
                    }
                }
            } catch (IOException e) {
                logger.warn("failed to send saml logout", e);
                return Response.serverError().build();
            }
            break;
        }
        return Response.ok().build();
    }

    protected LogoutRequestType createLogoutRequest(String logoutUrl, AuthenticatedClientSessionModel clientSession, ClientModel client, NodeGenerator... extensions) throws ConfigurationException {
        // build userPrincipal with subject used at login
        SAML2LogoutRequestBuilder logoutBuilder = new SAML2LogoutRequestBuilder().assertionExpiration(realm.getAccessCodeLifespan()).issuer(getResponseIssuer(realm))
                .userPrincipal(clientSession.getNote(SAML_NAME_ID), clientSession.getNote(SAML_NAME_ID_FORMAT)).destination(logoutUrl);

        String sessionIndex = SamlSessionUtils.getSessionIndex(clientSession);
        logoutBuilder.sessionIndex(sessionIndex);

        for (NodeGenerator extension : extensions) {
            logoutBuilder.addExtension(extension);
        }
        LogoutRequestType logoutRequest = logoutBuilder.createLogoutRequest();
        for (Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext(); ) {
            logoutRequest = it.next().beforeSendingLogoutRequest(logoutRequest, clientSession.getUserSession(), clientSession);
        }

        return logoutRequest;
    }

    @Override
    public boolean requireReauthentication(UserSessionModel userSession, AuthenticationSessionModel authSession) {
        String requireReauthentication = authSession.getAuthNote(SamlProtocol.SAML_LOGIN_REQUEST_FORCEAUTHN);
        return Objects.equals(SamlProtocol.SAML_FORCEAUTHN_REQUIREMENT, requireReauthentication);
    }

    private JaxrsSAML2BindingBuilder createBindingBuilder(SamlClient samlClient, boolean skipRealmSignature) {
        JaxrsSAML2BindingBuilder binding = new JaxrsSAML2BindingBuilder(session);
        if (!skipRealmSignature && samlClient.requiresRealmSignature()) {
            KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);
            String keyName = samlClient.getXmlSigKeyInfoKeyNameTransformer().getKeyName(keys.getKid(), keys.getCertificate());
            binding.signatureAlgorithm(samlClient.getSignatureAlgorithm()).signWith(keyName, keys.getPrivateKey(), keys.getPublicKey(), keys.getCertificate()).signDocument();
        }
        return binding;
    }

    @Override
    public void close() {

    }

    /**
     * This method, instead of sending the actual response with the token sends
     * the artifact message via post or redirect.
     *
     * @param clientSession  the current authenticated client session
     * @param redirectUri    the redirect uri to the client
     * @param samlDocument   a Document containing the saml Response
     * @param bindingBuilder the current JaxrsSAML2BindingBuilder configured with information for signing and encryption
     * @return A response (POSTed form or redirect) with a newly generated artifact
     * @throws ConfigurationException
     * @throws ProcessingException
     * @throws IOException
     */
    protected Response buildArtifactAuthenticatedResponse(AuthenticatedClientSessionModel clientSession,
                                                          String redirectUri, SAML2Object samlDocument,
                                                          JaxrsSAML2BindingBuilder bindingBuilder)
            throws ProcessingException, ConfigurationException {

        try {
            String artifact = buildArtifactAndStoreResponse(samlDocument, clientSession);
            String relayState = clientSession.getNote(GeneralConstants.RELAY_STATE);

            logger.debugf("Sending artifact %s to client %s", artifact, clientSession.getClient().getClientId());

            if (isPostBinding(clientSession)) {
                return artifactPost(redirectUri, artifact, relayState, bindingBuilder);
            } else {
                return artifactRedirect(redirectUri, artifact, relayState);
            }

        } catch (ArtifactResolverProcessingException e) {
            throw new ProcessingException(e);
        }
    }

    /**
     * This method, instead of sending the actual response with the token, sends
     * the artifact message via post or redirect. This method is only to be used for the final LogoutResponse.
     *
     * @param userSession    The current user session being logged out
     * @param redirectUri    the redirect uri to the client
     * @param statusResponseType   a Document containing the saml Response
     * @param bindingBuilder the current JaxrsSAML2BindingBuilder configured with information for signing and encryption
     * @return A response (POSTed form or redirect) with a newly generated artifact
     * @throws ProcessingException
     * @throws IOException
     */
    protected Response buildLogoutArtifactResponse(UserSessionModel userSession,
                                                   String redirectUri, StatusResponseType statusResponseType,
                                                   JaxrsSAML2BindingBuilder bindingBuilder)
            throws ProcessingException, ConfigurationException {

        try {
            String artifact = buildArtifactAndStoreResponse(statusResponseType, userSession);
            String relayState = userSession.getNote(SAML_LOGOUT_RELAY_STATE);

            logger.debugf("Sending artifact for LogoutResponse %s to user %s", artifact, userSession.getLoginUsername());

            if (isLogoutPostBindingForInitiator(userSession)) {
                return artifactPost(redirectUri, artifact, relayState, bindingBuilder);
            } else {
                return artifactRedirect(redirectUri, artifact, relayState);
            }

        } catch (ArtifactResolverProcessingException e) {
            throw new ProcessingException(e);
        }
    }
    
    protected String buildArtifactAndStoreResponse(SAML2Object statusResponseType, UserSessionModel userSession) throws ArtifactResolverProcessingException, ConfigurationException, ProcessingException {
        String clientIdThatInitiatedLogout = userSession.getNote(SAML_LOGOUT_INITIATOR_CLIENT_ID);
        userSession.removeNote(SAML_LOGOUT_INITIATOR_CLIENT_ID);

        AuthenticatedClientSessionModel clientSessionModel = userSession.getAuthenticatedClientSessionByClient(clientIdThatInitiatedLogout);
        if (clientSessionModel == null) {
            throw new IllegalStateException("Initiator client id is unknown when artifact response is created");
        }

        return buildArtifactAndStoreResponse(statusResponseType, clientSessionModel);
    }
    
    protected String buildArtifactAndStoreResponse(SAML2Object saml2Object, AuthenticatedClientSessionModel clientSessionModel) throws ArtifactResolverProcessingException, ProcessingException, ConfigurationException {
        String entityId = RealmsResource.realmBaseUrl(uriInfo).build(realm.getName()).toString();
        ArtifactResponseType artifactResponseType = SamlProtocolUtils.buildArtifactResponse(saml2Object, SAML2NameIDBuilder.value(getResponseIssuer(realm)).build());


        // Create artifact and store session mapping
        SAMLDataMarshaller marshaller = new SAMLDataMarshaller();
        String artifact = getArtifactResolver().buildArtifact(clientSessionModel, entityId, marshaller.serialize(artifactResponseType));
        HashMap<String, String> notes = new HashMap<>();
        notes.put(USER_SESSION_ID, clientSessionModel.getUserSession().getId());
        notes.put(CLIENT_SESSION_ID, clientSessionModel.getClient().getId());
        getSingleUseStore().put(artifact, realm.getAccessCodeLifespan(), notes);
        
        return artifact;
    }

    /**
     * Return an artifact through a redirect message
     *
     * @param redirectUri the redirect uri to the client
     * @param artifact    the artifact to send
     * @param relayState  the current relayState
     * @return a redirect Response with the artifact
     */
    private Response artifactRedirect(String redirectUri, String artifact, String relayState) {
        KeycloakUriBuilder builder = KeycloakUriBuilder.fromUri(redirectUri)
                .replaceQuery(null)
                .queryParam(GeneralConstants.SAML_ARTIFACT_KEY, artifact);

        if (relayState != null) {
            builder.queryParam(GeneralConstants.RELAY_STATE, relayState);
        }

        URI uri = builder.build();
        return Response.status(302).location(uri)
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache, no-store").build();
    }

    /**
     * Return an artifact through a POSTed form
     *
     * @param redirectUri    the redirect uri to the client
     * @param artifact       the artifact to send
     * @param relayState     current relayState
     * @param bindingBuilder the current JaxrsSAML2BindingBuilder configured with information for signing and encryption
     * @return a POSTed form response, with the artifact
     */
    private Response artifactPost(String redirectUri, String artifact, String relayState, JaxrsSAML2BindingBuilder bindingBuilder) {
        Map<String, String> inputTypes = new HashMap<>();
        inputTypes.put(GeneralConstants.SAML_ARTIFACT_KEY, artifact);
        if (relayState != null) {
            inputTypes.put(GeneralConstants.RELAY_STATE, relayState);
        }

        String str = bindingBuilder.buildHtmlForm(redirectUri, inputTypes);

        return Response.ok(str, MediaType.TEXT_HTML_TYPE)
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache, no-store").build();
    }

}
