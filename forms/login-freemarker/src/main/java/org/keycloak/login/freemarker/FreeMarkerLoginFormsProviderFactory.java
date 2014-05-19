package org.keycloak.login.freemarker;

import org.keycloak.Config;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.login.LoginFormsProviderFactory;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerLoginFormsProviderFactory implements LoginFormsProviderFactory {

    @Override
    public LoginFormsProvider create(ProviderSession providerSession) {
        return new FreeMarkerLoginFormsProvider(providerSession);
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
