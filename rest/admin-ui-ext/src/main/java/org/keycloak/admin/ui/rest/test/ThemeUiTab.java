package org.keycloak.admin.ui.rest.test;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.ui.extend.UiPageProvider;
import org.keycloak.services.ui.extend.UiTabProvider;
import org.keycloak.services.ui.extend.UiTabProviderFactory;
import org.keycloak.theme.Theme;
import org.keycloak.theme.ThemeProvider;
import org.keycloak.theme.ThemeProviderFactory;
import org.keycloak.theme.ThemeSelectorProvider;

import java.io.IOException;
import java.util.List;

public class ThemeUiTab implements UiPageProvider, UiTabProviderFactory<ComponentModel> {

    private KeycloakSession session;

    @Override
    public String getId() {
        return "Themes";
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        System.out.println("factory = " + factory);
    }

    @Override
    public void close() {

    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        final ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();
//        try {
//            final Theme theme = session.theme().getTheme(Theme.Type.LOGIN.name(), Theme.Type.LOGIN);
            builder.property()
                    .name("loginTheme")
                    .helpText("Select theme for login, OTP, grant, registration and forgot password pages.")
                    .add();
            //.options(theme.)

//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        return builder.build();
    }
}
