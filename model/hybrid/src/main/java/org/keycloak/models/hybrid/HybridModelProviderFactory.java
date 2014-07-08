package org.keycloak.models.hybrid;

import org.keycloak.Config;
import org.keycloak.models.realms.RealmProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.ModelProviderFactory;
import org.keycloak.models.sessions.SessionProvider;
import org.keycloak.models.users.UserProvider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class HybridModelProviderFactory implements ModelProviderFactory {

    @Override
    public String getId() {
        return "hybrid";
    }

    @Override
    public ModelProvider create(KeycloakSession session) {
        return new HybridModelProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {
    }

}
