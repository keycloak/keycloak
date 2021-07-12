package org.keycloak.authentication.authenticators.sessionlimits;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;

import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.Response;
import org.keycloak.events.Errors;
import org.keycloak.services.messages.Messages;

public class RealmSessionLimitsAuthenticator extends AbstractSessionLimitsAuthenticator {

    private static Logger logger = Logger.getLogger(RealmSessionLimitsAuthenticator.class);

    public RealmSessionLimitsAuthenticator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        Map<String, String> config = authenticatorConfig.getConfig();
        String behavior = config.get(RealmSessionLimitsAuthenticatorFactory.BEHAVIOR);
        String customErrorMessage = config.get(RealmSessionLimitsAuthenticatorFactory.ERROR_MESSAGE);
        int realmLimit = getIntConfigProperty(RealmSessionLimitsAuthenticatorFactory.REALM_LIMIT, config);

        Map<String, Long> activeClientSessionStats = session.sessions().getActiveClientSessionStats(context.getRealm(), false);
        long realmSessionCount = activeClientSessionStats.values().stream().reduce(0L, Long::sum);

        logger.debugf("realm limit: %s", realmLimit);
        logger.debugf("current session count within realm: %s", realmSessionCount);
        final boolean exceedsLimit = exceedsLimit(realmSessionCount, realmLimit);
        if (exceedsLimit) {
            logger.infof("Session count exceeded configured limit for realm. Count: %s, Limit: %s, Realm: %s", realmSessionCount, realmLimit, context.getRealm().getName());
            if (RealmSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION.equals(behavior)) {
                String errorMessage = Optional.ofNullable(customErrorMessage).orElse(Messages.SESSION_LIMIT_EXCEEDED);
                context.getEvent().error(Errors.SESSION_LIMIT_EXCEEDED);
                Response challenge = context.form()
                        .setError(errorMessage)
                        .createErrorPage(Response.Status.FORBIDDEN);
                context.failure(AuthenticationFlowError.SESSION_LIMIT_EXCEEDED, challenge);
                logger.infof("Denying access to user because the maximum number of sessions for this realm has been reached. Configured maximum: %s", realmLimit);
                return;
            }
        } 
        context.attempted();
    }
}
