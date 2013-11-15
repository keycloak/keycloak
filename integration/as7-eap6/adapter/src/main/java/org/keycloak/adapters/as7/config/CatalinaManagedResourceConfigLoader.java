package org.keycloak.adapters.as7.config;

import org.apache.catalina.Context;
import org.keycloak.adapters.config.ManagedResourceConfigLoader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class CatalinaManagedResourceConfigLoader extends ManagedResourceConfigLoader {

    public CatalinaManagedResourceConfigLoader(Context context) {
        InputStream is = null;
        String path = context.getServletContext().getInitParameter("keycloak.config.file");
        if (path == null) {
            is = context.getServletContext().getResourceAsStream("/WEB-INF/keycloak.json");
        } else {
            try {
                is = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        loadConfig(is);
    }

}