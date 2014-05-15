package org.keycloak.timer;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TimerSpi implements Spi {
    @Override
    public String getName() {
        return "timer";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return TimerProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return TimerProviderFactory.class;
    }
}
