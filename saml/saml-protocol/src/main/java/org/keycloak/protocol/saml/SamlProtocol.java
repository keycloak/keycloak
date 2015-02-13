package org.keycloak.protocol.saml;

import com.dell.software.ce.dib.claims.ClaimsManipulation;
import com.dell.software.ce.dib.claims.ClaimsManipulationFactory;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.admin.ClientAttributeCertificateResource;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.util.MultivaluedHashMap;
import org.picketlink.common.constants.GeneralConstants;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ParsingException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.identity.federation.core.saml.v2.constants.X500SAMLProfileConstants;
import org.picketlink.identity.federation.web.handlers.saml2.SAML2LogOutHandler;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
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
    public static final String LOGIN_PROTOCOL = "saml";
    public static final String SAML_BINDING = "saml_binding";
    public static final String SAML_POST_BINDING = "post";
    public static final String SAML_GET_BINDING = "get";
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
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Failed to process response");
        }
    }

    protected boolean isPostBinding(ClientSessionModel clientSession) {
        ClientModel client = clientSession.getClient();
        return SamlProtocol.SAML_POST_BINDING.equals(clientSession.getNote(SamlProtocol.SAML_BINDING)) || "true".equals(client.getAttribute(SAML_FORCE_POST_BINDING));
    }

    protected boolean isLogoutPostBindingForInitiator(UserSessionModel session) {
        String note = session.getNote(SamlProtocol.SAML_LOGOUT_BINDING);
        return SamlProtocol.SAML_POST_BINDING.equals(note);
    }

    protected boolean isLogoutPostBindingForClient(ClientModel client) {
        return SamlProtocol.SAML_POST_BINDING.equals(client.getAttribute(SamlProtocol.SAML_LOGOUT_BINDING));
    }

    protected String getNameIdFormat(ClientSessionModel clientSession) {
        String nameIdFormat = clientSession.getNote(GeneralConstants.NAMEID_FORMAT);
        if(nameIdFormat == null) return SAML_DEFAULT_NAMEID_FORMAT;
        return nameIdFormat;
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
               .relayState(relayState)
               .destination(redirectUri)
               .issuer(responseIssuer)
               .requestIssuer(clientSession.getClient().getClientId())
               .nameIdentifier(nameIdFormat, nameId)
               .authMethod(JBossSAMLURIConstants.AC_UNSPECIFIED.get());
        initClaims(builder, userSession, clientSession.getClient(), userSession.getUser());
        if (clientSession.getRoles() != null) {
            if (multivaluedRoles(client)) {
                builder.multiValuedRoles(true);
            }
            for (String roleId : clientSession.getRoles()) {
                // todo need a role mapping
                RoleModel roleModel = clientSession.getRealm().getRoleById(roleId);
                builder.roles(roleModel.getName());
            }
        }
        if (requiresRealmSignature(client)) {
            builder.signatureAlgorithm(getSignatureAlgorithm(client))
                   .signWith(realm.getPrivateKey(), realm.getPublicKey(), realm.getCertificate())
                   .signDocument();
        }
        if (requiresAssertionSignature(client)) {
            builder.signatureAlgorithm(getSignatureAlgorithm(client))
                    .signWith(realm.getPrivateKey(), realm.getPublicKey(), realm.getCertificate())
                    .signAssertions();
        }
        if (!includeAuthnStatement(client)) {
            builder.disableAuthnStatement(true);
        }
        if (requiresEncryption(client)) {
            PublicKey publicKey = null;
            try {
                publicKey = SamlProtocolUtils.getEncryptionValidationKey(client);
            } catch (Exception e) {
                logger.error("failed", e);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Failed to process response");
            }
            builder.encrypt(publicKey);
        }
        try {
            if (isPostBinding(clientSession)) {
                return builder.postBinding().response();
            } else {
                return builder.redirectBinding().response();
            }
        } catch (Exception e) {
            logger.error("failed", e);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Failed to process response");
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

    public static boolean multivaluedRoles(ClientModel client) {
        return "true".equals(client.getAttribute(SAML_MULTIVALUED_ROLES));
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

    public void initClaims(SALM2LoginResponseBuilder builder, UserSessionModel userSession, ClientModel model, UserModel user) {
        MultivaluedHashMap<String, String> claims = (MultivaluedHashMap<String, String>)userSession.getClaims().clone();

        if (ClaimMask.hasEmail(model.getAllowedClaimsMask())) {
            claims.add(X500SAMLProfileConstants.EMAIL_ADDRESS.getFriendlyName(), user.getEmail());
            //builder.attribute(X500SAMLProfileConstants.EMAIL_ADDRESS.getFriendlyName(), user.getEmail());
        }
        if (ClaimMask.hasName(model.getAllowedClaimsMask())) {
            claims.add(X500SAMLProfileConstants.GIVEN_NAME.getFriendlyName(), user.getFirstName());
            claims.add(X500SAMLProfileConstants.SURNAME.getFriendlyName(), user.getLastName());
            //builder.attribute(X500SAMLProfileConstants.GIVEN_NAME.getFriendlyName(), user.getFirstName());
            //builder.attribute(X500SAMLProfileConstants.SURNAME.getFriendlyName(), user.getLastName());
        }
        if (ClaimMask.hasUsername(model.getAllowedClaimsMask())) {
            claims.add(X500SAMLProfileConstants.USERID.getFriendlyName(), user.getUsername());
            //builder.attribute(X500SAMLProfileConstants.USERID.getFriendlyName(), user.getUsername());
        }

        initClaimsSpi(builder, claims, userSession, model, user);
    }

    protected void initClaimsSpi(SALM2LoginResponseBuilder builder, MultivaluedHashMap<String, String> claims, UserSessionModel userSession, ClientModel model, UserModel user) {
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(ClaimsManipulation.class);

        if(factories == null) {
            return;
        }

        if(claims == null /* || !should pass through claims model.getAllowedClaimsMask() */) {
            claims = new MultivaluedHashMap<String, String>();
        }

        for(ProviderFactory factory : factories) {
            ClaimsManipulation provider = (ClaimsManipulation) factory.create(session);

            try {
                provider.initClaims(claims, userSession, model, user);
            }
            finally {
                provider.close();
            }
        }

        addClaims(builder, claims);
    }

    private void addClaims(SALM2LoginResponseBuilder builder, MultivaluedHashMap<String, String> claims) {
        if(claims == null) {
            return;
        }

        for(Map.Entry<String, List<String>> claim : claims.entrySet()) {
            builder.attribute(claim.getKey(), claim.getValue());
        }
    }

    @Override
    public Response consentDenied(ClientSessionModel clientSession) {
        return getErrorResponse(clientSession, JBossSAMLURIConstants.STATUS_REQUEST_DENIED.get());
    }

    protected String getBindingUri(ClientModel client) {
        String bindingUri = client.getAttribute(SamlProtocol.SAML_LOGOUT_BINDING_URI);
        if (bindingUri == null ) bindingUri = ((ApplicationModel)client).getManagementUrl();
        return ResourceAdminManager.resolveUri(uriInfo.getRequestUri(), bindingUri);

    }

    @Override
    public Response frontchannelLogout(UserSessionModel userSession, ClientSessionModel clientSession) {
        ClientModel client = clientSession.getClient();
        if (!(client instanceof ApplicationModel)) return null;
        ApplicationModel app = (ApplicationModel)client;
        String bindingUri = getBindingUri(client);
        if (bindingUri == null) return null;
        SAML2LogoutRequestBuilder logoutBuilder = createLogoutRequest(clientSession, client);
        try {
            if (isLogoutPostBindingForClient(app)) {
                return logoutBuilder.postBinding().request(bindingUri);
            } else {
                logger.debug("frontchannel redirect binding");
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
        SAML2LogoutResponseBuilder builder = new SAML2LogoutResponseBuilder();
        builder.logoutRequestID(userSession.getNote(SAML_LOGOUT_REQUEST_ID));
        builder.destination(userSession.getNote(SAML_LOGOUT_ISSUER));
        String signingAlgorithm = userSession.getNote(SAML_LOGOUT_SIGNATURE_ALGORITHM);
        if (signingAlgorithm != null) {
            SignatureAlgorithm algorithm = SignatureAlgorithm.valueOf(signingAlgorithm);
            builder.signatureAlgorithm(algorithm)
                    .signWith(realm.getPrivateKey(), realm.getPublicKey(), realm.getCertificate())
                    .signDocument();
        }

        try {
            if (isLogoutPostBindingForInitiator(userSession)) {
                return builder.postBinding().response(userSession.getNote(SAML_LOGOUT_BINDING_URI));
            } else {
                return builder.redirectBinding().response(userSession.getNote(SAML_LOGOUT_BINDING_URI));
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
        if (app.getManagementUrl() == null) return;
        SAML2LogoutRequestBuilder logoutBuilder = createLogoutRequest(clientSession, client);


        String logoutRequestString = null;
        try {
            logoutRequestString = logoutBuilder.postBinding().encoded();
        } catch (Exception e) {
            logger.warn("failed to send saml logout", e);
            return;
        }


        String adminUrl = ResourceAdminManager.getManagementUrl(uriInfo.getRequestUri(), app);

        ApacheHttpClient4Executor executor = ResourceAdminManager.createExecutor();


        try {
            ClientRequest request = executor.createRequest(adminUrl);
            request.formParameter(GeneralConstants.SAML_REQUEST_KEY, logoutRequestString);
            request.formParameter(SAML2LogOutHandler.BACK_CHANNEL_LOGOUT, SAML2LogOutHandler.BACK_CHANNEL_LOGOUT);
            ClientResponse response = null;
            try {
                response = request.post();
                response.releaseConnection();
                // Undertow will redirect root urls not ending in "/" to root url + "/".  Test for this weird behavior
                if (response.getStatus() == 302  && !adminUrl.endsWith("/")) {
                    String redirect = (String)response.getHeaders().getFirst(HttpHeaders.LOCATION);
                    String withSlash = adminUrl + "/";
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
