package org.keycloak.provider;

public class KeycloakDeploymentInfo {

    private String name;
    private boolean services;
    private boolean themes;
    private boolean themeResources;

    public boolean isProvider() {
        return services || themes || themeResources;
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

    public KeycloakDeploymentInfo services() {
        this.services = true;
        return this;
    }

    public boolean hasThemes() {
        return themes;
    }

    public KeycloakDeploymentInfo themes() {
        this.themes = true;
        return this;
    }

    public boolean hasThemeResources() {
        return themeResources;
    }

    public KeycloakDeploymentInfo themeResources() {
        themeResources = true;
        return this;
    }
}
