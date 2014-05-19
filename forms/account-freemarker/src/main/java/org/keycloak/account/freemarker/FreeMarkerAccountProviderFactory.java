package org.keycloak.account.freemarker;

import org.keycloak.Config;
import org.keycloak.account.AccountProvider;
import org.keycloak.account.AccountProviderFactory;
import org.keycloak.provider.ProviderSession;

import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerAccountProviderFactory implements AccountProviderFactory {

    @Override
    public AccountProvider create(ProviderSession providerSession) {
        return new FreeMarkerAccountProvider(providerSession);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "freemarker";
    }

}
