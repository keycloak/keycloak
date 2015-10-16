package org.keycloak.common;

import org.keycloak.common.util.Time;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Version {
    public static final String UNKNOWN = "UNKNOWN";
    public static String VERSION;
    public static String RESOURCES_VERSION;
    public static String BUILD_TIME;

    static {
        Properties props = new Properties();
        InputStream is = Version.class.getResourceAsStream("/keycloak-version.properties");
        try {
            props.load(is);
            Version.VERSION = props.getProperty("version");
            Version.BUILD_TIME = props.getProperty("build-time");
            Version.RESOURCES_VERSION = Version.VERSION.toLowerCase();
            if (Version.RESOURCES_VERSION.endsWith("-snapshot")) {
                Version.RESOURCES_VERSION = Version.RESOURCES_VERSION.replace("-snapshot", "-" + Time.currentTime());
            }
        } catch (IOException e) {
            Version.VERSION= Version.UNKNOWN;
            Version.BUILD_TIME= Version.UNKNOWN;
        }

    }

}
