package org.keycloak.cookie;

public interface CookieMaxAge {

    int EXPIRED = 0;

    int SESSION = -1;

    int YEAR = 365 * 24 * 60 * 60;

}
