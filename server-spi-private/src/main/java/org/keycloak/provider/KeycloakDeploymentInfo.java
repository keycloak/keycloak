package org.keycloak.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KeycloakDeploymentInfo {

    private String name;
    private boolean services;
    private boolean themes;
    private boolean themeResources;
    private Map<Class<? extends Spi>, List<ProviderFactory>> providers = new HashMap<>();
    private Map<Class<? extends ProviderFactory>, Set<Class<? extends ProviderFactory>>> providerFactoryClasses = new HashMap<>();

    public boolean isProvider() {
        return services || themes || themeResources || !providers.isEmpty() || !providerFactoryClasses.isEmpty();
    }

    public boolean hasServices() {
        return services;
    }

    public static KeycloakDeploymentInfo create() {
        return new KeycloakDeploymentInfo();
    }

    private KeycloakDeploymentInfo() {
    }

    public KeycloakDeploymentInfo name(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    /**
     * Enables discovery of services via {@link java.util.ServiceLoader}.
     * @return
     */
    public KeycloakDeploymentInfo services() {
        this.services = true;
        return this;
    }

    public boolean hasThemes() {
        return themes;
    }

    /**
     * Enables discovery embedded themes.
     * @return
     */
    public KeycloakDeploymentInfo themes() {
        this.themes = true;
        return this;
    }

    public boolean hasThemeResources() {
        return themeResources;
    }

    /**
     * Enables discovery of embedded theme-resources.
     * @return
     */
    public KeycloakDeploymentInfo themeResources() {
        themeResources = true;
        return this;
    }

    public void addProvider(Class<? extends Spi> spi, ProviderFactory factory) {
        providers.computeIfAbsent(spi, key -> new ArrayList<>()).add(factory);
    }

    public Map<Class<? extends Spi>, List<ProviderFactory>> getProviders() {
        return providers;
    }

    /**
     * Registers a {@link ProviderFactory} class for the SPI identified by its provider factory
     * interface — the same association a {@code META-INF/services/<factoryInterface>} descriptor
     * expresses. Used for provider factories discovered at build time via the
     * {@link KeycloakProvider} annotation.
     *
     * @param factoryInterface the SPI's provider factory interface (see {@link Spi#getProviderFactoryClass()})
     * @param factoryClass the implementation to register for that SPI
     */
    public void addProviderFactoryClass(Class<? extends ProviderFactory> factoryInterface, Class<? extends ProviderFactory> factoryClass) {
        providerFactoryClasses.computeIfAbsent(factoryInterface, key -> new LinkedHashSet<>()).add(factoryClass);
    }

    /**
     * @return the factory classes registered for the SPI identified by {@code factoryInterface}, in registration order
     */
    public Set<Class<? extends ProviderFactory>> getProviderFactoryClasses(Class<? extends ProviderFactory> factoryInterface) {
        return providerFactoryClasses.getOrDefault(factoryInterface, Collections.emptySet());
    }

    public Map<Class<? extends ProviderFactory>, Set<Class<? extends ProviderFactory>>> getProviderFactoryClasses() {
        return providerFactoryClasses;
    }
}
