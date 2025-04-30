package org.keycloak.testframework.database;

import java.io.IOException;
import java.util.Properties;

public class DatabaseProperties {

    private static final Properties PROPERTIES = loadProperties();

    public static String getContainerImageName(String database) {
        return PROPERTIES.getProperty(database + ".container");
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try {
            properties.load(DatabaseProperties.class.getResourceAsStream("database.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

}
