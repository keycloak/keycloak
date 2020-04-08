package org.keycloak.protocol.ciba.decoupledauthn;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.CodeToTokenStoreProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.ciba.CIBAConstants;
import org.keycloak.protocol.ciba.endpoints.request.BackchannelAuthenticationRequest;
import org.keycloak.protocol.ciba.resolvers.CIBALoginUserResolver;
import org.keycloak.protocol.ciba.utils.DecoupledAuthnResult;
import org.keycloak.protocol.ciba.utils.DecoupledAuthnResultParser;
import org.keycloak.protocol.ciba.utils.DecoupledAuthStatus;
import org.keycloak.services.CorsErrorResponseException;

public class DelegateDecoupledAuthenticationProvider extends DecoupledAuthenticationProviderBase {

    private static final Logger logger = Logger.getLogger(DelegateDecoupledAuthenticationProvider.class);

    private final String decoupledAuthenticationRequestUri;

    public DelegateDecoupledAuthenticationProvider(KeycloakSession session, String decoupledAuthenticationRequestUri) {
        super(session);
        this.decoupledAuthenticationRequestUri = decoupledAuthenticationRequestUri;
    }

    private String scope;
    private String userSessionIdWillBeCreated;
    private String userIdToBeAuthenticated;
    private String authResultId;
    private int expiration;

    @Override
    protected String getScope() {
        return scope;
    }

    @Override
    protected String getUserSessionIdWillBeCreated() {
        return userSessionIdWillBeCreated;
    }

    @Override
    protected String getUserIdToBeAuthenticated() {
        return userIdToBeAuthenticated;
    }

    @Override
    protected String getAuthResultId() {
        return authResultId;
    }

    @Override
    protected int getExpiration() {
        return expiration;
    }

    @Override
    protected Response verifyDecoupledAuthnResult() {
        String decoupledAuthId = formParams.getFirst(DelegateDecoupledAuthenticationProvider.DECOUPLED_AUTHN_ID);
        ParseResult parseResult = parseDecoupledAuthId(session, decoupledAuthId, event);

        if (parseResult.isIllegalDecoupledAuthId()) {
            event.error(Errors.INVALID_INPUT);
            persistDecoupledAuthenticationResult(DecoupledAuthStatus.UNKNOWN);
            // decoupled auth id format is invalid or it has already been used
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "invalid decoupled authn id", Response.Status.BAD_REQUEST);
        } else if (parseResult.isExpiredDecoupledAuthId()) {
            event.error(Errors.SESSION_EXPIRED);
            persistDecoupledAuthenticationResult(DecoupledAuthStatus.EXPIRED);
            return cors.builder(Response.ok("", MediaType.APPLICATION_JSON_TYPE)).build();
        }

        DecoupledAuthId decoupledAuthIdData = parseResult.decoupledAuthIdData();
        authResultId = decoupledAuthIdData.getAuthResultId().toString();
        scope = decoupledAuthIdData.getScope();
        expiration = decoupledAuthIdData.getExpiration();
        userSessionIdWillBeCreated = decoupledAuthIdData.getUserSessionIdWillBeCreated();
        userIdToBeAuthenticated = decoupledAuthIdData.getUserIdToBeAuthenticated();
        // to bind Client Session of CD(Consumer Device) with User Session, set CD's Client Model to this class member "client".
        client = realm.getClientByClientId(decoupledAuthIdData.getClientId());

        CIBALoginUserResolver resolver = session.getProvider(CIBALoginUserResolver.class);
        if (resolver == null) {
            throw new RuntimeException("CIBA Login User Resolver not setup properly.");
        }
        String userIdAuthenticated = resolver.getUserFromInfoUsedByAuthentication(formParams.getFirst(DelegateDecoupledAuthenticationProvider.DECOUPLED_AUTHN_USER_INFO)).getId();
        if (!userIdToBeAuthenticated.equals(userIdAuthenticated)) {
            event.error(Errors.DIFFERENT_USER_AUTHENTICATED);
            persistDecoupledAuthenticationResult(DecoupledAuthStatus.DIFFERENT);
            return cors.builder(Response.ok("", MediaType.APPLICATION_JSON_TYPE)).build();
        }

