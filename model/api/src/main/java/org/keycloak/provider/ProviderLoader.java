package org.keycloak.provider;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ProviderLoader {

    List<ProviderFactory> load(Spi spi);

}
