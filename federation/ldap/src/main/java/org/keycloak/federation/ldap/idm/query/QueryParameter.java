package org.keycloak.federation.ldap.idm.query;

/**
 * A marker interface indicating that the implementing class can be used as a
 * parameter within an IdentityQuery or RelationshipQuery
 *
 * @author Shane Bryzak
 *
 */
public class QueryParameter {

    private String name;

    public QueryParameter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
