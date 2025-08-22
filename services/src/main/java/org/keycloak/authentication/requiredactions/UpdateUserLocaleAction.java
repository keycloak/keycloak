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

public class UpdateUserLocaleAction implements RequiredActionProvider, RequiredActionFactory {

    private boolean updateOnClientRequestedLocale;

    @Override
    public String getDisplayText() {
        return "Update User Locale";
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
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
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        this.updateOnClientRequestedLocale = config.getBoolean("updateOnClientRequestedLocale", false);
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
