package org.keycloak.authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers {@link AuthenticationSelectionResolver#isSelectableForCurrentUser}, added so a
 * non-CredentialValidator authenticator that requires a user is only offered as a selectable
 * alternative ("Try another way") when it's actually usable by that user.
 */
class AuthenticationSelectionResolverTest {

    private AuthenticationProcessor processor;
    private AuthenticationExecutionModel execution;
    private Authenticator authenticator;
    private AuthenticatorFactory factory;
    private KeycloakSession session;
    private KeycloakSessionFactory sessionFactory;
    private RealmModel realm;
    private AuthenticationSessionModel authSession;
    private UserModel user;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        sessionFactory = mock(KeycloakSessionFactory.class);
        realm = mock(RealmModel.class);
        authSession = mock(AuthenticationSessionModel.class);
        user = mock(UserModel.class);
        execution = mock(AuthenticationExecutionModel.class);
        authenticator = mock(Authenticator.class);
        factory = mock(AuthenticatorFactory.class);

        processor = mock(AuthenticationProcessor.class);
        when(processor.getSession()).thenReturn(session);
        when(processor.getRealm()).thenReturn(realm);
        when(processor.getAuthenticationSession()).thenReturn(authSession);
        when(session.getKeycloakSessionFactory()).thenReturn(sessionFactory);
        when(execution.getAuthenticator()).thenReturn("custom-authenticator");
        when(sessionFactory.getProviderFactory(Authenticator.class, "custom-authenticator")).thenReturn(factory);
    }

    @Test
    void authenticatorNotRequiringUserIsAlwaysSelectable() {
        when(authenticator.requiresUser()).thenReturn(false);

        assertTrue(AuthenticationSelectionResolver.isSelectableForCurrentUser(processor, execution, authenticator));
    }

    @Test
    void noAuthenticatedUserYetIsSelectable() {
        when(authenticator.requiresUser()).thenReturn(true);
        when(authSession.getAuthenticatedUser()).thenReturn(null);

        assertTrue(AuthenticationSelectionResolver.isSelectableForCurrentUser(processor, execution, authenticator));
    }

    @Test
    void configuredForCurrentUserIsSelectable() {
        when(authenticator.requiresUser()).thenReturn(true);
        when(authSession.getAuthenticatedUser()).thenReturn(user);
        when(authenticator.configuredFor(session, realm, user)).thenReturn(true);

        assertTrue(AuthenticationSelectionResolver.isSelectableForCurrentUser(processor, execution, authenticator));
    }

    @Test
    void notConfiguredButUserSetupAllowedIsSelectable() {
        when(authenticator.requiresUser()).thenReturn(true);
        when(authSession.getAuthenticatedUser()).thenReturn(user);
        when(authenticator.configuredFor(session, realm, user)).thenReturn(false);
        when(factory.isUserSetupAllowed()).thenReturn(true);

        assertTrue(AuthenticationSelectionResolver.isSelectableForCurrentUser(processor, execution, authenticator));
    }

    @Test
    void notConfiguredAndUserSetupNotAllowedIsNotSelectable() {
        when(authenticator.requiresUser()).thenReturn(true);
        when(authSession.getAuthenticatedUser()).thenReturn(user);
        when(authenticator.configuredFor(session, realm, user)).thenReturn(false);
        when(factory.isUserSetupAllowed()).thenReturn(false);

        assertFalse(AuthenticationSelectionResolver.isSelectableForCurrentUser(processor, execution, authenticator));
    }
}
