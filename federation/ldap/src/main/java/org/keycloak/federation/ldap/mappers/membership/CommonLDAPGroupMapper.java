package org.keycloak.federation.ldap.mappers.membership;

import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;

/**
 * Mapper related to mapping of LDAP groups to keycloak model objects (either keycloak roles or keycloak groups)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface CommonLDAPGroupMapper {

    LDAPQuery createLDAPGroupQuery();

    CommonLDAPGroupMapperConfig getConfig();
}
