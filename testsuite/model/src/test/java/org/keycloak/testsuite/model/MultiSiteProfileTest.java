package org.keycloak.testsuite.model;

import org.keycloak.common.Profile;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.sessions.infinispan.PersistentUserSessionProvider;
import org.keycloak.models.sessions.infinispan.remote.RemoteInfinispanAuthenticationSessionProvider;
import org.keycloak.models.sessions.infinispan.remote.RemoteInfinispanSingleUseObjectProvider;
import org.keycloak.models.sessions.infinispan.remote.RemoteUserLoginFailureProvider;
import org.keycloak.sessions.AuthenticationSessionProvider;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assume.assumeTrue;

public class MultiSiteProfileTest extends KeycloakModelTest {

    @Test
    public void testMultiSiteConfiguredCorrectly() {
        assumeTrue(Profile.isFeatureEnabled(Profile.Feature.MULTI_SITE));
        assumeTrue(Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS));

        inComittedTransaction(session -> {
            UserSessionProvider sessions = session.sessions();
            assertThat(sessions, instanceOf(PersistentUserSessionProvider.class));

            AuthenticationSessionProvider authenticationSessionProvider = session.authenticationSessions();
            assertThat(authenticationSessionProvider, instanceOf(RemoteInfinispanAuthenticationSessionProvider.class));

            UserLoginFailureProvider userLoginFailureProvider = session.loginFailures();
            assertThat(userLoginFailureProvider, instanceOf(RemoteUserLoginFailureProvider.class));

            SingleUseObjectProvider singleUseObjectProvider = session.singleUseObjects();
            assertThat(singleUseObjectProvider, instanceOf(RemoteInfinispanSingleUseObjectProvider.class));
        });
    }
}
