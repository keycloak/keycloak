package org.keycloak.provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ProviderLoaderFactory {

    boolean supports(String type);

    ProviderLoader create(ClassLoader baseClassLoader, String resource);

}
