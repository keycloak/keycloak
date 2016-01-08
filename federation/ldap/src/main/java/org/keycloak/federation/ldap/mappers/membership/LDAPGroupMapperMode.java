package org.keycloak.federation.ldap.mappers.membership;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public enum LDAPGroupMapperMode {

    /**
     * All role mappings are retrieved from LDAP and saved into LDAP
     */
    LDAP_ONLY,

    /**
     * Read-only LDAP mode. Role mappings are retrieved from LDAP for particular user just at the time when he is imported and then
     * they are saved to local keycloak DB. Then all role mappings are always retrieved from keycloak DB, never from LDAP.
     * Creating or deleting of role mapping is propagated only to DB.
     *
     * This is read-only mode LDAP mode and it's good for performance, but when user is put to some role directly in LDAP, it
     * won't be seen by Keycloak
     */
    IMPORT,

    /**
     * Read-only LDAP mode. Role mappings are retrieved from both LDAP and DB and merged together. New role grants are not saved to LDAP but to DB.
     * Deleting role mappings, which is mapped to LDAP, will throw an error.
     */
    READ_ONLY

}
