package org.keycloak.adapters.undertow;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ConfidentialPortManager;
import io.undertow.util.AttachmentKey;
import org.keycloak.adapters.AuthChallenge;
import org.keycloak.adapters.AuthOutcome;
import org.keycloak.adapters.KeycloakDeployment;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletKeycloakAuthMech implements AuthenticationMechanism {
    public static final AttachmentKey<AuthChallenge> KEYCLOAK_CHALLENGE_ATTACHMENT_KEY = AttachmentKey.create(AuthChallenge.class);

    protected KeycloakDeployment deployment;
    protected UndertowUserSessionManagement userSessionManagement;
    protected ConfidentialPortManager portManager;

    public ServletKeycloakAuthMech(KeycloakDeployment deployment, UndertowUserSessionManagement userSessionManagement, ConfidentialPortManager portManager) {
        this.deployment = deployment;
        this.userSessionManagement = userSessionManagement;
        this.portManager = portManager;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        UndertowHttpFacade facade = new UndertowHttpFacade(exchange);
        ServletRequestAuthenticator authenticator = new ServletRequestAuthenticator(facade, deployment,
                portManager.getConfidentialPort(exchange), securityContext, exchange, userSessionManagement);
        AuthOutcome outcome = authenticator.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            return AuthenticationMechanismOutcome.AUTHENTICATED;
        }
        AuthChallenge challenge = authenticator.getChallenge();
        if (challenge != null) {
            exchange.putAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY, challenge);
        }

        if (outcome == AuthOutcome.FAILED) {
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }
        return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        AuthChallenge challenge = exchange.getAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY);
        if (challenge != null) {
            UndertowHttpFacade facade = new UndertowHttpFacade(exchange);
            if (challenge.challenge(facade)) {
                return new ChallengeResult(true, exchange.getResponseCode());
            }
        }
        return new ChallengeResult(false);
    }
}
