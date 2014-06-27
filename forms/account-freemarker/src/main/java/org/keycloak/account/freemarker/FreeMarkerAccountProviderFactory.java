package org.keycloak.account.freemarker;

import org.keycloak.Config;
import org.keycloak.account.AccountProvider;
import org.keycloak.account.AccountProviderFactory;
import org.keycloak.freemarker.FreeMarkerUtil;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerAccountProviderFactory implements AccountProviderFactory {

    private FreeMarkerUtil freeMarker;

    @Override
    public AccountProvider create(KeycloakSession session) {
        return new FreeMarkerAccountProvider(session, freeMarker);
    }

    @Override
    public void init(Config.Scope config) {
        freeMarker = new FreeMarkerUtil();
    }

    @Override
    public void close() {
        freeMarker = null;
    }

    @Override
    public String getId() {
        return "freemarker";
    }

}
