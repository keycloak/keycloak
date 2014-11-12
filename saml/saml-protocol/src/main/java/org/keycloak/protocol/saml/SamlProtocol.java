package org.keycloak.protocol.saml;

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
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.admin.ClientAttributeCertificateResource;
import org.keycloak.services.resources.flows.Flows;
import org.picketlink.common.constants.GeneralConstants;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.identity.federation.core.saml.v2.constants.X500SAMLProfileConstants;
import org.picketlink.identity.federation.web.handlers.saml2.SAML2LogOutHandler;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.security.PublicKey;

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
    public static final String SAML_DEFAULT_NAMEID_FORMAT = JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get();

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
                .responseIssuer(getResponseIssuer(realm))
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

    protected String getNameIdFormat(ClientSessionModel clientSession) {
        String nameIdFormat = clientSession.getNote(GeneralConstants.NAMEID_FORMAT);
        if(nameIdFormat == null) return SAML_DEFAULT_NAMEID_FORMAT;
        return nameIdFormat;
    }

    protected String getNameId(String nameIdFormat, ClientSessionModel clientSession, UserSessionModel userSession) {
        if(nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get())) {
            return userSession.getUser().getEmail();
        } else if(nameIdFormat.equals(JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT.get())) {
            return userSession.getUser().getUsername();
        } else {
            // TODO: Support for persistent NameID (pseudo-random identifier persisted in user object)
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

        SALM2LoginResponseBuilder builder = new SALM2LoginResponseBuilder();
        builder.requestID(requestID)
               .relayState(relayState)
               .destination(redirectUri)
               .responseIssuer(responseIssuer)
               .requestIssuer(clientSession.getClient().getClientId())
               .nameIdentifier(nameIdFormat, nameId)
               .authMethod(JBossSAMLURIConstants.AC_UNSPECIFIED.get());
        initClaims(builder, clientSession.getClient(), userSession.getUser());
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

    private boolean requiresRealmSignature(ClientModel client) {
        return "true".equals(client.getAttribute(SAML_SERVER_SIGNATURE));
    }

    private boolean requiresAssertionSignature(ClientModel client) {
        return "true".equals(client.getAttribute(SAML_ASSERTION_SIGNATURE));
    }

    private boolean includeAuthnStatement(ClientModel client) {
        return "true".equals(client.getAttribute(SAML_AUTHNSTATEMENT));
    }

    private boolean multivaluedRoles(ClientModel client) {
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

    public void initClaims(SALM2LoginResponseBuilder builder, ClientModel model, UserModel user) {
        if (ClaimMask.hasEmail(model.getAllowedClaimsMask())) {
            builder.attribute(X500SAMLProfileConstants.EMAIL_ADDRESS.getFriendlyName(), user.getEmail());
        }
        if (ClaimMask.hasName(model.getAllowedClaimsMask())) {
            builder.attribute(X500SAMLProfileConstants.GIVEN_NAME.getFriendlyName(), user.getFirstName());
            builder.attribute(X500SAMLProfileConstants.SURNAME.getFriendlyName(), user.getLastName());
        }
        if (ClaimMask.hasUsername(model.getAllowedClaimsMask())) {
            builder.attribute(X500SAMLProfileConstants.USERID.getFriendlyName(), user.getUsername());
        }
    }



    @Override
    public Response consentDenied(ClientSessionModel clientSession) {
        return getErrorResponse(clientSession, JBossSAMLURIConstants.STATUS_REQUEST_DENIED.get());
    }

    @Override
    public void backchannelLogout(UserSessionModel userSession, ClientSessionModel clientSession) {
        ClientModel client = clientSession.getClient();
        if (!(client instanceof ApplicationModel)) return;
        ApplicationModel app = (ApplicationModel)client;
        if (app.getManagementUrl() == null) return;

        SAML2LogoutRequestBuilder logoutBuilder = new SAML2LogoutRequestBuilder()
                                         .userPrincipal(userSession.getUser().getUsername())
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

    @Override
    public void close() {

    }
}
