package org.keycloak.provider;

import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultProviderLoader implements ProviderLoader {

    private ClassLoader classLoader;

    public DefaultProviderLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public List<ProviderFactory> load(Spi spi) {
        LinkedList<ProviderFactory> list = new LinkedList<ProviderFactory>();
        for (ProviderFactory f : ServiceLoader.load(spi.getProviderFactoryClass(), classLoader)) {
            list.add(f);
        }
        return list;
    }

}
