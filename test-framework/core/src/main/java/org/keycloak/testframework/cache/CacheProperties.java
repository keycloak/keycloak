package org.keycloak.testframework.cache;

import java.io.IOException;
import java.util.Properties;

public class CacheProperties {

    private static final Properties PROPERTIES = loadProperties();

    public static String getContainerImageName() {
        return PROPERTIES.getProperty("infinispan.container");
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try {
            properties.load(CacheProperties.class.getResourceAsStream("cache.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }
}
