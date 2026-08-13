package org.keycloak.cookie;

import org.keycloak.provider.Provider;

public interface CookieProvider extends Provider {

    void set(CookieType cookieType, String value);

    void set(CookieType cookieType, String value, int maxAge);

    String get(CookieType cookieType);

    void expire(CookieType cookieType);

}
