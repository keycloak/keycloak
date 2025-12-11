/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.connections.jpa.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.ValidationMode;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.models.KeycloakSession;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JpaUtils {

    public static final String HIBERNATE_DEFAULT_SCHEMA = "hibernate.default_schema";
    public static final String QUERY_NATIVE_SUFFIX = "[native]";
    public static final String QUERY_JPQL_SUFFIX = "[jpql]";
    private static final Logger logger = Logger.getLogger(JpaUtils.class);

    public static String getTableNameForNativeQuery(String tableName, EntityManager em) {
        final Dialect dialect = em.getEntityManagerFactory().unwrap(SessionFactoryImpl.class).getJdbcServices().getDialect();
        IdentifierHelper identifierHelper = em.getEntityManagerFactory().unwrap(SessionFactoryImpl.class).getJdbcServices().getJdbcEnvironment().getIdentifierHelper();
        String schema = em.getEntityManagerFactory().unwrap(SessionFactoryImpl.class).getSessionFactoryOptions().getDefaultSchema();
        return (schema==null) ? tableName : identifierHelper.toIdentifier(schema).render(dialect) + "." + tableName;
    }

    private static List<ParsedPersistenceXmlDescriptor> transformPersistenceUnits(Collection<PersistenceUnitDescriptor> descriptors) {
        return descriptors.stream().map(descriptor -> (ParsedPersistenceXmlDescriptor) descriptor).collect(Collectors.toList());
    }

    public static EntityManagerFactory createEntityManagerFactory(KeycloakSession session, String unitName, Map<String, Object> properties, boolean jta) {
        PersistenceUnitTransactionType txType = jta ? PersistenceUnitTransactionType.JTA : PersistenceUnitTransactionType.RESOURCE_LOCAL;
        PersistenceXmlParser parser = PersistenceXmlParser.create(properties);
        List<URL> urls = parser.getClassLoaderService().locateResources("META-INF/persistence.xml");

        List<ParsedPersistenceXmlDescriptor> persistenceUnits = urls.isEmpty() ? new ArrayList<>() : transformPersistenceUnits(parser.parse(urls).values());
        ParsedPersistenceXmlDescriptor defaultPersistenceUnit = transformPersistenceUnits(parser.parse(Collections.singletonList(JpaUtils.class.getClassLoader().getResource("default-persistence.xml")), txType)
                .values())
                .stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot find the file 'default-persistence.xml'"));
        persistenceUnits.add(defaultPersistenceUnit);

        for (ParsedPersistenceXmlDescriptor persistenceUnit : persistenceUnits) {
            if (persistenceUnit.getName().equals(unitName)) {
                List<Class<?>> providedEntities = getProvidedEntities(session);
                for (Class<?> entityClass : providedEntities) {
                    // Add all extra entity classes to the persistence unit.
                    persistenceUnit.addClasses(entityClass.getName());
                }
                // Now build the entity manager factory, supplying a proxy classloader, so Hibernate will be able
                // to find and load the extra provided entities.
                persistenceUnit.setTransactionType(txType);
                persistenceUnit.setValidationMode(ValidationMode.NONE.name());
                return Bootstrap.getEntityManagerFactoryBuilder(persistenceUnit, properties).build();
            }
        }
        throw new RuntimeException("Persistence unit '" + unitName + "' not found");
    }

    /**
     * Get a list of all provided entities by looping over all configured entity providers.
     *
     * @param session the keycloak session
     * @return a list of all provided entities (can be an empty list)
     */
    public static List<Class<?>> getProvidedEntities(KeycloakSession session) {
        List<Class<?>> providedEntityClasses = new ArrayList<>();
        // Get all configured entity providers.
        Set<JpaEntityProvider> entityProviders = session.getAllProviders(JpaEntityProvider.class);
        // For every provider, add all entity classes to the list.
        for (JpaEntityProvider entityProvider : entityProviders) {
            providedEntityClasses.addAll(entityProvider.getEntities());
        }
        return providedEntityClasses;
    }

    /**
     * Get the name of custom table for liquibase updates for give ID of JpaEntityProvider
     * @param jpaEntityProviderFactoryId
     * @return table name
     */
    public static String getCustomChangelogTableName(String jpaEntityProviderFactoryId) {
        String upperCased = jpaEntityProviderFactoryId.toUpperCase();
        upperCased = upperCased.replaceAll("-", "_");
        upperCased = upperCased.replaceAll("[^A-Z_]", "");
        return "DATABASECHANGELOG_" + upperCased.substring(0, Math.min(10, upperCased.length()));
    }

    /**
     * Loads the URL as a properties file.
     * @param url The url to load, it can be null
     * @return A properties file with the url loaded or null
     */
    public static Properties loadSqlProperties(URL url) {
        if (url == null) {
            return null;
        }
        Properties props = new Properties();
        try (InputStream is = url.openStream()) {
            props.load(is);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return props;
    }

    /**
     * Returns the name of the query in the queries file. It searches for the
     * three possible forms: name[native], name[jpql] or name.
     * @param name The name of the query to search
     * @param queries The properties file with the queries
     * @return The key with the query found or null if not found
     */
    private static String getQueryFromProperties(String name, Properties queries) {
        if (queries == null) {
            return null;
        }
        String nameFull = name + QUERY_NATIVE_SUFFIX;
        if (queries.containsKey(nameFull)) {
            return nameFull;
        }
        nameFull = name + QUERY_JPQL_SUFFIX;
        if (queries.containsKey(nameFull)) {
            return nameFull;
        }
        nameFull = name;
        if (queries.containsKey(nameFull)) {
            return nameFull;
        }
        return null;
    }

    /**
     * Returns the query name but removing the suffix.
     * @param name The query name as it is on the key
     * @return The name without the suffix
     */
    private static String getQueryShortName(String name) {
        if (name.endsWith(QUERY_NATIVE_SUFFIX)) {
            return name.substring(0, name.length() - QUERY_NATIVE_SUFFIX.length());
        } else if (name.endsWith(QUERY_JPQL_SUFFIX)) {
            return name.substring(0, name.length() - QUERY_JPQL_SUFFIX.length());
        } else {
            return name;
        }
    }

    /**
     * Method that adds the different query variants for the database.
     * The method loads the queries specified in the files
     * <em>META-INF/queries-{dbType}.properties</em> and the default
     * <em>META-INF/queries-default.properties</em>. At least the default file
     * should exist inside the jar file. The default file contains all the
     * needed queries and the specific one can overload all or some of them for
     * that database type.
     * @param databaseType The database type as managed in
     * @return
     */
    public static Properties loadSpecificNamedQueries(String databaseType) {
        URL specificUrl = JpaUtils.class.getClassLoader().getResource("META-INF/queries-" + databaseType + ".properties");

        Properties specificQueries = loadSqlProperties(specificUrl);
        Properties queries = new Properties();
        if (specificQueries == null) {
            return queries;
        }

        for (String queryNameFull : specificQueries.stringPropertyNames()) {
            String querySql = specificQueries.getProperty(queryNameFull);
            String queryName = getQueryShortName(queryNameFull);
            String specificQueryNameFull = getQueryFromProperties(queryName, specificQueries);

            if (specificQueryNameFull != null) {
                // the query is redefined in the specific database file => use it
                queryNameFull = specificQueryNameFull;
                querySql = specificQueries.getProperty(queryNameFull);
            }

            queries.put(queryNameFull, querySql);
        }

        return queries;
    }

    /**
     * Configures a named query to Hibernate.
     *
     * @param queryName the query name
     * @param querySql the query SQL
     * @param entityManager the entity manager
     */
    public static void configureNamedQuery(String queryName, String querySql, EntityManager entityManager) {
        boolean isNative = queryName.endsWith(QUERY_NATIVE_SUFFIX);
        queryName = getQueryShortName(queryName);

        logger.tracef("adding query from properties files native=%b %s:%s", isNative, queryName, querySql);

        SessionFactoryImplementor sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactoryImplementor.class);

        if (isNative) {
            sessionFactory.addNamedQuery(queryName, entityManager.createNativeQuery(querySql));
        } else {
            sessionFactory.addNamedQuery(queryName, entityManager.createQuery(querySql));
        }
    }

    public static String getDatabaseType(String productName) {
        switch (productName) {
            case "Microsoft SQL Server":
            case "SQLOLEDB":
                return "mssql";
            case "EnterpriseDB":
                return "postgresql";
            default:
                return productName.toLowerCase();
        }
    }

    /**
     * Helper to close the entity manager.
     * @param em The entity manager to close
     */
    public static void closeEntityManager(EntityManager em) {
        if (em != null) {
            try {
                em.close();
            } catch (Exception e) {
                logger.warn("Failed to close entity manager", e);
            }
        }
    }
}
