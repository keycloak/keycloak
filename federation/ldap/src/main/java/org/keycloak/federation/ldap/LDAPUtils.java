package org.keycloak.federation.ldap;

import java.util.List;
import java.util.Set;

import org.keycloak.federation.ldap.idm.model.LDAPDn;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.internal.LDAPIdentityQuery;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.federation.ldap.mappers.LDAPFederationMapper;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserModel;

/**
 * Allow to directly call some operations against LDAPIdentityStore.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPUtils {

    /**
     * @param ldapProvider
     * @param realm
     * @param user
     * @return newly created LDAPObject with all the attributes, uuid and DN properly set
     */
    public static LDAPObject addUserToLDAP(LDAPFederationProvider ldapProvider, RealmModel realm, UserModel user) {
        LDAPObject ldapUser = new LDAPObject();

        LDAPIdentityStore ldapStore = ldapProvider.getLdapIdentityStore();
        LDAPConfig ldapConfig = ldapStore.getConfig();
        ldapUser.setRdnAttributeName(ldapConfig.getRdnLdapAttribute());
        ldapUser.setObjectClasses(ldapConfig.getUserObjectClasses());

        Set<UserFederationMapperModel> federationMappers = realm.getUserFederationMappersByFederationProvider(ldapProvider.getModel().getId());
        for (UserFederationMapperModel mapperModel : federationMappers) {
            LDAPFederationMapper ldapMapper = ldapProvider.getMapper(mapperModel);
            ldapMapper.onRegisterUserToLDAP(mapperModel, ldapProvider, ldapUser, user, realm);
        }

        LDAPUtils.computeAndSetDn(ldapConfig, ldapUser);
        ldapStore.add(ldapUser);
        return ldapUser;
    }

    public static void removeAllUsers(LDAPFederationProvider ldapProvider, RealmModel realm) {
        LDAPIdentityStore ldapStore = ldapProvider.getLdapIdentityStore();
        LDAPIdentityQuery ldapQuery = LDAPUtils.createQueryForUserSearch(ldapProvider, realm);
        List<LDAPObject> allUsers = ldapQuery.getResultList();

        for (LDAPObject ldapUser : allUsers) {
            ldapStore.remove(ldapUser);
        }
    }

    public static LDAPIdentityQuery createQueryForUserSearch(LDAPFederationProvider ldapProvider, RealmModel realm) {
        LDAPIdentityQuery ldapQuery = new LDAPIdentityQuery(ldapProvider);
        LDAPConfig config = ldapProvider.getLdapIdentityStore().getConfig();
        ldapQuery.setSearchScope(config.getSearchScope());
        ldapQuery.setSearchDn(config.getUsersDn());
        ldapQuery.addObjectClasses(config.getUserObjectClasses());

        Set<UserFederationMapperModel> mapperModels = realm.getUserFederationMappersByFederationProvider(ldapProvider.getModel().getId());
        ldapQuery.addMappers(mapperModels);

        return ldapQuery;
    }

    // ldapUser has filled attributes, but doesn't have filled dn.
    private static void computeAndSetDn(LDAPConfig config, LDAPObject ldapUser) {
        String rdnLdapAttrName = config.getRdnLdapAttribute();
        String rdnLdapAttrValue = ldapUser.getAttributeAsString(rdnLdapAttrName);
        if (rdnLdapAttrValue == null) {
            throw new ModelException("RDN Attribute [" + rdnLdapAttrName + "] is not filled. Filled attributes: " + ldapUser.getAttributes());
        }

        LDAPDn dn = LDAPDn.fromString(config.getUsersDn());
        dn.addFirst(rdnLdapAttrName, rdnLdapAttrValue);
        ldapUser.setDn(dn);
    }

    public static String getUsername(LDAPObject ldapUser, LDAPConfig config) {
        String usernameAttr = config.getUsernameLdapAttribute();
        return ldapUser.getAttributeAsString(usernameAttr);
    }
}
