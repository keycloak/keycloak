package org.keycloak.federation.ldap.mappers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPUtils;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.QueryParameter;
import org.keycloak.federation.ldap.idm.query.internal.EqualCondition;
import org.keycloak.federation.ldap.idm.query.internal.LDAPIdentityQuery;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Mapper useful for the LDAP deployments when some attribute (usually CN) is mapped to full name of user
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FullNameLDAPFederationMapper extends AbstractLDAPFederationMapper {

    public static final String LDAP_FULL_NAME_ATTRIBUTE = "ldap.full.name.attribute";
    public static final String READ_ONLY = "read.only";

    @Override
    public String getHelpText() {
        return "Some help text - full name mapper - TODO";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }

    @Override
    public String getId() {
        return "full-name-ldap-mapper";
    }

    @Override
    public void importUserFromLDAP(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapObject, UserModel user, boolean isCreate) {
        String ldapFullNameAttrName = getLdapFullNameAttrName(mapperModel);
        String fullName = (String) ldapObject.getAttribute(ldapFullNameAttrName);
        fullName = fullName.trim();
        if (fullName != null) {
            int lastSpaceIndex = fullName.lastIndexOf(" ");
            if (lastSpaceIndex == -1) {
                user.setLastName(fullName);
            } else {
                user.setFirstName(fullName.substring(0, lastSpaceIndex));
                user.setLastName(fullName.substring(lastSpaceIndex + 1));
            }
        }
    }

    @Override
    public void registerUserToLDAP(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapObject, UserModel localUser) {
        String ldapFullNameAttrName = getLdapFullNameAttrName(mapperModel);
        String fullName = getFullName(localUser.getFirstName(), localUser.getLastName());
        ldapObject.setAttribute(ldapFullNameAttrName, fullName);

        if (isReadOnly(mapperModel)) {
            ldapObject.addReadOnlyAttributeName(ldapFullNameAttrName);
        }
    }

    @Override
    public UserModel proxy(final UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapObject, UserModel delegate) {
        if (ldapProvider.getEditMode() == UserFederationProvider.EditMode.WRITABLE && !isReadOnly(mapperModel)) {


            AbstractTxAwareLDAPUserModelDelegate txDelegate = new AbstractTxAwareLDAPUserModelDelegate(delegate, ldapProvider, ldapObject) {

                @Override
                public void setFirstName(String firstName) {
                    super.setFirstName(firstName);
                    setFullNameToLDAPObject();
                }

                @Override
                public void setLastName(String lastName) {
                    super.setLastName(lastName);
                    setFullNameToLDAPObject();
                }

                private void setFullNameToLDAPObject() {
                    String fullName = getFullName(getFirstName(), getLastName());
                    if (logger.isTraceEnabled()) {
                        logger.tracef("Pushing full name attribute to LDAP. Full name: %s", fullName);
                    }

                    ensureTransactionStarted();

                    String ldapFullNameAttrName = getLdapFullNameAttrName(mapperModel);
                    ldapObject.setAttribute(ldapFullNameAttrName, fullName);
                }

            };

            return txDelegate;
        } else {
            return delegate;
        }
    }

    @Override
    public void beforeLDAPQuery(UserFederationMapperModel mapperModel, LDAPIdentityQuery query) {
        String ldapFullNameAttrName = getLdapFullNameAttrName(mapperModel);
        query.addReturningLdapAttribute(ldapFullNameAttrName);

        // Change conditions and compute condition for fullName from the conditions for firstName and lastName. Right now just "equal" condition is supported
        EqualCondition firstNameCondition = null;
        EqualCondition lastNameCondition = null;
        Set<Condition> conditionsCopy = new HashSet<Condition>(query.getConditions());
        for (Condition condition : conditionsCopy) {
            QueryParameter param = condition.getParameter();
            if (param != null) {
                if (param.getName().equals(UserModel.FIRST_NAME)) {
                    firstNameCondition = (EqualCondition) condition;
                    query.getConditions().remove(condition);
                } else if (param.getName().equals(UserModel.LAST_NAME)) {
                    lastNameCondition = (EqualCondition) condition;
                    query.getConditions().remove(condition);
                } else if (param.getName().equals(LDAPConstants.GIVENNAME)) {
                    // Some previous mapper already converted it
                    firstNameCondition = (EqualCondition) condition;
                } else if (param.getName().equals(LDAPConstants.SN)) {
                    // Some previous mapper already converted it
                    lastNameCondition = (EqualCondition) condition;
                }
            }
        }


        String fullName = null;
        if (firstNameCondition != null && lastNameCondition != null) {
            fullName = firstNameCondition.getValue() + " " + lastNameCondition.getValue();
        } else if (firstNameCondition != null) {
            fullName = (String) firstNameCondition.getValue();
        } else if (firstNameCondition != null) {
            fullName = (String) lastNameCondition.getValue();
        } else {
            return;
        }
        EqualCondition fullNameCondition = new EqualCondition(new QueryParameter(ldapFullNameAttrName), fullName);
        query.getConditions().add(fullNameCondition);
    }

    protected String getLdapFullNameAttrName(UserFederationMapperModel mapperModel) {
        String ldapFullNameAttrName = mapperModel.getConfig().get(LDAP_FULL_NAME_ATTRIBUTE);
        return ldapFullNameAttrName == null ? LDAPConstants.CN : ldapFullNameAttrName;
    }

    protected String getFullName(String firstName, String lastName) {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return LDAPConstants.EMPTY_ATTRIBUTE_VALUE;
        }
    }

    private boolean isReadOnly(UserFederationMapperModel mapperModel) {
        return LDAPUtils.parseBooleanParameter(mapperModel, READ_ONLY);
    }
}
