package org.keycloak.provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ProviderSessionFactory {

    ProviderSession createSession();

    void close();

    void init();

}
