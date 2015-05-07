package org.keycloak.federation.ldap.idm.store;

import java.util.List;

import org.keycloak.federation.ldap.idm.model.AttributedType;
import org.keycloak.federation.ldap.idm.model.IdentityType;
import org.keycloak.federation.ldap.idm.model.LDAPUser;
import org.keycloak.federation.ldap.idm.query.internal.IdentityQuery;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStoreConfiguration;

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
    LDAPIdentityStoreConfiguration getConfig();

    // General

    /**
     * Persists the specified IdentityType
     *
     * @param value
     */
    void add(AttributedType value);

    /**
     * Updates the specified IdentityType
     *
     * @param value
     */
    void update(AttributedType value);

    /**
     * Removes the specified IdentityType
     *
     * @param value
     */
    void remove(AttributedType value);

    // Identity query

    <V extends IdentityType> List<V> fetchQueryResults(IdentityQuery<V> identityQuery);

    <V extends IdentityType> int countQueryResults(IdentityQuery<V> identityQuery);

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
    boolean validatePassword(LDAPUser user, String password);

    /**
     * Updates the specified credential value.
     *
     * @param user Keycloak user
     * @param password Ldap password
     */
    void updatePassword(LDAPUser user, String password);

}
