package org.keycloak.tests.providers.ui;

import org.keycloak.services.ui.extend.UiScriptProvider;

public class ExampleUiScriptProvider implements UiScriptProvider {

    @Override
    public void close() {
    }

    @Override
    public String getHelpText() {
        return "";
    }
}
