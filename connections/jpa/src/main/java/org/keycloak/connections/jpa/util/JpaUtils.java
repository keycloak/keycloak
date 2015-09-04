package org.keycloak.connections.jpa.util;

import javax.persistence.EntityManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JpaUtils {

    public static final String HIBERNATE_DEFAULT_SCHEMA = "hibernate.default_schema";

    public static String getTableNameForNativeQuery(String tableName, EntityManager em) {
        String schema = (String) em.getEntityManagerFactory().getProperties().get(HIBERNATE_DEFAULT_SCHEMA);
        return (schema==null) ? tableName : schema + "." + tableName;
    }
}
