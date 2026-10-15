package org.keycloak.tests.providers.ui;

import java.util.Collections;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.ui.extend.UiScriptProviderFactory;

public class ExampleUiScriptProviderFactory implements UiScriptProviderFactory<ExampleUiScriptProvider> {

    public static final String ID = "example-ui-script";

    @Override
    public ExampleUiScriptProvider create(KeycloakSession session, ComponentModel model) {
        return new ExampleUiScriptProvider();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getHelpText() {
        return "Example UI script extension for testing";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public String getTagName() {
        return "example-ui-widget";
    }

    @Override
    public String getScriptPath() {
        return "ui-scripts/example-widget.js";
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
}
