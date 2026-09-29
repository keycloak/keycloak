package org.keycloak.tests.authz;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.Map;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.util.crypto.AuthzClientCryptoProvider;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthzClientTest {

    private CryptoProvider originalProvider;

    @BeforeEach
    public void setAuthzClientCryptoProvider() {
        originalProvider = getProviderOrNull();
        CryptoIntegration.setProvider(new AuthzClientCryptoProvider());
    }

    @AfterEach
    public void restoreCryptoProvider() {
        CryptoIntegration.setProvider(originalProvider);
    }

    @Test
    public void testCreateWithEnvVars() throws Exception {
        setEnv("KEYCLOAK_REALM", "test");
        setEnv("KEYCLOAK_AUTH_SERVER", "http://test");
        try {
            RuntimeException runtimeException = Assertions.assertThrows(RuntimeException.class, () -> {
                AuthzClient.create(new ByteArrayInputStream(("{\n"
                        + "  \"realm\": \"${env.KEYCLOAK_REALM}\",\n"
                        + "  \"auth-server-url\": \"${env.KEYCLOAK_AUTH_SERVER}\",\n"
                        + "  \"ssl-required\": \"external\",\n"
                        + "  \"enable-cors\": true,\n"
                        + "  \"resource\": \"my-server\",\n"
                        + "  \"credentials\": {\n"
                        + "    \"secret\": \"${env.KEYCLOAK_SECRET}\"\n"
                        + "  },\n"
                        + "  \"confidential-port\": 0,\n"
                        + "  \"policy-enforcer\": {\n"
                        + "    \"enforcement-mode\": \"ENFORCING\"\n"
                        + "  }\n"
                        + "}").getBytes()));
            });

            MatcherAssert.assertThat(runtimeException.getMessage(), Matchers.containsString("Could not obtain configuration from server"));
        } finally {
            removeEnv("KEYCLOAK_REALM");
            removeEnv("KEYCLOAK_AUTH_SERVER");
        }
    }

    private static CryptoProvider getProviderOrNull() {
        try {
            return CryptoIntegration.getProvider();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void setEnv(String key, String value) throws Exception {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map<String, String>) field.get(env)).put(key, value);
    }

    @SuppressWarnings("unchecked")
    private static void removeEnv(String key) throws Exception {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map<String, String>) field.get(env)).remove(key);
    }
}
