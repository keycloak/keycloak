package org.keycloak.federation.ldap.mappers;

import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.internal.LDAPIdentityQuery;
import org.keycloak.models.UserFederationMapper;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface LDAPFederationMapper extends UserFederationMapper {

    // TODO: rename?
    // Called when importing user from federation provider to local keycloak DB. Flag "isCreate" means if we creating new user to Keycloak DB or just update existing user in Keycloak DB
    void importUserFromLDAP(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapObject, UserModel user, boolean isCreate);

    // TODO: rename to beforeRegister or something?
    // Called when register new user to federation provider
    void registerUserToLDAP(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapObject, UserModel localUser);

    // Called when invoke proxy on federation provider
    UserModel proxy(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapObject, UserModel delegate);

    // Called before any LDAPIdentityQuery is executed
    void beforeLDAPQuery(UserFederationMapperModel mapperModel, LDAPIdentityQuery query);
}
