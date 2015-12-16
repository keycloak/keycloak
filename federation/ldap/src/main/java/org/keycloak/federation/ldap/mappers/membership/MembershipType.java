package org.keycloak.federation.ldap.mappers.membership;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public enum MembershipType {

    /**
     * Used if LDAP role has it's members declared in form of their full DN. For example ( "member: uid=john,ou=users,dc=example,dc=com" )
     */
    DN,

    /**
     * Used if LDAP role has it's members declared in form of pure user uids. For example ( "memberUid: john" )
     */
    UID
}
