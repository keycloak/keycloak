package org.keycloak.provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultProviderLoaderFactory implements ProviderLoaderFactory {

    @Override
    public boolean supports(String type) {
        return false;
    }

    @Override
    public ProviderLoader create(ClassLoader baseClassLoader, String resource) {
        return new DefaultProviderLoader(baseClassLoader);
    }

}
