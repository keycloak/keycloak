package org.keycloak.services.clientpolicy.executor;

import java.util.Map;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AbstractTokenResponseContext;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

/**
 *
 * @author rmartinc
 */
public class RejectMayActClaimExecutor implements ClientPolicyExecutorProvider<RejectMayActClaimExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(RejectMayActClaimExecutor.class);
    private final KeycloakSession session;
    private Configuration configuration;

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

        @JsonProperty(RejectMayActClaimExecutorFactory.REJECT_ANY_MAY_ACT)
        protected Boolean rejectAny;
        @JsonProperty(RejectMayActClaimExecutorFactory.AVOID_PERMISSION_CHECK)
        protected Boolean avoidPermissionCheck;
        @JsonProperty(RejectMayActClaimExecutorFactory.AVOID_CONSENT_CHECK)
        protected Boolean avoidConsentCheck;

        public Configuration() {
            // empty
        }

        public Configuration (boolean rejectAny, boolean avoidPermissionCheck, boolean avoidConsentCheck) {
            this.rejectAny = rejectAny;
            this.avoidPermissionCheck = avoidPermissionCheck;
            this.avoidConsentCheck = avoidConsentCheck;
        }

        public Boolean isRejectAny() {
            return rejectAny;
        }

        public void setRejectAny(Boolean rejectAny) {
            this.rejectAny = rejectAny;
        }

        public Boolean isAvoidPermissionCheck() {
            return avoidPermissionCheck;
        }

        public void setAvoidPermissionCheck(Boolean avoidPermissionCheck) {
            this.avoidPermissionCheck = avoidPermissionCheck;
        }

        public Boolean isAvoidConsentCheck() {
            return avoidConsentCheck;
        }

        public void setAvoidConsentCheck(Boolean avoidConsentCheck) {
            this.avoidConsentCheck = avoidConsentCheck;
        }
    }

    public RejectMayActClaimExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return RejectMayActClaimExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.configuration = config != null ? config : new Configuration();
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (context instanceof AbstractTokenResponseContext tokenResponseContext) {
            validate(tokenResponseContext);
        }
    }

    private void validate(AbstractTokenResponseContext context) throws ClientPolicyException {
        Object mayActObject = context.getAccessTokenResponseBuilder().getAccessToken().getOtherClaims().get(IDToken.MAY_ACT);
        if (mayActObject == null) {
            return; // no may_act claim, just allow the token
        }

        if (Boolean.TRUE.equals(configuration.isRejectAny())) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "The may_act claim is rejected for this client");
        }

        if (!(mayActObject instanceof Map mayActMap)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid may_act claim in the token");
        }

        Object issObject = mayActMap.get(OIDCLoginProtocol.ISSUER);
        if (issObject != null && !issObject.equals(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), session.getContext().getRealm().getName()))) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST,  "Invalid issuer in the may_act claim in the token");
        }

        Object subjectObject = mayActMap.get(JsonWebToken.SUBJECT);
        if (!(subjectObject instanceof String subject)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST,  "Invalid may_act claim in the token");
        }

        // check the sub in the may_act is a valid user
        RealmModel realm = session.getContext().getRealm();
        UserModel user = context.getClientSession().getUserSession().getUser();
        UserModel admin = session.users().getUserById(realm, subject);
        if (admin == null || !admin.isEnabled() || admin.getId().equals(user.getId())) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid may_act sub in the token");
        }
        ClientModel client = null;
        if (admin.getServiceAccountClientLink() != null) {
            client = session.clients().getClientById(realm, admin.getServiceAccountClientLink());
            if (client == null || !client.isEnabled()) {
                logger.debugf("Invalid service account '%s' for user '%s' in realm '%s' in the may_act sub", admin.getUsername(), user.getUsername(), realm.getName());
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid may_act sub in the token");
            }
        }

        // check if the admin in sub has delegation permission over the user
        if (!Boolean.TRUE.equals(configuration.isAvoidPermissionCheck())) {
            AdminPermissionEvaluator evaluator = AdminPermissions.evaluator(session, realm, realm, admin);
            if (!evaluator.users().canDelegate(user)) {
                logger.debugf("Admin '%s' is not allowed to delegate as user '%s' in realm '%s'", admin.getUsername(), user.getUsername(), realm.getName());
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid may_act sub in the token");
            }
        }

        // check the consent is granted for the scope requested
        if (!Boolean.TRUE.equals(configuration.isAvoidConsentCheck())) {
            String scope = client != null
                    ? OIDCLoginProtocolFactory.CLIENT_DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + client.getClientId()
                    : OIDCLoginProtocolFactory.USER_DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + admin.getUsername();
            if (!TokenManager.isValidScope(session, scope, context.getClient())
                    || !TokenManager.verifyConsentStillAvailable(session, user, context.getClient(), context.getClientSession(), scope)) {
                logger.debugf("Consent '%s' is not granted for the may_act sub claim", scope);
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid may_act sub in the token");
            }
        }

        // it should be OK now, let the may_act pass
    }
}
