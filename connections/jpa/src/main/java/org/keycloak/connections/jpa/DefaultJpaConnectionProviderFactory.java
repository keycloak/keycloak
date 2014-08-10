package org.keycloak.connections.jpa;

import org.hibernate.ejb.AvailableSettings;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultJpaConnectionProviderFactory implements JpaConnectionProviderFactory {

    private volatile EntityManagerFactory emf;

    private Config.Scope config;

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
        this.config = config;
    }

    private void lazyInit() {
        if (emf == null) {
            synchronized (this) {
                if (emf == null) {
                    String unitName = config.get("unitName");
                    Map<String, Object> properties = new HashMap<String, Object>();

                    // Only load config from keycloak-server.json if unitName is not specified
                    if (unitName == null) {
                        unitName = "keycloak-default";

                        String dataSource = config.get("dataSource");
                        if (dataSource != null) {
                            if (config.getBoolean("jta", false)) {
                                properties.put(AvailableSettings.JTA_DATASOURCE, dataSource);
                            } else {
                                properties.put(AvailableSettings.NON_JTA_DATASOURCE, dataSource);
                            }
                        } else {
                            properties.put(AvailableSettings.JDBC_URL, config.get("url"));
                            properties.put(AvailableSettings.JDBC_DRIVER, config.get("driver"));

                            String user = config.get("user");
                            if (user != null) {
                                properties.put(AvailableSettings.JDBC_USER, user);
                            }
                            String password = config.get("password");
                            if (password != null) {
                                properties.put(AvailableSettings.JDBC_PASSWORD, password);
                            }
                        }

                        String driverDialect = config.get("driverDialect");
                        if (driverDialect != null && driverDialect.length() > 0) {
                            properties.put("hibernate.dialect", driverDialect);
                        }

                        String databaseSchema = config.get("databaseSchema", "validate");
                        if (databaseSchema != null) {
                            properties.put("hibernate.hbm2ddl.auto", databaseSchema);
                        }

                        properties.put("hibernate.show_sql", config.getBoolean("showSql", false));
                        properties.put("hibernate.format_sql", config.getBoolean("formatSql", true));
                    }

                    emf = Persistence.createEntityManagerFactory(unitName, properties);
                }
            }
        }
    }

}
