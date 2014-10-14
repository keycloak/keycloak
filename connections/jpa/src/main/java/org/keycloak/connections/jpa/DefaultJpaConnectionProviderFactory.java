package org.keycloak.connections.jpa;

import org.hibernate.ejb.AvailableSettings;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.jpa.updater.JpaUpdaterProvider;
import org.keycloak.models.KeycloakSession;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultJpaConnectionProviderFactory implements JpaConnectionProviderFactory {

    private static final Logger logger = Logger.getLogger(DefaultJpaConnectionProviderFactory.class);

    private volatile EntityManagerFactory emf;

    private Config.Scope config;

    @Override
    public JpaConnectionProvider create(KeycloakSession session) {
        lazyInit(session);

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

    private void lazyInit(KeycloakSession session) {
        if (emf == null) {
            synchronized (this) {
                if (emf == null) {
                    logger.debug("Initializing JPA connections");

                    Connection connection = null;

                    String unitName = config.get("unitName");
                    String databaseSchema = config.get("databaseSchema");

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

                        if (databaseSchema != null) {
                            if (databaseSchema.equals("development-update")) {
                                properties.put("hibernate.hbm2ddl.auto", "update");
                                databaseSchema = null;
                            } else if (databaseSchema.equals("development-validate")) {
                                properties.put("hibernate.hbm2ddl.auto", "validate");
                                databaseSchema = null;
                            }
                        }

                        properties.put("hibernate.show_sql", config.getBoolean("showSql", false));
                        properties.put("hibernate.format_sql", config.getBoolean("formatSql", true));
                    }

                    if (databaseSchema != null) {
                        logger.trace("Updating database");

                        JpaUpdaterProvider updater = session.getProvider(JpaUpdaterProvider.class);
                        connection = getConnection();

                        if (databaseSchema.equals("update")) {
                            String currentVersion = null;
                            try {
                                ResultSet resultSet = connection.createStatement().executeQuery(updater.getCurrentVersionSql());
                                if (resultSet.next()) {
                                    currentVersion = resultSet.getString(1);
                                }
                            } catch (SQLException e) {
                            }

                            if (currentVersion == null || !JpaUpdaterProvider.LAST_VERSION.equals(currentVersion)) {
                                updater.update(connection);
                            } else {
                                logger.debug("Database is up to date");
                            }
                        } else if (databaseSchema.equals("validate")) {
                            updater.validate(connection);
                        } else {
                            throw new RuntimeException("Invalid value for databaseSchema: " + databaseSchema);
                        }

                        logger.trace("Database update completed");
                    }

                    logger.trace("Creating EntityManagerFactory");
                    emf = Persistence.createEntityManagerFactory(unitName, properties);
                    logger.trace("EntityManagerFactory created");

                    // Close after creating EntityManagerFactory to prevent in-mem databases from closing
                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (SQLException e) {
                            logger.warn(e);
                        }
                    }
                }
            }
        }
    }

    private Connection getConnection() {
        try {
            String dataSourceLookup = config.get("dataSource");
            if (dataSourceLookup != null) {
                DataSource dataSource = (DataSource) new InitialContext().lookup(dataSourceLookup);
                return dataSource.getConnection();
            } else {
                Class.forName(config.get("driver"));
                return DriverManager.getConnection(config.get("url"), config.get("user"), config.get("password"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

}
