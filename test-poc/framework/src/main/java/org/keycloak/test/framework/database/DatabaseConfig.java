package org.keycloak.test.framework.database;

import java.util.HashMap;
import java.util.Map;

public class DatabaseConfig {

    private String vendor;
    private String containerImage;
    private String url;
    private String username;
    private String password;

    public String getVendor() {
        return vendor;
    }

    public DatabaseConfig vendor(String vendor) {
        this.vendor = vendor;
        return this;
    }

    public String getContainerImage() {
        return containerImage;
    }

    public DatabaseConfig containerImage(String containerImage) {
        this.containerImage = containerImage;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public DatabaseConfig url(String url) {
        this.url = url;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public DatabaseConfig username(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public DatabaseConfig password(String password) {
        this.password = password;
        return this;
    }

    public Map<String, String> toConfig() {
        Map<String, String> config = new HashMap<>();
        if (vendor != null) {
            config.put("db", vendor);
        }
        if (url != null) {
            config.put("db-url", url);
        }
        if (username != null) {
            config.put("db-username", username);
        }
        if (password != null) {
            config.put("db-password", password);
        }
        return config;
    }

}
