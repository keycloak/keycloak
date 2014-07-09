package org.keycloak.models.sessions.mem;

import org.keycloak.model.test.AbstractUserSessionProviderTest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionProvider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MemUserSessionProviderTest extends AbstractUserSessionProviderTest {

    @Override
    public UserSessionProvider createProvider(KeycloakSession session) {
        return new MemUserSessionProviderFactory().create(session);
    }

}
