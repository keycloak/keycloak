package org.keycloak.connections.jpa;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Properties;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultJpaConnectionProviderFactory implements JpaConnectionProviderFactory {

    private volatile EntityManagerFactory emf;
    private String unitName;

    @Override
    public JpaConnectionProvider create(KeycloakSession session) {
        lazyInit();

        EntityManager em = emf.createEntityManager();
        em = PersistenceExceptionConverter.create(em);
        session.getTransaction().enlist(new JpaKeycloakTransaction(em));
        return new DefaultJpaConnectionProvider(em);
    }

    @Override
    public void close() {
        if (emf != null) {
            emf.close();
        }
    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public void init(Config.Scope config) {
        unitName = config.get("unitName", "jpa-keycloak-identity-store");
    }

    private void lazyInit() {
        if (emf == null) {
            synchronized (this) {
                if (emf == null) {
                    emf = Persistence.createEntityManagerFactory(unitName, getHibernateProperties());
                }
            }
        }
    }

    private Properties getHibernateProperties() {
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
