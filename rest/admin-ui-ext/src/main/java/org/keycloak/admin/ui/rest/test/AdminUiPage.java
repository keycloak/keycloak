package org.keycloak.admin.ui.rest.test;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ui.extend.UiPageProvider;
import org.keycloak.services.ui.extend.UiPageProviderFactory;

import java.util.List;
import java.util.Map;

/**
 * Test implementation this is should be removed.
 */
public class AdminUiPage implements UiPageProvider, UiPageProviderFactory<ComponentModel> {

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        System.out.println("save model = " + model);
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
        return "admin-ui-page";
    }

    @Override
    public String getHelpText() {
        return "Who needs help";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("test")
                .label("Test attribute")
                .type(ProviderConfigProperty.MULTIVALUED_STRING_TYPE)
                .helpText("Array of test values")
                .add().property()
                .name("other")
                .label("Other")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("Other field that you can edit")
                .add().property()
                .name("switch")
                .label("Switch")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .helpText("This will be a switch")
                .add().build();
    }
}
