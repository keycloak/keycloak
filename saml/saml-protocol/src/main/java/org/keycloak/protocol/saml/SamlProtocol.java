package org.keycloak.protocol.saml;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.saml.mappers.SAMLAttributeStatementMapper;
import org.keycloak.protocol.saml.mappers.SAMLLoginResponseMapper;
import org.keycloak.protocol.saml.mappers.SAMLRoleListMapper;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.admin.ClientAttributeCertificateResource;
import org.keycloak.services.resources.flows.Flows;
import org.picketlink.common.constants.GeneralConstants;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ParsingException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.identity.federation.saml.v2.assertion.AssertionType;
import org.picketlink.identity.federation.saml.v2.assertion.AttributeStatementType;
import org.picketlink.identity.federation.saml.v2.protocol.ResponseType;
import org.picketlink.identity.federation.web.handlers.saml2.SAML2LogOutHandler;
import org.w3c.dom.Document;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlProtocol implements LoginProtocol {
    protected static final Logger logger = Logger.getLogger(SamlProtocol.class);


    public static final String ATTRIBUTE_TRUE_VALUE = "true";
    public static final String ATTRIBUTE_FALSE_VALUE = "false";
    public static final String SAML_SIGNING_CERTIFICATE_ATTRIBUTE = "saml.signing." + ClientAttributeCertificateResource.X509CERTIFICATE;
    public static final String SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE = "saml.encryption." + ClientAttributeCertificateResource.X509CERTIFICATE;
    public static final String SAML_CLIENT_SIGNATURE_ATTRIBUTE = "saml.client.signature";
    public static final String SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE = "saml_assertion_consumer_url_post";
    public static final String SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE = "saml_assertion_consumer_url_redirect";
    public static final String SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE = "saml_single_logout_service_url_post";
    public static final String SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE = "saml_single_logout_service_url_redirect";
    public static final String SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE = "saml_force_name_id_format";
    public static final String SAML_NAME_ID_FORMAT_ATTRIBUTE = "saml_name_id_format";
    public static final String LOGIN_PROTOCOL = "saml";
    public static final String SAML_BINDING = "saml_binding";
    public static final String SAML_POST_BINDING = "post";
    public static final String SAML_REDIRECT_BINDING = "get";
    public static final String SAML_SERVER_SIGNATURE = "saml.server.signature";
    public static final String SAML_ASSERTION_SIGNATURE = "saml.assertion.signature";
    public static final String SAML_AUTHNSTATEMENT = "saml.authnstatement";
    public static final String SAML_MULTIVALUED_ROLES = "saml.multivalued.roles";
    public static final String SAML_SIGNATURE_ALGORITHM = "saml.signature.algorithm";
    public static final String SAML_ENCRYPT = "saml.encrypt";
    public static final String SAML_FORCE_POST_BINDING = "saml.force.post.binding";
    public static final String SAML_REQUEST_ID = "SAML_REQUEST_ID";
    public static final String SAML_LOGOUT_BINDING = "saml.logout.binding";
    public static final String SAML_LOGOUT_ISSUER = "saml.logout.issuer";
    public static final String SAML_LOGOUT_REQUEST_ID = "SAML_LOGOUT_REQUEST_ID";
    public static final String SAML_LOGOUT_RELAY_STATE = "SAML_LOGOUT_RELAY_STATE";
    public static final String SAML_LOGOUT_BINDING_URI = "SAML_LOGOUT_BINDING_URI";
    public static final String SAML_LOGOUT_SIGNATURE_ALGORITHM = "saml.logout.signature.algorithm";
    public static final String SAML_NAME_ID = "SAML_NAME_ID";
    public static final String SAML_NAME_ID_FORMAT = "SAML_NAME_ID_FORMAT";
    public static final String SAML_DEFAULT_NAMEID_FORMAT = JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get();
    public static final String SAML_PERSISTENT_NAME_ID_FOR = "saml.persistent.name.id.for";

    protected KeycloakSession session;

    protected RealmModel realm;

    protected UriInfo uriInfo;

    protected HttpHeaders headers;

    protected EventBuilder event;


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
    public SamlProtocol setHttpHeaders(HttpHeaders headers){
        this.headers = headers;
        return this;
    }

    @Override
    public SamlProtocol setEventBuilder(EventBuilder event) {
        this.event = event;
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
        SAML2ErrorResponseBuilder builder = new SAML2ErrorResponseBuilder()
                .relayState(clientSession.getNote(GeneralConstants.RELAY_STATE))
                .destination(clientSession.getRedirectUri())
                .issuer(getResponseIssuer(realm))
                .status(status);
      try {
          if (isPostBinding(clientSession)) {
              return builder.postBinding().response();
          } else {
              return builder.redirectBinding().response();
          }
        } catch (Exception e) {
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.FAILED_TO_PROCESS_RESPONSE );
        }
    }

    protected boolean isPostBinding(ClientSessionModel clientSession) {
        ClientModel client = clientSession.getClient();
        return SamlProtocol.SAML_POST_BINDING.equals(clientSession.getNote(SamlProtocol.SAML_BINDING)) || forcePostBinding(client);
    }

    protected boolean isLogoutPostBindingForInitiator(UserSessionModel session) {
        String note = session.getNote(SamlProtocol.SAML_LOGOUT_BINDING);
        return SamlProtocol.SAML_POST_BINDING.equals(note);
    }



    protected boolean isLogoutPostBindingForClient(ClientSessionModel clientSession) {
        ClientModel client = clientSession.getClient();
        String logoutPostUrl = client.getAttribute(SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE);
        String logoutRedirectUrl = client.getAttribute(SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE);

        if (logoutPostUrl == null) {
            // if we don't have a redirect uri either, return true and default to the admin url + POST binding
            if (logoutRedirectUrl == null) return true;
            return false;
        }

        if (forcePostBinding(client)) {
            return true; // configured to force a post binding and post binding logout url is not null
        }

        String bindingType = clientSession.getNote(SAML_BINDING);

        // if the login binding was POST, return true
        if (SAML_POST_BINDING.equals(bindingType)) return true;

        if (logoutRedirectUrl == null) return true; // we don't have a redirect binding url, so use post binding

        return false;  // redirect binding

    }

    public static boolean forcePostBinding(ClientModel client) {
        return "true".equals(client.getAttribute(SamlProtocol.SAML_FORCE_POST_BINDING));
    }

    protected String getNameIdFormat(ClientSessionModel clientSession) {
        String nameIdFormat = clientSession.getNote(GeneralConstants.NAMEID_FORMAT);
        ClientModel client = clientSession.getClient();
        boolean forceFormat = forceNameIdFormat(client);
        String configuredNameIdFormat = client.getAttribute(SAML_NAME_ID_FORMAT_ATTRIBUTE);
        if ((nameIdFormat == null || forceFormat) && configuredNameIdFormat != null) {
            if (configuredNameIdFormat.equals("email")) {
                nameIdFormat = JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get();
            } else if (configuredNameIdFormat.equals("persistent")) {
                nameIdFormat = JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get();
            } else if (configuredNameIdFormat.equals("transient")) {
                nameIdFormat = JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT.get();
            } else if (configuredNameIdFormat.equals("username")) {
                nameIdFormat = JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get();
            } else {
                nameIdFormat = JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get();
            }
        }
        if(nameIdFormat == null) return SAML_DEFAULT_NAMEID_FORMAT;
        return nameIdFormat;
    }

    public static boolean forceNameIdFormat(ClientModel client) {
        return "true".equals(client.getAttribute(SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE));
    }

    protected String getNameId(String nameIdFormat, ClientSessionModel clientSession, UserSessionModel userSession) {
        if (nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get())) {
            return userSession.getUser().getEmail();
        } else if(nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT.get())) {
            // "G-" stands for "generated" Add this for the slight possibility of collisions.
            return "G-" + UUID.randomUUID().toString();
        } else if(nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get())) {
            // generate a persistent user id specifically for each client.
            UserModel user = userSession.getUser();
            String name = SAML_PERSISTENT_NAME_ID_FOR + "." + clientSession.getClient().getClientId();
            String samlPersistentId = user.getAttribute(name);
            if (samlPersistentId != null) return samlPersistentId;
            // "G-" stands for "generated"
            samlPersistentId = "G-" + UUID.randomUUID().toString();
            user.setAttribute(name, samlPersistentId);
            return samlPersistentId;
        } else if(nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get())){
            // TODO: Support for persistent NameID (pseudo-random identifier persisted in user object)
            return userSession.getUser().getUsername();
        } else {
            return userSession.getUser().getUsername();
        }
    }

    @Override
    public Response authenticated(UserSessionModel userSession, ClientSessionCode accessCode) {
        ClientSessionModel clientSession = accessCode.getClientSession();
        ClientModel client = clientSession.getClient();
        String requestID = clientSession.getNote(SAML_REQUEST_ID);
        String relayState = clientSession.getNote(GeneralConstants.RELAY_STATE);
        String redirectUri = clientSession.getRedirectUri();
        String responseIssuer = getResponseIssuer(realm);
        String nameIdFormat = getNameIdFormat(clientSession);
        String nameId = getNameId(nameIdFormat, clientSession, userSession);

        // save NAME_ID and format in clientSession as they may be persistent or transient or email and not username
        // we'll need to send this back on a logout
        clientSession.setNote(SAML_NAME_ID, nameId);
        clientSession.setNote(SAML_NAME_ID_FORMAT, nameIdFormat);

        SALM2LoginResponseBuilder builder = new SALM2LoginResponseBuilder();
        builder.requestID(requestID)
               .destination(redirectUri)
               .issuer(responseIssuer)
               .sessionIndex(clientSession.getId())
               .requestIssuer(clientSession.getClient().getClientId())
               .nameIdentifier(nameIdFormat, nameId)
               .authMethod(JBossSAMLURIConstants.AC_UNSPECIFIED.get());
        if (!includeAuthnStatement(client)) {
            builder.disableAuthnStatement(true);
        }

        List<ProtocolMapperProcessor<SAMLAttributeStatementMapper>> attributeStatementMappers = new LinkedList<>();
        List<ProtocolMapperProcessor<SAMLLoginResponseMapper>> loginResponseMappers = new LinkedList<>();
        ProtocolMapperProcessor<SAMLRoleListMapper> roleListMapper = null;

        Set<ProtocolMapperModel> mappings = client.getProtocolMappers();
        for (ProtocolMapperModel mapping : mappings) {
            if (!mapping.getProtocol().equals(SamlProtocol.LOGIN_PROTOCOL)) continue;

            ProtocolMapper mapper = (ProtocolMapper)session.getKeycloakSessionFactory().getProviderFactory(ProtocolMapper.class, mapping.getProtocolMapper());
            if (mapper == null) continue;
            if (mapper instanceof SAMLAttributeStatementMapper) {
                attributeStatementMappers.add(new ProtocolMapperProcessor<SAMLAttributeStatementMapper>((SAMLAttributeStatementMapper)mapper, mapping));
            }
            if (mapper instanceof SAMLLoginResponseMapper) {
                loginResponseMappers.add(new ProtocolMapperProcessor<SAMLLoginResponseMapper>((SAMLLoginResponseMapper)mapper, mapping));
            }
            if (mapper instanceof SAMLRoleListMapper) {
                roleListMapper = new ProtocolMapperProcessor<SAMLRoleListMapper>((SAMLRoleListMapper)mapper, mapping);
            }
        }


        Document samlDocument = null;
        try {
            ResponseType samlModel = builder.buildModel();
            transformAttributeStatement(attributeStatementMappers, samlModel, session, userSession, clientSession);
            populateRoles(roleListMapper, samlModel, session, userSession, clientSession);
            samlModel = transformLoginResponse(loginResponseMappers, samlModel, session, userSession, clientSession);
            samlDocument = builder.buildDocument(samlModel);
        } catch (Exception e) {
            logger.error("failed", e);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo,headers, Messages.FAILED_TO_PROCESS_RESPONSE);
        }

        SAML2BindingBuilder2 bindingBuilder = new SAML2BindingBuilder2();
        bindingBuilder.relayState(relayState);

        if (requiresRealmSignature(client)) {
            bindingBuilder.signatureAlgorithm(getSignatureAlgorithm(client))
                   .signWith(realm.getPrivateKey(), realm.getPublicKey(), realm.getCertificate())
                   .signDocument();
        }
        if (requiresAssertionSignature(client)) {
            bindingBuilder.signatureAlgorithm(getSignatureAlgorithm(client))
                    .signWith(realm.getPrivateKey(), realm.getPublicKey(), realm.getCertificate())
                    .signAssertions();
        }
        if (requiresEncryption(client)) {
            PublicKey publicKey = null;
            try {
                publicKey = SamlProtocolUtils.getEncryptionValidationKey(client);
            } catch (Exception e) {
                logger.error("failed", e);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.FAILED_TO_PROCESS_RESPONSE);
            }
            bindingBuilder.encrypt(publicKey);
        }
        try {
            if (isPostBinding(clientSession)) {
                return bindingBuilder.postBinding(samlDocument).response(redirectUri);
            } else {
                return bindingBuilder.redirectBinding(samlDocument).response(redirectUri);
            }
        } catch (Exception e) {
            logger.error("failed", e);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.FAILED_TO_PROCESS_RESPONSE );
        }
    }

    public static boolean requiresRealmSignature(ClientModel client) {
        return "true".equals(client.getAttribute(SAML_SERVER_SIGNATURE));
    }

    public static boolean requiresAssertionSignature(ClientModel client) {
        return "true".equals(client.getAttribute(SAML_ASSERTION_SIGNATURE));
    }

    public static boolean includeAuthnStatement(ClientModel client) {
        return "true".equals(client.getAttribute(SAML_AUTHNSTATEMENT));
    }

    public static SignatureAlgorithm getSignatureAlgorithm(ClientModel client) {
        String alg = client.getAttribute(SAML_SIGNATURE_ALGORITHM);
        if (alg != null) {
            SignatureAlgorithm algorithm = SignatureAlgorithm.valueOf(alg);
            if (algorithm != null) return algorithm;
        }
        return SignatureAlgorithm.RSA_SHA256;
    }

    private boolean requiresEncryption(ClientModel client) {
        return "true".equals(client.getAttribute(SAML_ENCRYPT));
    }

    public static class ProtocolMapperProcessor<T> {
        final public T mapper;
        final public ProtocolMapperModel model;

        public ProtocolMapperProcessor(T mapper, ProtocolMapperModel model) {
            this.mapper = mapper;
            this.model = model;
        }
    }

    public void transformAttributeStatement(List<ProtocolMapperProcessor<SAMLAttributeStatementMapper>> attributeStatementMappers,
                                            ResponseType response,
                                            KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionModel clientSession) {
        AssertionType assertion = response.getAssertions().get(0).getAssertion();
        AttributeStatementType attributeStatement = new AttributeStatementType();
        assertion.addStatement(attributeStatement);
        for (ProtocolMapperProcessor<SAMLAttributeStatementMapper> processor : attributeStatementMappers) {
            processor.mapper.transformAttributeStatement(attributeStatement, processor.model, session, userSession, clientSession);
        }
    }

    public ResponseType transformLoginResponse(List<ProtocolMapperProcessor<SAMLLoginResponseMapper>> mappers,
                                            ResponseType response,
                                            KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionModel clientSession) {
        for (ProtocolMapperProcessor<SAMLLoginResponseMapper> processor : mappers) {
            response = processor.mapper.transformLoginResponse(response, processor.model, session, userSession, clientSession);
        }
        return response;
    }

    public void populateRoles(ProtocolMapperProcessor<SAMLRoleListMapper> roleListMapper,
                              ResponseType response,
                              KeycloakSession session,
                              UserSessionModel userSession, ClientSessionModel clientSession) {
        if (roleListMapper == null) return;
        AssertionType assertion = response.getAssertions().get(0).getAssertion();
        AttributeStatementType attributeStatement = new AttributeStatementType();
        assertion.addStatement(attributeStatement);
        roleListMapper.mapper.mapRoles(attributeStatement, roleListMapper.model, session, userSession, clientSession);
    }


    @Override
    public Response consentDenied(ClientSessionModel clientSession) {
        return getErrorResponse(clientSession, JBossSAMLURIConstants.STATUS_REQUEST_DENIED.get());
    }

    public static String getLogoutServiceUrl(UriInfo uriInfo, ClientModel client, String bindingType) {
        String logoutServiceUrl = null;
        if (SAML_POST_BINDING.equals(bindingType)) {
            logoutServiceUrl = client.getAttribute(SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE);
        } else {
            logoutServiceUrl = client.getAttribute(SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE);
        }
        if (logoutServiceUrl == null && client instanceof ApplicationModel) logoutServiceUrl = ((ApplicationModel)client).getManagementUrl();
        return ResourceAdminManager.resolveUri(uriInfo.getRequestUri(), logoutServiceUrl);

    }

    @Override
    public Response frontchannelLogout(UserSessionModel userSession, ClientSessionModel clientSession) {
        ClientModel client = clientSession.getClient();
        if (!(client instanceof ApplicationModel)) return null;
        SAML2LogoutRequestBuilder logoutBuilder = createLogoutRequest(clientSession, client);
        try {
            if (isLogoutPostBindingForClient(clientSession)) {
                String bindingUri = getLogoutServiceUrl(uriInfo, client, SAML_POST_BINDING);
                return logoutBuilder.postBinding().request(bindingUri);
            } else {
                logger.debug("frontchannel redirect binding");
                String bindingUri = getLogoutServiceUrl(uriInfo, client, SAML_REDIRECT_BINDING);
                return logoutBuilder.redirectBinding().request(bindingUri);
            }
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        } catch (ProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParsingException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Response finishLogout(UserSessionModel userSession) {
        logger.debug("finishLogout");
        String logoutBindingUri = userSession.getNote(SAML_LOGOUT_BINDING_URI);
        String logoutRelayState = userSession.getNote(SAML_LOGOUT_RELAY_STATE);
        SAML2LogoutResponseBuilder builder = new SAML2LogoutResponseBuilder();
        builder.logoutRequestID(userSession.getNote(SAML_LOGOUT_REQUEST_ID));
        builder.destination(logoutBindingUri);
        builder.issuer(getResponseIssuer(realm));
        builder.relayState(logoutRelayState);
        String signingAlgorithm = userSession.getNote(SAML_LOGOUT_SIGNATURE_ALGORITHM);
        if (signingAlgorithm != null) {
            SignatureAlgorithm algorithm = SignatureAlgorithm.valueOf(signingAlgorithm);
            builder.signatureAlgorithm(algorithm)
                    .signWith(realm.getPrivateKey(), realm.getPublicKey(), realm.getCertificate())
                    .signDocument();
        }

        try {
            if (isLogoutPostBindingForInitiator(userSession)) {
                return builder.postBinding().response(logoutBindingUri);
            } else {
                return builder.redirectBinding().response(logoutBindingUri);
            }
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        } catch (ProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    @Override
    public void backchannelLogout(UserSessionModel userSession, ClientSessionModel clientSession) {
        ClientModel client = clientSession.getClient();
        if (!(client instanceof ApplicationModel)) return;
        ApplicationModel app = (ApplicationModel)client;
        String logoutUrl = getLogoutServiceUrl(uriInfo, client, SAML_POST_BINDING);
        if (logoutUrl == null) {
            logger.warnv("Can't do backchannel logout. No SingleLogoutService POST Binding registered for client: {1}", client.getClientId());
            return;
        }
        SAML2LogoutRequestBuilder logoutBuilder = createLogoutRequest(clientSession, client);


        String logoutRequestString = null;
        try {
            logoutRequestString = logoutBuilder.postBinding().encoded();
        } catch (Exception e) {
            logger.warn("failed to send saml logout", e);
            return;
        }


        ApacheHttpClient4Executor executor = ResourceAdminManager.createExecutor();


        try {
            ClientRequest request = executor.createRequest(logoutUrl);
            request.formParameter(GeneralConstants.SAML_REQUEST_KEY, logoutRequestString);
            request.formParameter(SAML2LogOutHandler.BACK_CHANNEL_LOGOUT, SAML2LogOutHandler.BACK_CHANNEL_LOGOUT);
            ClientResponse response = null;
            try {
                response = request.post();
                response.releaseConnection();
                // Undertow will redirect root urls not ending in "/" to root url + "/".  Test for this weird behavior
                if (response.getStatus() == 302  && !logoutUrl.endsWith("/")) {
                    String redirect = (String)response.getHeaders().getFirst(HttpHeaders.LOCATION);
                    String withSlash = logoutUrl + "/";
                    if (withSlash.equals(redirect)) {
                        request = executor.createRequest(withSlash);
                        request.formParameter(GeneralConstants.SAML_REQUEST_KEY, logoutRequestString);
                        request.formParameter(SAML2LogOutHandler.BACK_CHANNEL_LOGOUT, SAML2LogOutHandler.BACK_CHANNEL_LOGOUT);
                        response = request.post();
                        response.releaseConnection();
                    }
                }
            } catch (Exception e) {
                logger.warn("failed to send saml logout", e);
            }

        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }

    }

    protected SAML2LogoutRequestBuilder createLogoutRequest(ClientSessionModel clientSession, ClientModel client) {
        // build userPrincipal with subject used at login
        SAML2LogoutRequestBuilder logoutBuilder = new SAML2LogoutRequestBuilder()
                                         .issuer(getResponseIssuer(realm))
                                         .userPrincipal(clientSession.getNote(SAML_NAME_ID), clientSession.getNote(SAML_NAME_ID_FORMAT))
                                         .destination(client.getClientId());
        if (requiresRealmSignature(client)) {
            logoutBuilder.signatureAlgorithm(getSignatureAlgorithm(client))
                         .signWith(realm.getPrivateKey(), realm.getPublicKey(), realm.getCertificate())
                         .signDocument();
        }
        /*
        if (requiresEncryption(client)) {
            PublicKey publicKey = null;
            try {
                publicKey = PemUtils.decodePublicKey(client.getAttribute(ClientModel.PUBLIC_KEY));
            } catch (Exception e) {
                logger.error("failed", e);
                return;
            }
            logoutBuilder.encrypt(publicKey);
        }
        */
        return logoutBuilder;
    }

    @Override
    public void close() {

    }
}
