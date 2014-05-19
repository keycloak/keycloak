package org.keycloak.freemarker;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ThemeSpi implements Spi {
    @Override
    public String getName() {
        return "theme";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ThemeProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ThemeProviderFactory.class;
    }
}
