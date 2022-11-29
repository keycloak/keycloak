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

package org.keycloak.models.map.storage.jpa;

import org.hibernate.Session;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.jboss.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.keycloak.models.map.storage.jpa.JpaMapStorageProviderFactory.HIBERNATE_DEFAULT_SCHEMA;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JpaMapUtils {

    public static final String QUERY_NATIVE_SUFFIX = "[native]";
    public static final String QUERY_JPQL_SUFFIX = "[jpql]";
    private static final Logger logger = Logger.getLogger(JpaMapUtils.class);

    public static String getSchemaForNativeQuery(EntityManager em) {
        String schema = (String) em.getEntityManagerFactory().getProperties().get(HIBERNATE_DEFAULT_SCHEMA);
        return (schema == null) ? "" : schema + ".";
    }

    /**
     * Method that adds the different query variants for the database.
     * The method loads the queries specified in the files
     * <em>META-INF/jpa-map/queries-{dbType}.properties</em> and the default
     * <em>META-INF/jpa-map/queries-default.properties</em>. At least the default file
     * should exist inside the jar file. The default file contains all the
     * needed queries and the specific one can overload all or some of them for
     * that database type.
     * @param databaseType The database type as returned by <code>getDatabaseType</code>
     */
    public static Properties loadSpecificNamedQueries(String databaseType) {
        URL specificUrl = JpaMapUtils.class.getClassLoader().getResource("META-INF/jpa-map/queries-" + databaseType + ".properties");
        URL defaultUrl = JpaMapUtils.class.getClassLoader().getResource("META-INF/jpa-map/queries-default.properties");

        if (defaultUrl == null) {
            throw new IllegalStateException("META-INF/jpa-map/queries-default.properties was not found in the classpath");
        }

        Properties specificQueries = loadSqlProperties(specificUrl);
        Properties defaultQueries = loadSqlProperties(defaultUrl);
        Properties queries = new Properties();

        for (String queryNameFull : defaultQueries.stringPropertyNames()) {
            String querySql = defaultQueries.getProperty(queryNameFull);
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
            NativeSQLQuerySpecification spec = new NativeSQLQuerySpecification(querySql, new NativeSQLQueryReturn[0], Collections.emptySet());
            sessionFactory.getQueryPlanCache().getNativeSQLQueryPlan(spec);
            sessionFactory.addNamedQuery(queryName, entityManager.createNativeQuery(querySql));
        } else {
            sessionFactory.getQueryPlanCache().getHQLQueryPlan(querySql, false, Collections.emptyMap());
            sessionFactory.addNamedQuery(queryName, entityManager.createQuery(querySql));
        }
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


    public static void addSpecificNamedQueries(EntityManagerFactory emf) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            String dbProductName = em.unwrap(Session.class).doReturningWork(connection -> connection.getMetaData().getDatabaseProductName());
            String dbKind = getDatabaseType(dbProductName);
            String schemaForNativeQuery = getSchemaForNativeQuery(em);
            for (Map.Entry<Object, Object> query : loadSpecificNamedQueries(dbKind.toLowerCase()).entrySet()) {
                String queryName = query.getKey().toString();
                String querySql = query.getValue().toString();
                querySql = querySql.replaceAll(Pattern.quote("${schemaprefix}"), schemaForNativeQuery);
                configureNamedQuery(queryName, querySql, em);
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }


}
