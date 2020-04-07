package org.keycloak.adapters.servlet;

import org.junit.Test;
import org.keycloak.adapters.servlet.helpers.TestFilterConfig;
import org.keycloak.adapters.servlet.helpers.TestIdMapper;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import static org.junit.Assert.*;
import static org.keycloak.adapters.servlet.KeycloakOIDCFilter.CONFIG_ID_MAPPER_CLASS;
import static org.keycloak.adapters.servlet.KeycloakOIDCFilter.CONFIG_RESOLVER_PARAM;

public class KeycloakOIDCFilterTest {

    public static final FilterConfig TEST_FILTER_CONFIG = new TestFilterConfig() {

        @Override
        public String getInitParameter(String s) {
            if (CONFIG_RESOLVER_PARAM.equals(s)) {
                return "org.keycloak.adapters.servlet.TestConfigResolver";
            }
            if (CONFIG_ID_MAPPER_CLASS.equals(s)) {
                return "org.keycloak.adapters.servlet.TestIdMapper";
            }
            return null;
        }

    };

    @Test
    public void testIdMapperclass() throws ServletException {
        final KeycloakOIDCFilter keycloakOIDCFilter = new KeycloakOIDCFilter();

        keycloakOIDCFilter.init(TEST_FILTER_CONFIG);

        assertTrue(keycloakOIDCFilter.idMapper instanceof TestIdMapper);
    }

}