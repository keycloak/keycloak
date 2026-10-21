package org.keycloak.config.database;

import java.util.function.Function;

import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.Option;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class DatabaseTest {

    @Test
    void oracleDefaultUrlAppendsDbUrlProperties() {
        Function<Option<?>, String> getter = option -> {
            if (option == DatabaseOptions.DB_URL_PROPERTIES) {
                return "?oracle.net.ssl_server_dn_match=true";
            }
            return null;
        };

        String url = Database.getDefaultUrl(getter, null, "oracle").orElseThrow();

        assertThat(url, is("jdbc:oracle:thin:@//localhost:1521/keycloak?oracle.net.ssl_server_dn_match=true"));
    }

    @Test
    void oracleDefaultUrlWithoutDbUrlProperties() {
        String url = Database.getDefaultUrl(option -> null, null, "oracle").orElseThrow();

        assertThat(url, is("jdbc:oracle:thin:@//localhost:1521/keycloak"));
    }

}
