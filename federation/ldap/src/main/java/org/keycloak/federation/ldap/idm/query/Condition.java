package org.keycloak.federation.ldap.idm.query;

/**
 * <p>A {@link Condition} is used to specify how a specific query parameter
 * is defined in order to filter query results.</p>
 *
 * @author Pedro Igor
 */
public interface Condition {

    String getParameterName();
    void setParameterName(String parameterName);

    /**
     * Will change the parameter name if it is "modelParamName" to "ldapParamName" . Implementation can apply this to subconditions as well.
     *
     * It is used to update LDAP queries, which were created with model parameter name ( for example "firstName" ) and rewrite them to use real
     * LDAP mapped attribute (for example "givenName" )
     */
    void updateParameterName(String modelParamName, String ldapParamName);


    void applyCondition(StringBuilder filter);

}