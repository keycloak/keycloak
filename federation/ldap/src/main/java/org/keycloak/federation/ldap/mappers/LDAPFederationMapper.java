package org.keycloak.federation.ldap.mappers;

import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.models.RealmModel;
import org.keycloak.mappers.UserFederationMapper;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface LDAPFederationMapper extends UserFederationMapper {


    /**
     * Called when importing user from LDAP to local keycloak DB.
     *
     * @param mapperModel
     * @param ldapProvider
     * @param ldapUser
     * @param user
     * @param realm
     * @param isCreate true if we importing new user from LDAP. False if user already exists in Keycloak, but we are upgrading (syncing) it from LDAP
     */
    void onImportUserFromLDAP(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate);


    /**
     * Called when register new user to LDAP - just after user was created in Keycloak DB
     *
     * @param mapperModel
     * @param ldapProvider
     * @param ldapUser
     * @param localUser
     * @param realm
     */
    void onRegisterUserToLDAP(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, UserModel localUser, RealmModel realm);


    /**
     * Called when invoke proxy on LDAP federation provider
     *
     * @param mapperModel
     * @param ldapProvider
     * @param ldapUser
     * @param delegate
     * @param realm
     * @return
     */
    UserModel proxy(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, UserModel delegate, RealmModel realm);


    /**
     * Called before LDAP Identity query for retrieve LDAP users was executed. It allows to change query somehow (add returning attributes from LDAP, change conditions etc)
     *
     * @param mapperModel
     * @param query
     */
    void beforeLDAPQuery(UserFederationMapperModel mapperModel, LDAPQuery query);
}
