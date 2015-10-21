package org.keycloak.common.util;

import java.util.Properties;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SystemEnvProperties extends Properties {

    @Override
    public String getProperty(String key) {
        if (key.startsWith("env.")) {
            return System.getenv().get(key.substring(4));
        } else {
            return System.getProperty(key);
        }
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

}
