package org.keycloak.locale;

import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

public interface LocaleUpdaterProvider extends Provider {

    void updateUsersLocale(UserModel user, String locale);

    void updateLocaleCookie(String locale);

    void expireLocaleCookie();

}
