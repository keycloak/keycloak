package org.keycloak.federation.ldap.idm.store;

import java.util.List;

import org.keycloak.federation.ldap.LDAPConfig;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;

/**
 * IdentityStore representation providing minimal SPI
 *
 * TODO: Rather remove this abstraction
 *
 * @author Boleslaw Dawidowicz
 * @author Shane Bryzak
 */
public interface IdentityStore {

    /**
     * Returns the configuration for this IdentityStore instance
     *
     * @return
     */
    LDAPConfig getConfig();

    // General

    /**
     * Persists the specified IdentityType
     *
     * @param ldapObject
     */
    void add(LDAPObject ldapObject);

    /**
     * Updates the specified IdentityType
     *
     * @param ldapObject
     */
    void update(LDAPObject ldapObject);

    /**
     * Removes the specified IdentityType
     *
     * @param ldapObject
     */
    void remove(LDAPObject ldapObject);

    // Identity query

    List<LDAPObject> fetchQueryResults(LDAPQuery LDAPQuery);

    int countQueryResults(LDAPQuery LDAPQuery);

//    // Relationship query
//
//    <V extends Relationship> List<V> fetchQueryResults(RelationshipQuery<V> query);
//
//    <V extends Relationship> int countQueryResults(RelationshipQuery<V> query);

    // Credentials

    /**
     * Validates the specified credentials.
     *
     * @param user Keycloak user
     * @param password Ldap password
     */
    boolean validatePassword(LDAPObject user, String password);

    /**
     * Updates the specified credential value.
     *
     * @param user Keycloak user
     * @param password Ldap password
     */
    void updatePassword(LDAPObject user, String password);

}
