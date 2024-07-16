package org.keycloak.test.framework.database;

import java.util.HashMap;
import java.util.Map;

public class DatabaseConfig {

    private String vendor;
    private String containerImage;
    private String urlHost;
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

    public String getUrlHost() {
        return urlHost;
    }

    public DatabaseConfig urlHost(String urlHost) {
        this.urlHost = urlHost;
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
        if (urlHost != null) {
            config.put("db-url-host", urlHost);
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
