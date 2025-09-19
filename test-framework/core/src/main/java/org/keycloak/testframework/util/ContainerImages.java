package org.keycloak.testframework.util;

import java.io.IOException;
import java.util.Properties;

public class ContainerImages {

    public static String getContainerImageName(String containerName) {
        return loadProperties().getProperty(containerName + ".container");
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try {
            properties.load(ContainerImages.class.getResourceAsStream("containers.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }
}
