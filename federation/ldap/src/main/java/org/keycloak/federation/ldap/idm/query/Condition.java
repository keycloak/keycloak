package org.keycloak.federation.ldap.idm.query;

/**
 * <p>A {@link Condition} is used to specify how a specific {@link QueryParameter}
 * is defined in order to filter query results.</p>
 *
 * @author Pedro Igor
 */
public interface Condition {

    /**
     * <p>The {@link QueryParameter} restricted by this condition.</p>
     *
     * @return
     */
    QueryParameter getParameter();

}