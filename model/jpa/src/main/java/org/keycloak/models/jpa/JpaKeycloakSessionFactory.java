package org.keycloak.models.jpa;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderSession;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaKeycloakSessionFactory implements KeycloakSessionFactory {

    protected EntityManagerFactory emf;

    @Override
    public void init(Config.Scope config) {
        emf = Persistence.createEntityManagerFactory("jpa-keycloak-identity-store", getHibernateProperties());
    }

    @Override
    public String getId() {
        return "jpa";
    }

    @Override
    public KeycloakSession create(ProviderSession providerSession) {
        return new JpaKeycloakSession(emf.createEntityManager());
    }

    @Override
    public void close() {
        emf.close();
    }

    // Allows to override some properties in persistence.xml by system properties
    protected Properties getHibernateProperties() {
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
