package org.keycloak.admin.ui.rest.test;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ui.extend.UiPageProvider;
import org.keycloak.services.ui.extend.UiPageProviderFactory;

import java.util.List;

/**
 * Test implementation this is should be removed.
 */
public class AdminUiPage implements UiPageProvider, UiPageProviderFactory<ComponentModel> {

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        System.out.println("Extra logic on create");
    }

    @Override
    public UiPageProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "Clients";
    }

    @Override
    public String getHelpText() {
        return "Clients are applications and services that can request authentication of a user.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("clientId")
                .label("Client ID")
                .helpText("The client identifier registered with the identity provider.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add().property()
                .name("name")
                .label("Name")
                .helpText("Specifies display name of the client. For example 'My Client'. Supports keys for localized values as well. For example: ${my_client}")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add().property()
                .name("description")
                .label("Description")
                .helpText("Specifies description of the client. For example 'My Client for TimeSheets'. Supports keys for localized values as well. For example: ${my_client_description}")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add().property()
                .name("alwaysDisplayInUI")
                .label("Always display in UI")
                .helpText("Always list this client in the Account UI, even if the user does not have an active session.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .add().property()
                .name("rootURL")
                .label("Root URL")
                .helpText("Always list this client in the Account UI, even if the user does not have an active session.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add().property()
                .name("homeURL")
                .label("Home URL")
                .helpText("Default URL to use when the auth server needs to redirect or link back to the client.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add().property()
                .name("validRedirectURIs")
                .label("Valid redirect URIs")
                .helpText("Default URL to use when the auth server needs to redirect or link back to the client.")
                .type(ProviderConfigProperty.MULTIVALUED_STRING_TYPE)
                .add().property()
                .name("flow")
                .label("Authentication flow")
                .type(ProviderConfigProperty.MULTIVALUED_LIST_TYPE)
                .options("Standard flow", "Implicit flow", "OAuth 2.0 Device Authorization Grant", "OIDC CIBA Grant", "Direct access grants", "Service accounts roles"

                )
                .add().build();
    }
}
