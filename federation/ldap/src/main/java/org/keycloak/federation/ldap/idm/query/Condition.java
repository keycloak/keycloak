package org.keycloak.federation.ldap.idm.query;

/**
 * <p>A {@link Condition} is used to specify how a specific {@link QueryParameter}
 * is defined in order to filter query results.</p>
 *
 * @author Pedro Igor
 */
public interface Condition {

    String getParameterName();
    void setParameterName(String parameterName);

    void applyCondition(StringBuilder filter);

}