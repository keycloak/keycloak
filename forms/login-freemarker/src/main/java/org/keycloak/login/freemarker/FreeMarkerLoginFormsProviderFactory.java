package org.keycloak.login.freemarker;

import org.keycloak.Config;
import org.keycloak.freemarker.FreeMarkerUtil;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.login.LoginFormsProviderFactory;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerLoginFormsProviderFactory implements LoginFormsProviderFactory {

    private FreeMarkerUtil freeMarker;

    @Override
    public LoginFormsProvider create(ProviderSession providerSession) {
        return new FreeMarkerLoginFormsProvider(providerSession, freeMarker);
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
