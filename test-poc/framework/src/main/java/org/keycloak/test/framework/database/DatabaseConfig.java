package org.keycloak.test.framework.database;

import java.util.Collections;
import java.util.Map;

public interface DatabaseConfig {

    default String vendor() {
        return "";
    }

    default String containerImage() {
        return "";
    }

    default String urlHost() {
        return "";
    }

    default String username() {
        return "";
    }

    default String password() {
        return "";
    }

    default boolean isExternal() {
        return false;
    }
}
