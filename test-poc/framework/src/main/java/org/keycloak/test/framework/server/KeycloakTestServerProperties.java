package org.keycloak.test.framework.server;

public class KeycloakTestServerProperties {

    public static final String KEYCLOAK_TEST_SERVER_ENV_KEY = "keycloak-server";

    public static final String KEYCLOAK_TEST_SERVER_PROP_EMBEDDED = "embedded";

    public static final String KEYCLOAK_TEST_SERVER_PROP_STANDALONE = "standalone";

    public static final String KEYCLOAK_TEST_SERVER_PROP_REMOTE = "remote";

    public static String KEYCLOAK_TEST_SERVER_ENV_VALUE = System.getProperty(KEYCLOAK_TEST_SERVER_ENV_KEY) == null
            || System.getProperty(KEYCLOAK_TEST_SERVER_ENV_KEY).isEmpty()
            ? KEYCLOAK_TEST_SERVER_PROP_EMBEDDED : System.getProperty(KEYCLOAK_TEST_SERVER_ENV_KEY);
}
