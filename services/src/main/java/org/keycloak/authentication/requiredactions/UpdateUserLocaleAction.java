package org.keycloak.authentication.requiredactions;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.component.ComponentModel;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.locale.LocaleUpdaterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;

public class UpdateUserLocaleAction implements RequiredActionProvider, RequiredActionFactory {

    @Override
    public String getDisplayText() {
        return "Update User Locale";
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        String userRequestedLocale = context.getAuthenticationSession().getAuthNote(LocaleSelectorProvider.USER_REQUEST_LOCALE);
        if (userRequestedLocale != null) {
            LocaleUpdaterProvider updater = context.getSession().getProvider(LocaleUpdaterProvider.class);
            updater.updateUsersLocale(context.getUser(), userRequestedLocale);
        } else {
            String userLocale = context.getUser().getFirstAttribute(UserModel.LOCALE);

            if (userLocale != null) {
                LocaleUpdaterProvider updater = context.getSession().getProvider(LocaleUpdaterProvider.class);
                updater.updateLocaleCookie(userLocale);
            } else {
                CookieProvider cookies = context.getSession().getProvider(CookieProvider.class);
                String cookieLocale = cookies.get(CookieType.LOCALE);
                if (cookieLocale != null && !cookieLocale.isEmpty() && isReadOnlyFederatedUser(context)) {
                    context.getSession().getProvider(LocaleUpdaterProvider.class).updateLocaleCookie(cookieLocale);
                } else {
                    context.getSession().getProvider(LocaleUpdaterProvider.class).expireLocaleCookie();
                }
            }
        }
    }

    private boolean isReadOnlyFederatedUser(RequiredActionContext context) {
        String federationLink = context.getUser().getFederationLink();
        if (federationLink == null) {
            return false;
        }

        ComponentModel component = context.getRealm().getComponent(federationLink);
        if (component == null) {
            return false;
        }

        String editMode = component.getConfig().getFirst(LDAPConstants.EDIT_MODE);
        return UserStorageProvider.EditMode.READ_ONLY.toString().equals(editMode);
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
