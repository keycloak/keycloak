package org.keycloak.testframework.util;

import java.io.IOException;
import java.util.Properties;

public class JavaPropertiesUtil {

    public static String getContainerImageName(String fileName, String containerName) {
        return loadProperties(fileName).getProperty(containerName + ".container");
    }

    private static Properties loadProperties(String fileName) {
        Properties properties = new Properties();
        try {
            properties.load(JavaPropertiesUtil.class.getResourceAsStream(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }
}
