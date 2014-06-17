package org.keycloak.util;

import java.util.Properties;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JpaUtils {

    // Allows to override some properties in persistence.xml by system properties
    public static Properties getHibernateProperties() {
        Properties result = new Properties();

        for (Object property : System.getProperties().keySet()) {
            if (property.toString().startsWith("hibernate.")) {
                String propValue = System.getProperty(property.toString());
                result.put(property, propValue);
            }
        }
        return result;
    }
}
