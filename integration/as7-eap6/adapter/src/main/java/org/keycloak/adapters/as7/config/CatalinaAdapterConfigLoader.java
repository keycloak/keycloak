package org.keycloak.adapters.as7.config;

import org.apache.catalina.Context;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.adapters.config.RealmConfigurationLoader;

import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class CatalinaAdapterConfigLoader extends RealmConfigurationLoader {
    private static final Logger log = Logger.getLogger(CatalinaAdapterConfigLoader.class);

    private InputStream getJSONFromServletContext(ServletContext servletContext) {
        String json = servletContext.getInitParameter(AdapterConstants.AUTH_DATA_PARAM_NAME);
        if (json == null) {
            return null;
        }
        log.info("**** using " + AdapterConstants.AUTH_DATA_PARAM_NAME);
        log.info(json);
        return new ByteArrayInputStream(json.getBytes());
    }

    public CatalinaAdapterConfigLoader(Context context) {
        log.info("******* Loading adapter config.");
        InputStream is = getJSONFromServletContext(context.getServletContext());
        if (is == null) {
            String path = context.getServletContext().getInitParameter("keycloak.config.file");
            if (path == null) {
                log.info("**** using /WEB-INF/keycloak.json");
                is = context.getServletContext().getResourceAsStream("/WEB-INF/keycloak.json");
            } else {
                try {
                    is = new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (is == null) throw new RuntimeException("Could not find keycloak config.");
        loadConfig(is);
    }

}