package org.keycloak.adapters;

import org.keycloak.constants.GenericConstants;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FindFile {
    public static InputStream findFile(String keycloakConfigFile) {
        if (keycloakConfigFile.startsWith(GenericConstants.PROTOCOL_CLASSPATH)) {
            String classPathLocation = keycloakConfigFile.replace(GenericConstants.PROTOCOL_CLASSPATH, "");
            // Try current class classloader first
            InputStream is = FindFile.class.getClassLoader().getResourceAsStream(classPathLocation);
            if (is == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classPathLocation);
            }

            if (is != null) {
                return is;
            } else {
                throw new RuntimeException("Unable to find config from classpath: " + keycloakConfigFile);
            }
        } else {
            // Fallback to file
            try {
                return new FileInputStream(keycloakConfigFile);
            } catch (FileNotFoundException fnfe) {
                throw new RuntimeException(fnfe);
            }
        }
    }
}
