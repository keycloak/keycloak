package org.keycloak.testsuite.util;

import java.io.IOException;
import java.io.InputStream;
import org.keycloak.util.JsonSerialization;

/**
 *
 * @author tkyjovsk
 */
public class Json {

    public static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load json.", e);
        }
    }
}
