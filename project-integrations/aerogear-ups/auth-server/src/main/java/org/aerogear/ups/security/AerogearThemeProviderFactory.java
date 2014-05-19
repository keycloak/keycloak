package org.aerogear.ups.security;

import org.keycloak.Config;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.freemarker.ThemeProviderFactory;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AerogearThemeProviderFactory implements ThemeProviderFactory {
    protected AerogearThemeProvider theme;

    @Override
    public ThemeProvider create(ProviderSession providerSession) {
        return theme;
    }

    @Override
    public void init(Config.Scope config) {
        theme = new AerogearThemeProvider();
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "aerogear";
    }
}
