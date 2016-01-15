package org.keycloak.provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface Spi {

    boolean isInternal();
    String getName();
    Class<? extends Provider> getProviderClass();
    Class<? extends ProviderFactory> getProviderFactoryClass();

}
