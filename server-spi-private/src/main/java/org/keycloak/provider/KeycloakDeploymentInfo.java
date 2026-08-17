package org.keycloak.provider;

import java.util.ArrayList;
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
    private Set<Class<? extends ProviderFactory>> providerFactoryClasses = new LinkedHashSet<>();

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
     * Registers a {@link ProviderFactory} class without associating it with a {@link Spi} yet.
     * The SPI is resolved by assignability when factories are loaded for a given SPI.
     * Used for provider factories discovered at build time via the
     * {@link KeycloakProvider} annotation instead of a {@code META-INF/services} descriptor.
     */
    public void addProviderFactoryClass(Class<? extends ProviderFactory> factoryClass) {
        providerFactoryClasses.add(factoryClass);
    }

    public Set<Class<? extends ProviderFactory>> getProviderFactoryClasses() {
        return providerFactoryClasses;
    }
}
