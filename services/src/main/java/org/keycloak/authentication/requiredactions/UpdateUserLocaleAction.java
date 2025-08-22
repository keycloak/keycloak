package org.keycloak.authentication.requiredactions;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.locale.LocaleUpdaterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.ArrayList;
import java.util.List;

public class UpdateUserLocaleAction implements RequiredActionProvider, RequiredActionFactory {

    public static final String UPDATE_ON_CLIENT_REQUESTED_LOCALE_KEY = "update_on_client_requested_locale";

    static List<ProviderConfigProperty> getUpdateOnClientRequestedLocalePropertyConfig() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(UPDATE_ON_CLIENT_REQUESTED_LOCALE_KEY)
                .label("Update on Client Requested Locale")
                .helpText("If enabled and client requested locale (ui_locales parameter) is present the user's locale is updated by this locale. If both user and client locales are present, the user requested locale takes precedence.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(Boolean.FALSE)
                .add()
                .build();
    }

    @Override
    public String getDisplayText() {
        return "Update User Locale";
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        boolean updateOnClientRequestedLocale = Boolean.parseBoolean(context.getConfig().getConfigValue(UPDATE_ON_CLIENT_REQUESTED_LOCALE_KEY));

        LocaleUpdaterProvider updater = context.getSession().getProvider(LocaleUpdaterProvider.class);

        String userRequestedLocale = context.getAuthenticationSession().getAuthNote(LocaleSelectorProvider.USER_REQUEST_LOCALE);
        String clientRequestedLocale = context.getAuthenticationSession ().getClientNote (LocaleSelectorProvider.CLIENT_REQUEST_LOCALE);
        if (userRequestedLocale != null) {
            updater.updateUsersLocale(context.getUser(), userRequestedLocale);
        } else if (updateOnClientRequestedLocale && clientRequestedLocale != null) {
            updater.updateUsersLocale(context.getUser(), clientRequestedLocale);
        } else {
            String userLocale = context.getUser().getFirstAttribute(UserModel.LOCALE);
            if (userLocale != null) {
                updater.updateLocaleCookie(userLocale);
            } else {
                updater.expireLocaleCookie();
            }
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
    }

    @Override
    public void processAction(RequiredActionContext context) {
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        List<ProviderConfigProperty> result = new ArrayList<>();
        result.addAll(RequiredActionFactory.super.getConfigMetadata());
        result.addAll(getUpdateOnClientRequestedLocalePropertyConfig());
        return List.copyOf(result);
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
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
        return "update_user_locale";
    }

}