        String authResult = formParams.getFirst(DelegateDecoupledAuthenticationProvider.DECOUPLED_AUTHN_RESULT);
        if (authResult == null) {
            event.error(Errors.INVALID_INPUT);
            persistDecoupledAuthenticationResult(DecoupledAuthStatus.UNKNOWN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "authentication result not specified", Response.Status.BAD_REQUEST);
        } else if (authResult.equals(DecoupledAuthStatus.FAILED)) {
            event.error(Errors.NOT_LOGGED_IN);
            persistDecoupledAuthenticationResult(DecoupledAuthStatus.FAILED);
        } else if (authResult.equals(DecoupledAuthStatus.CANCELLED)) {
            event.error(Errors.NOT_ALLOWED);
            persistDecoupledAuthenticationResult(DecoupledAuthStatus.CANCELLED);
        } else if (authResult.equals(DecoupledAuthStatus.UNAUTHORIZED)) {
            event.error(Errors.CONSENT_DENIED);
            persistDecoupledAuthenticationResult(DecoupledAuthStatus.UNAUTHORIZED);
        } else if (authResult.equals(DecoupledAuthStatus.SUCCEEDED)) {
            return null;
        } else {
            event.error(Errors.INVALID_INPUT);
            persistDecoupledAuthenticationResult(DecoupledAuthStatus.UNKNOWN);
        }
        return cors.builder(Response.ok("", MediaType.APPLICATION_JSON_TYPE)).build();
    }

    @Override
    public void doBackchannelAuthentication(ClientModel client, BackchannelAuthenticationRequest request, int expiresIn, String authResultId, String userSessionIdWillBeCreated) {
        // create Decoupled Auth ID
        // Decoupled Auth ID binds Backchannel Authentication Request with Authentication by AD(Authentication Device).
        // By including userSessionIdWillBeCreated. keycloak can create UserSession whose ID is userSessionIdWillBeCreated on Decoupled Authentication Callback Endpoint,
        // which can bind userSessionIdWillBeCreated (namely, Backchannel Authentication Request) with authenticated UserSession.
        // By including authResultId, keycloak can create Decoupled Authentication Result of Authentication by AD on Decoupled Authentication Callback Endpoint,
        // which can bind authResultId with Decoupled Authentication Result of Authentication by AD.
        // By including client_id, Decoupled Authentication Callback Endpoint can recognize the CD(Consumer Device) who sent Backchannel Authentication Request.

        // The following scopes should be displayed on AD(Authentication Device):
        // 1. scopes specified explicitly as query parameter in the authorization request
        // 2. scopes specified implicitly as default client scope in keycloak
        CIBALoginUserResolver resolver = session.getProvider(CIBALoginUserResolver.class);
        if (resolver == null) {
            throw new RuntimeException("CIBA Login User Resolver not setup properly.");
        }
        String authRequestedUserHint = realm.getCIBAPolicy().getAuthRequestedUserHint();
        UserModel user = null;
        if (authRequestedUserHint.equals(CIBAConstants.LOGIN_HINT)) {
            user = resolver.getUserFromLoginHint(request.getLoginHint());
        } else if (authRequestedUserHint.equals(CIBAConstants.ID_TOKEN_HINT)) {
            user = resolver.getUserFromLoginHint(request.getIdTokenHint());
        } else if (authRequestedUserHint.equals(CIBAConstants.LOGIN_HINT_TOKEN)) {
            user = resolver.getUserFromLoginHint(request.getLoginHintToken());
        } else {
            throw new RuntimeException("CIBA invalid Authentication Requested User Hint.");
        }
        String infoUsedByAuthentication = resolver.getInfoUsedByAuthentication(user);

        StringBuilder scopeBuilder = new StringBuilder();
        Map<String, ClientScopeModel> defaultScopesMap = client.getClientScopes(true, true);
        defaultScopesMap.forEach((key, value)->{if (value.isDisplayOnConsentScreen()) scopeBuilder.append(value.getName()).append(" ");});
        String defaultClientScope = scopeBuilder.toString();

        String userIdToBeAuthenticated = session.users().getUserByUsername(request.getLoginHint(), realm).getId();

        DecoupledAuthId decoupledAuthIdData = new DecoupledAuthId(Time.currentTime() + expiresIn, request.getScope(),
                userSessionIdWillBeCreated, userIdToBeAuthenticated, client.getClientId(), authResultId);
        String decoupledAuthId = persistDecoupledAuthId(session, decoupledAuthIdData, expiresIn);

        logger.info("  decoupledAuthnRequestUri = " + decoupledAuthenticationRequestUri);
        try {
            int status = SimpleHttp.doPost(decoupledAuthenticationRequestUri, session)
                .param(DECOUPLED_AUTHN_ID, decoupledAuthId)
                .param(DECOUPLED_AUTHN_USER_INFO, infoUsedByAuthentication)
                .param(DECOUPLED_AUTHN_IS_CONSENT_REQUIRED, Boolean.toString(client.isConsentRequired()))
                .param(CIBAConstants.SCOPE, request.getScope())
                .param(DECOUPLED_DEFAULT_CLIENT_SCOPE, defaultClientScope)
                .param(CIBAConstants.BINDING_MESSAGE, request.getBindingMessage())
                .asStatus();
            logger.info("  Decoupled Authn Request URI Access = " + status);
            if (status != 200) {
                // To terminate CIBA flow, set Auth Result as unknown
                DecoupledAuthnResult decoupledAuthnResult = new DecoupledAuthnResult(Time.currentTime() + expiresIn, DecoupledAuthStatus.UNKNOWN);
                DecoupledAuthnResultParser.persistDecoupledAuthnResult(session, authResultId.toString(), decoupledAuthnResult, Time.currentTime() + expiresIn);
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Decoupled Authn Request URI Access failed.", ioe);
        }
    }

    public static final String DECOUPLED_AUTHN_ID = "decoupled_auth_id";
    public static final String DECOUPLED_AUTHN_USER_INFO = "user_info";
    public static final String DECOUPLED_AUTHN_RESULT = "auth_result";
    public static final String DECOUPLED_AUTHN_IS_CONSENT_REQUIRED = "is_consent_required";
    public static final String DECOUPLED_DEFAULT_CLIENT_SCOPE = "default_client_scope";

    public DecoupledAuthId deserializeCode(Map<String, String> data) {
        return new DecoupledAuthId(data);
    }

    public class DecoupledAuthId {
        private static final String EXPIRATION_NOTE = "exp";
        private static final String SCOPE_NOTE = "scope";
        private static final String USER_SESSION_ID_NOTE = "user_session_id";
        private static final String USER_ID_NOTE = "user_id";
        private static final String CLIENT_ID_NOTE = "client_id";
        private static final String AUTH_RESULT_ID_NOTE = "auth_result_id";

        private final int expiration;
        private final String scope;
        private final String userSessionIdWillBeCreated;
        private final String userIdToBeAuthenticated;
        private final String clientId;
        private final String authResultId;

        public DecoupledAuthId(int expiration, String scope, String userSessionIdWillBeCreated, String userIdToBeAuthenticated, String clientId, String authResultId) {
            this.expiration = expiration;
            this.scope = scope;
            this.userSessionIdWillBeCreated = userSessionIdWillBeCreated;
            this.userIdToBeAuthenticated = userIdToBeAuthenticated;
            this.clientId = clientId;
            this.authResultId = authResultId;
        }
     
        private DecoupledAuthId(Map<String, String> data) {
            expiration = Integer.parseInt(data.get(EXPIRATION_NOTE));
            scope = data.get(SCOPE_NOTE);
            userSessionIdWillBeCreated = data.get(USER_SESSION_ID_NOTE);
            userIdToBeAuthenticated = data.get(USER_ID_NOTE);
            clientId = data.get(CLIENT_ID_NOTE);
            authResultId = data.get(AUTH_RESULT_ID_NOTE);
        }

        public Map<String, String> serializeCode() {
            Map<String, String> result = new HashMap<>();
            result.put(EXPIRATION_NOTE, String.valueOf(expiration));
            result.put(SCOPE_NOTE, scope);
            result.put(USER_SESSION_ID_NOTE, userSessionIdWillBeCreated);
            result.put(USER_ID_NOTE, userIdToBeAuthenticated);
            result.put(CLIENT_ID_NOTE, clientId);
            result.put(AUTH_RESULT_ID_NOTE, authResultId);
            return result;
        }

        public int getExpiration() {
            return expiration;
        }

        public String getScope() {
            return scope;
        }

        public String getUserSessionIdWillBeCreated() {
            return userSessionIdWillBeCreated;
        }

        public String getUserIdToBeAuthenticated() {
            return userIdToBeAuthenticated;
        }

        public String getClientId() {
            return clientId;
        }

        public String getAuthResultId() {
            return authResultId;
        }
    }

    public String persistDecoupledAuthId(KeycloakSession session, DecoupledAuthId decoupledAuthIdData, int expires_in) {
        CodeToTokenStoreProvider codeStore = session.getProvider(CodeToTokenStoreProvider.class);
        UUID key = UUID.randomUUID();
        Map<String, String> serialized = decoupledAuthIdData.serializeCode();
        codeStore.put(key, expires_in, serialized);
        return key.toString();
    }

    public ParseResult parseDecoupledAuthId(KeycloakSession session, String decoupledAuthId, EventBuilder event) {
        ParseResult result = new ParseResult(decoupledAuthId);

        // Parse UUID
        UUID codeUUID;
        try {
            codeUUID = UUID.fromString(decoupledAuthId);
        } catch (IllegalArgumentException re) {
            logger.warn("Invalid format of the UUID in Decoupled Auth Id");
            return result.illegalDecoupledAuthId();
        }

        CodeToTokenStoreProvider decoupledAuthIdStore = session.getProvider(CodeToTokenStoreProvider.class);
        Map<String, String> decoupledAuthIdData = decoupledAuthIdStore.remove(codeUUID);

        // Either code not available or was already used
        if (decoupledAuthIdData == null) {
            logger.warnf("Decoupled Auth Id '%s' has already been used.", decoupledAuthId);
            return result.illegalDecoupledAuthId();
        }


        logger.tracef("Successfully verified Decoupled Auth Id '%s'", decoupledAuthId);

        result.decoupledAuthIdData = deserializeCode(decoupledAuthIdData);

        event.detail(Details.CODE_ID, result.decoupledAuthIdData.getUserSessionIdWillBeCreated());
        event.session(result.decoupledAuthIdData.getUserSessionIdWillBeCreated());

        // Finally doublecheck if code is not expired
        int currentTime = Time.currentTime();
        if (currentTime > result.decoupledAuthIdData.getExpiration()) {
            return result.expiredDecoupledAuthId();
        }

        return result;
    }

    public class ParseResult {

        private final String decoupledAuthId;
        private DecoupledAuthId decoupledAuthIdData;

        private boolean isIllegalDecoupledAuthId= false;
        private boolean isExpiredDecoupledAuthId = false;

        private ParseResult(String decoupledAuthId) {
            this.decoupledAuthId = decoupledAuthId;
        }

        public String getDecoupledAuthId() {
            return decoupledAuthId;
        }

        public DecoupledAuthId decoupledAuthIdData() {
            return decoupledAuthIdData;
        }

        public boolean isIllegalDecoupledAuthId() {
            return isIllegalDecoupledAuthId;
        }

        public boolean isExpiredDecoupledAuthId() {
            return isExpiredDecoupledAuthId;
        }

        private ParseResult illegalDecoupledAuthId() {
            this.isIllegalDecoupledAuthId = true;
            return this;
        }

        private ParseResult expiredDecoupledAuthId() {
            this.isExpiredDecoupledAuthId = true;
            return this;
        }
    }
}
