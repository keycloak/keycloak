package org.keycloak.protocol.oidc;

import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.profile.CommaSeparatedListProfileConfigResolver;
import org.keycloak.models.KeycloakSession;
import org.keycloak.rar.AuthorizationRequestContext;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TokenManagerTest {

    private static ResteasyKeycloakSessionFactory sessionFactory;
    private static KeycloakSession session;

    @BeforeAll
    public static void beforeAll() {
        Profile.configure();
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
        sessionFactory = new ResteasyKeycloakSessionFactory();
        sessionFactory.init();
        session = new ResteasyKeycloakSession(sessionFactory);
    }

    @AfterAll
    public static void afterAll() {
        if (session != null) {
            session.close();
        }
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @AfterEach
    public void resetProfile() {
        Profile.reset();
    }

    @Test
    public void shouldUseProvidedAuthorizationRequestContextWhenParameterizedScopesEnabled() {
        Profile.configure(new CommaSeparatedListProfileConfigResolver(Profile.Feature.PARAMETERIZED_SCOPES.getVersionedKey(), ""));

        AuthorizationRequestContext authorizationRequestContext = new AuthorizationRequestContext(List.of());

        // If the provided authorizationRequestContext is honored, validation fails because it contains no scopes.
        // Before the fix, this overload ignored the provided context and attempted to rebuild one from session/client.
        boolean valid = TokenManager.isValidScope(session, "foo", authorizationRequestContext, null);

        assertFalse(valid);
    }
}
