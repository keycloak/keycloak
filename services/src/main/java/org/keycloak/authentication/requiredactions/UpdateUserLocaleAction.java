package org.keycloak.authentication.requiredactions;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;

public class UpdateUserLocaleAction implements RequiredActionProvider, RequiredActionFactory {

    @Override
    public String getDisplayText() {
        return "Update User Locale";
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        String userRequestedLocale = context.getAuthenticationSession().getAuthNote(LocaleSelectorProvider.USER_REQUEST_LOCALE);
        if (userRequestedLocale != null) {
            LocaleSelectorProvider provider = context.getSession().getProvider(LocaleSelectorProvider.class);
            provider.updateUsersLocale(context.getUser(), userRequestedLocale);
        } else {
            String userLocale = context.getUser().getFirstAttribute(UserModel.LOCALE);
            LocaleSelectorProvider provider = context.getSession().getProvider(LocaleSelectorProvider.class);
            if (userLocale != null) {
                provider.updateLocaleCookie(context.getRealm(), userLocale, context.getUriInfo());
            } else {
                provider.expireLocaleCookie(context.getRealm(), context.getUriInfo());
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
