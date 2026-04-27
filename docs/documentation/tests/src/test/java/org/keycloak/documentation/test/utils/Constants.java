package org.keycloak.documentation.test.utils;

import java.util.concurrent.TimeUnit;

public class Constants {

    public static final int HTTP_RETRY = 5;
    public static final int HTTP_CONNECTION_TIMEOUT = 30000;
    public static final int HTTP_READ_TIMEOUT = 300000;
    public static final long LINK_CHECK_EXPIRATION = TimeUnit.DAYS.toMillis(1);

}
