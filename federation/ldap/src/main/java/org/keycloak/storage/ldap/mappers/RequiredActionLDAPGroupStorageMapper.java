package org.keycloak.storage.ldap.mappers;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQueryConditionsBuilder;

import java.util.List;
import java.util.Objects;

public class RequiredActionLDAPGroupStorageMapper extends AbstractLDAPStorageMapper {

    public static final String USER_ATTR_FOR_REMEMBERING_REQUIRED_ACTIONS_PREFIX = "required-actions-set-from-ldap-";
    private static final Logger logger = Logger.getLogger(RequiredActionLDAPGroupStorageMapper.class);


    public RequiredActionLDAPGroupStorageMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider) {
        super(mapperModel, ldapProvider);
    }

    /**
     * When a user is imported from LDAP and is in the specified LDAP group, then the chosen required actions will be added to the user.
     * This will only happen if the user did not already have or had the required actions assigned through this mapper.
     */
    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {
        String mapperUserAttr = USER_ATTR_FOR_REMEMBERING_REQUIRED_ACTIONS_PREFIX + mapperModel.getName();
        String alreadySetRequiredActionFromLdap = user.getFirstAttribute(mapperUserAttr);
        List<String> requiredActionList = mapperModel.getConfig().getList(RequiredActionLDAPGroupStorageMapperFactory.REQUIRED_ACTION);
        String requiredActionListString = String.join(",", requiredActionList);

        String groupName = mapperModel.getConfig().getFirst(RequiredActionLDAPGroupStorageMapperFactory.GROUP);
        String groupsDn = mapperModel.getConfig().getFirst(RequiredActionLDAPGroupStorageMapperFactory.GROUPS_DN);
        String membershipAttrName = mapperModel.getConfig().getFirst(RequiredActionLDAPGroupStorageMapperFactory.MEMBERSHIP_ATTR_NAME);


        logger.debugf("Checking LDAP group membership: groupName=%s, groupsDn=%s", groupName, groupsDn);

        try (LDAPQuery query = new LDAPQuery(ldapProvider)) {
            query.setSearchScope(ldapProvider.getLdapIdentityStore().getConfig().getSearchScope());
            query.setSearchDn(groupsDn);

            Condition groupNameCondition = new LDAPQueryConditionsBuilder().equal("cn", groupName);
            query.addWhereCondition(groupNameCondition);
            query.addReturningLdapAttribute(membershipAttrName);

            LDAPObject ldapGroup = query.getFirstResult();

            if (ldapGroup == null) {
                logger.warnf("LDAP group [%s] not found under DN [%s]", groupName, groupsDn);
                return;
            }

            logger.debugf("Found LDAP group [%s] at DN [%s]", groupName, ldapGroup.getDn());

            String userDn = ldapUser.getDn().toString();
            boolean isMember = ldapGroup.getAttributeAsSet(membershipAttrName).contains(userDn);

            if (isMember && !Objects.equals(alreadySetRequiredActionFromLdap, requiredActionListString)) {
                requiredActionList.forEach(user::addRequiredAction);
                user.setAttribute(mapperUserAttr, List.of(requiredActionListString));
                logger.debugf("Added required actions [%s] to user [%s]", String.join(",", requiredActionList), user.getUsername());
            }
        }
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser, RealmModel realm) {
        // Nothing to do when user is registered to LDAP
    }

    @Override
    public UserModel proxy(LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        return delegate;
    }

    @Override
    public void beforeLDAPQuery(LDAPQuery query) {
        // Nothing to do before LDAP query
    }
}
