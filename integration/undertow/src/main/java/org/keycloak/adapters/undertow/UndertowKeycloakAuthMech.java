package org.keycloak.adapters.undertow;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.keycloak.adapters.AuthChallenge;
import org.keycloak.adapters.AuthOutcome;
import org.keycloak.adapters.RequestAuthenticator;
import static org.keycloak.adapters.undertow.ServletKeycloakAuthMech.KEYCLOAK_CHALLENGE_ATTACHMENT_KEY;

/**
 * Abstract base class for a Keycloak-enabled Undertow AuthenticationMechanism.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public abstract class UndertowKeycloakAuthMech implements AuthenticationMechanism {
    public static final AttachmentKey<AuthChallenge> KEYCLOAK_CHALLENGE_ATTACHMENT_KEY = AttachmentKey.create(AuthChallenge.class);

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

    /**
     * Call this inside your authenticate method.
     */
    protected AuthenticationMechanismOutcome keycloakAuthenticate(HttpServerExchange exchange, RequestAuthenticator authenticator) {
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

}