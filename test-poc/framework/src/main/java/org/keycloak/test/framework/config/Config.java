package org.keycloak.test.framework.config;

import org.keycloak.test.framework.injection.ValueTypeAlias;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {

    private final static Config instance = new Config();

    private Properties localEnv = new Properties();

    private Config() {
        File envFile = new File(".env");
        if (envFile.isFile()) {
            try {
                localEnv.load(new FileInputStream(envFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Config getInstance() {
        return instance;
    }

    public String getSelectedSupplier(Class valueType) {
        return getString("kc-test-" + ValueTypeAlias.getAlias(valueType));
    }

    public String getString(String key) {
        String propKey = key.replace('-', '.');
        String envKey = key.replace('-', '_').toUpperCase();

        String value = System.getProperty(propKey);
        if (value != null) {
            return value;
        }

        value = System.getenv(envKey);
        if (value != null) {
            return value;
        }

        value = localEnv.getProperty(envKey);
        if (value != null) {
            return value;
        }

        return null;
    }

}
