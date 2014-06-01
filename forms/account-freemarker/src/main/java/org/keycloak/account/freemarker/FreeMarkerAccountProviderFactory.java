package org.keycloak.account.freemarker;

import org.keycloak.Config;
import org.keycloak.account.AccountProvider;
import org.keycloak.account.AccountProviderFactory;
import org.keycloak.freemarker.FreeMarkerUtil;
import org.keycloak.provider.ProviderSession;

import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerAccountProviderFactory implements AccountProviderFactory {

    private FreeMarkerUtil freeMarker;

    @Override
    public AccountProvider create(ProviderSession providerSession) {
        return new FreeMarkerAccountProvider(providerSession, freeMarker);
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
