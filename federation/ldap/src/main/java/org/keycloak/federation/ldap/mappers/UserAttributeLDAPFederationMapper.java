package org.keycloak.federation.ldap.mappers;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPUtils;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.QueryParameter;
import org.keycloak.federation.ldap.idm.query.internal.LDAPIdentityQuery;
import org.keycloak.mappers.UserFederationMapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.reflection.Property;
import org.keycloak.models.utils.reflection.PropertyCriteria;
import org.keycloak.models.utils.reflection.PropertyQueries;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserAttributeLDAPFederationMapper extends AbstractLDAPFederationMapper {

    private static final Map<String, Property<Object>> userModelProperties;

    static {
        userModelProperties = PropertyQueries.createQuery(UserModel.class).addCriteria(new PropertyCriteria() {

            @Override
            public boolean methodMatches(Method m) {
                if ((m.getName().startsWith("get") || m.getName().startsWith("is")) && m.getParameterTypes().length > 0) {
                    return false;
                }

                return true;
            }

        }).getResultList();
    }

    public static final String USER_MODEL_ATTRIBUTE = "user.model.attribute";
    public static final String LDAP_ATTRIBUTE = "ldap.attribute";
    public static final String READ_ONLY = "read.only";


    @Override
    public void onImportUserFromLDAP(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {
        String userModelAttrName = mapperModel.getConfig().get(USER_MODEL_ATTRIBUTE);
        String ldapAttrName = mapperModel.getConfig().get(LDAP_ATTRIBUTE);

        Object ldapAttrValue = ldapUser.getAttribute(ldapAttrName);
        if (ldapAttrValue != null && !ldapAttrValue.toString().trim().isEmpty()) {
            Property<Object> userModelProperty = userModelProperties.get(userModelAttrName);

            if (userModelProperty != null) {
                // we have java property on UserModel
                userModelProperty.setValue(user, ldapAttrValue);
            } else {
                // we don't have java property. Let's just setAttribute
                user.setAttribute(userModelAttrName, (String) ldapAttrValue);
            }
        }
    }

    @Override
    public void onRegisterUserToLDAP(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, UserModel localUser, RealmModel realm) {
        String userModelAttrName = mapperModel.getConfig().get(USER_MODEL_ATTRIBUTE);
        String ldapAttrName = mapperModel.getConfig().get(LDAP_ATTRIBUTE);

        Property<Object> userModelProperty = userModelProperties.get(userModelAttrName);

        Object attrValue;
        if (userModelProperty != null) {
            // we have java property on UserModel
            attrValue = userModelProperty.getValue(localUser);
        } else {
            // we don't have java property. Let's just setAttribute
            attrValue = localUser.getAttribute(userModelAttrName);
        }

        ldapUser.setAttribute(ldapAttrName, attrValue);
        if (isReadOnly(mapperModel)) {
            ldapUser.addReadOnlyAttributeName(ldapAttrName);
        }
    }

    @Override
    public UserModel proxy(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        if (ldapProvider.getEditMode() == UserFederationProvider.EditMode.WRITABLE && !isReadOnly(mapperModel)) {

            final String userModelAttrName = mapperModel.getConfig().get(USER_MODEL_ATTRIBUTE);
            final String ldapAttrName = mapperModel.getConfig().get(LDAP_ATTRIBUTE);

            TxAwareLDAPUserModelDelegate txDelegate = new TxAwareLDAPUserModelDelegate(delegate, ldapProvider, ldapUser) {

                @Override
                public void setAttribute(String name, String value) {
                    setLDAPAttribute(name, value);
                    super.setAttribute(name, value);
                }

                @Override
                public void setEmail(String email) {
                    setLDAPAttribute(UserModel.EMAIL, email);
                    super.setEmail(email);
                }

                @Override
                public void setLastName(String lastName) {
                    setLDAPAttribute(UserModel.LAST_NAME, lastName);
                    super.setLastName(lastName);
                }

                @Override
                public void setFirstName(String firstName) {
                    setLDAPAttribute(UserModel.FIRST_NAME, firstName);
                    super.setFirstName(firstName);
                }

                protected void setLDAPAttribute(String modelAttrName, String value) {
                    if (modelAttrName.equals(userModelAttrName)) {
                        if (logger.isTraceEnabled()) {
                            logger.tracef("Pushing user attribute to LDAP. Model attribute name: %s, LDAP attribute name: %s, Attribute value: %s", modelAttrName, ldapAttrName, value);
                        }

                        ensureTransactionStarted();

                        ldapUser.setAttribute(ldapAttrName, value);
                    }
                }

            };

            return txDelegate;
        } else {
            return delegate;
        }
    }

    @Override
    public void beforeLDAPQuery(UserFederationMapperModel mapperModel, LDAPIdentityQuery query) {
        String userModelAttrName = mapperModel.getConfig().get(USER_MODEL_ATTRIBUTE);
        String ldapAttrName = mapperModel.getConfig().get(LDAP_ATTRIBUTE);

        // Add mapped attribute to returning ldap attributes
        query.addReturningLdapAttribute(ldapAttrName);
        if (isReadOnly(mapperModel)) {
            query.addReturningReadOnlyLdapAttribute(ldapAttrName);
        }

        // Change conditions and use ldapAttribute instead of userModel
        for (Condition condition : query.getConditions()) {
            QueryParameter param = condition.getParameter();
            if (param != null && param.getName().equals(userModelAttrName)) {
                param.setName(ldapAttrName);
            }
        }
    }

    private boolean isReadOnly(UserFederationMapperModel mapperModel) {
        return parseBooleanParameter(mapperModel, READ_ONLY);
    }
}
