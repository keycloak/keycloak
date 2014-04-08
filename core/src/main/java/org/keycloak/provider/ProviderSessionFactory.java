package org.keycloak.provider;

import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ProviderSessionFactory {

    ProviderSession createSession();

    void close();

    <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz);

    <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz, String id);

    Set<String> providerIds(Class<? extends Provider> clazz);

    void init();

}
