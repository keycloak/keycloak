/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.federation.ldap.mappers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.models.utils.reflection.Property;
import org.keycloak.models.utils.reflection.PropertyCriteria;
import org.keycloak.models.utils.reflection.PropertyQueries;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserAttributeLDAPFederationMapper extends AbstractLDAPFederationMapper {

    private static final Logger logger = Logger.getLogger(UserAttributeLDAPFederationMapper.class);

    private static final Map<String, Property<Object>> userModelProperties;

    static {
        Map<String, Property<Object>> userModelProps = PropertyQueries.createQuery(UserModel.class).addCriteria(new PropertyCriteria() {

            @Override
            public boolean methodMatches(Method m) {
                if ((m.getName().startsWith("get") || m.getName().startsWith("is")) && m.getParameterTypes().length > 0) {
                    return false;
                }

                return true;
            }

        }).getResultList();

        // Convert to be keyed by lower-cased attribute names
        userModelProperties = new HashMap<>();
        for (Map.Entry<String, Property<Object>> entry : userModelProps.entrySet()) {
            userModelProperties.put(entry.getKey().toLowerCase(), entry.getValue());
        }
    }

    public static final String USER_MODEL_ATTRIBUTE = "user.model.attribute";
    public static final String LDAP_ATTRIBUTE = "ldap.attribute";
    public static final String READ_ONLY = "read.only";
    public static final String ALWAYS_READ_VALUE_FROM_LDAP = "always.read.value.from.ldap";
    public static final String IS_MANDATORY_IN_LDAP = "is.mandatory.in.ldap";

    public UserAttributeLDAPFederationMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, RealmModel realm) {
        super(mapperModel, ldapProvider, realm);
    }

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, boolean isCreate) {
        String userModelAttrName = mapperModel.getConfig().get(USER_MODEL_ATTRIBUTE);
        String ldapAttrName = mapperModel.getConfig().get(LDAP_ATTRIBUTE);

        Property<Object> userModelProperty = userModelProperties.get(userModelAttrName.toLowerCase());

        if (userModelProperty != null) {

            // we have java property on UserModel
            String ldapAttrValue = ldapUser.getAttributeAsString(ldapAttrName);

            checkDuplicateEmail(userModelAttrName, ldapAttrValue, realm, ldapProvider.getSession(), user);

            setPropertyOnUserModel(userModelProperty, user, ldapAttrValue);
        } else {

            // we don't have java property. Let's set attribute
            Set<String> ldapAttrValue = ldapUser.getAttributeAsSet(ldapAttrName);
            if (ldapAttrValue != null) {
                user.setAttribute(userModelAttrName, new ArrayList<>(ldapAttrValue));
            } else {
                user.removeAttribute(userModelAttrName);
            }
        }
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser) {
        String userModelAttrName = mapperModel.getConfig().get(USER_MODEL_ATTRIBUTE);
        String ldapAttrName = mapperModel.getConfig().get(LDAP_ATTRIBUTE);
        boolean isMandatoryInLdap = parseBooleanParameter(mapperModel, IS_MANDATORY_IN_LDAP);

        Property<Object> userModelProperty = userModelProperties.get(userModelAttrName.toLowerCase());

        if (userModelProperty != null) {

            // we have java property on UserModel. Assuming we support just properties of simple types
            Object attrValue = userModelProperty.getValue(localUser);

            if (attrValue == null) {
                if (isMandatoryInLdap) {
                    ldapUser.setSingleAttribute(ldapAttrName, LDAPConstants.EMPTY_ATTRIBUTE_VALUE);
                } else {
                    ldapUser.setAttribute(ldapAttrName, new LinkedHashSet<String>());
                }
            } else {
                ldapUser.setSingleAttribute(ldapAttrName, attrValue.toString());
            }
        } else {

            // we don't have java property. Let's set attribute
            List<String> attrValues = localUser.getAttribute(userModelAttrName);

            if (attrValues.size() == 0) {
                if (isMandatoryInLdap) {
                    ldapUser.setSingleAttribute(ldapAttrName, LDAPConstants.EMPTY_ATTRIBUTE_VALUE);
                } else {
                    ldapUser.setAttribute(ldapAttrName, new LinkedHashSet<String>());
                }
            } else {
                ldapUser.setAttribute(ldapAttrName, new LinkedHashSet<>(attrValues));
            }
        }

        if (isReadOnly()) {
            ldapUser.addReadOnlyAttributeName(ldapAttrName);
        }
    }

    // throw ModelDuplicateException if there is different user in model with same email
    protected void checkDuplicateEmail(String userModelAttrName, String email, RealmModel realm, KeycloakSession session, UserModel user) {
        if (UserModel.EMAIL.equalsIgnoreCase(userModelAttrName)) {
            // lowercase before search
            email = KeycloakModelUtils.toLowerCaseSafe(email);

            UserModel that = session.userStorage().getUserByEmail(email, realm);
            if (that != null && !that.getId().equals(user.getId())) {
                session.getTransactionManager().setRollbackOnly();
                String exceptionMessage = String.format("Can't import user '%s' from LDAP because email '%s' already exists in Keycloak. Existing user with this email is '%s'", user.getUsername(), email, that.getUsername());
                throw new ModelDuplicateException(exceptionMessage, UserModel.EMAIL);
            }
        }
    }

    @Override
    public UserModel proxy(final LDAPObject ldapUser, UserModel delegate) {
        final String userModelAttrName = mapperModel.getConfig().get(USER_MODEL_ATTRIBUTE);
        final String ldapAttrName = mapperModel.getConfig().get(LDAP_ATTRIBUTE);
        boolean isAlwaysReadValueFromLDAP = parseBooleanParameter(mapperModel, ALWAYS_READ_VALUE_FROM_LDAP);
        final boolean isMandatoryInLdap = parseBooleanParameter(mapperModel, IS_MANDATORY_IN_LDAP);

        // For writable mode, we want to propagate writing of attribute to LDAP as well
        if (ldapProvider.getEditMode() == UserFederationProvider.EditMode.WRITABLE && !isReadOnly()) {

            delegate = new TxAwareLDAPUserModelDelegate(delegate, ldapProvider, ldapUser) {

                @Override
                public void setSingleAttribute(String name, String value) {
                    setLDAPAttribute(name, value);
                    super.setSingleAttribute(name, value);
                }

                @Override
                public void setAttribute(String name, List<String> values) {
                    setLDAPAttribute(name, values);
                    super.setAttribute(name, values);
                }

                @Override
                public void removeAttribute(String name) {
                    setLDAPAttribute(name, null);
                    super.removeAttribute(name);
                }

                @Override
                public void setEmail(String email) {
                    checkDuplicateEmail(userModelAttrName, email, realm, ldapProvider.getSession(), this);

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

                protected void setLDAPAttribute(String modelAttrName, Object value) {
                    if (modelAttrName.equalsIgnoreCase(userModelAttrName)) {
                        if (logger.isTraceEnabled()) {
                            logger.tracef("Pushing user attribute to LDAP. username: %s, Model attribute name: %s, LDAP attribute name: %s, Attribute value: %s", getUsername(), modelAttrName, ldapAttrName, value);
                        }

                        ensureTransactionStarted();

                        if (value == null) {
                            if (isMandatoryInLdap) {
                                ldapUser.setSingleAttribute(ldapAttrName, LDAPConstants.EMPTY_ATTRIBUTE_VALUE);
                            } else {
                                ldapUser.setAttribute(ldapAttrName, new LinkedHashSet<String>());
                            }
                        } else if (value instanceof String) {
                            ldapUser.setSingleAttribute(ldapAttrName, (String) value);
                        } else {
                            List<String> asList = (List<String>) value;
                            if (asList.isEmpty() && isMandatoryInLdap) {
                                ldapUser.setSingleAttribute(ldapAttrName, LDAPConstants.EMPTY_ATTRIBUTE_VALUE);
                            } else {
                                ldapUser.setAttribute(ldapAttrName, new LinkedHashSet<>(asList));
                            }
                        }
                    }
                }

            };

        }

        // We prefer to read attribute value from LDAP instead of from local Keycloak DB
        if (isAlwaysReadValueFromLDAP) {

            delegate = new UserModelDelegate(delegate) {

                @Override
                public String getFirstAttribute(String name) {
                    if (name.equalsIgnoreCase(userModelAttrName)) {
                        return ldapUser.getAttributeAsString(ldapAttrName);
                    } else {
                        return super.getFirstAttribute(name);
                    }
                }

                @Override
                public List<String> getAttribute(String name) {
                    if (name.equalsIgnoreCase(userModelAttrName)) {
                        Collection<String> ldapAttrValue = ldapUser.getAttributeAsSet(ldapAttrName);
                        if (ldapAttrValue == null) {
                            return Collections.emptyList();
                        } else {
                            return new ArrayList<>(ldapAttrValue);
                        }
                    } else {
                        return super.getAttribute(name);
                    }
                }

                @Override
                public Map<String, List<String>> getAttributes() {
                    Map<String, List<String>> attrs = new HashMap<>(super.getAttributes());

                    // Ignore UserModel properties
                    if (userModelProperties.get(userModelAttrName.toLowerCase()) != null) {
                        return attrs;
                    }

                    Set<String> allLdapAttrValues = ldapUser.getAttributeAsSet(ldapAttrName);
                    if (allLdapAttrValues != null) {
                        attrs.put(userModelAttrName, new ArrayList<>(allLdapAttrValues));
                    }
                    return attrs;
                }

                @Override
                public String getEmail() {
                    if (UserModel.EMAIL.equalsIgnoreCase(userModelAttrName)) {
                        return ldapUser.getAttributeAsString(ldapAttrName);
                    } else {
                        return super.getEmail();
                    }
                }

                @Override
                public String getLastName() {
                    if (UserModel.LAST_NAME.equalsIgnoreCase(userModelAttrName)) {
                        return ldapUser.getAttributeAsString(ldapAttrName);
                    } else {
                        return super.getLastName();
                    }
                }

                @Override
                public String getFirstName() {
                    if (UserModel.FIRST_NAME.equalsIgnoreCase(userModelAttrName)) {
                        return ldapUser.getAttributeAsString(ldapAttrName);
                    } else {
                        return super.getFirstName();
                    }
                }

            };
        }

        return delegate;
    }

    @Override
    public void beforeLDAPQuery(LDAPQuery query) {
        String userModelAttrName = mapperModel.getConfig().get(USER_MODEL_ATTRIBUTE);
        String ldapAttrName = mapperModel.getConfig().get(LDAP_ATTRIBUTE);

        // Add mapped attribute to returning ldap attributes
        query.addReturningLdapAttribute(ldapAttrName);
        if (isReadOnly()) {
            query.addReturningReadOnlyLdapAttribute(ldapAttrName);
        }

        // Change conditions and use ldapAttribute instead of userModel
        for (Condition condition : query.getConditions()) {
            condition.updateParameterName(userModelAttrName, ldapAttrName);
        }
    }

    private boolean isReadOnly() {
        return parseBooleanParameter(mapperModel, READ_ONLY);
    }


    protected void setPropertyOnUserModel(Property<Object> userModelProperty, UserModel user, String ldapAttrValue) {
        if (ldapAttrValue == null) {
            userModelProperty.setValue(user, null);
        } else {
            Class<Object> clazz = userModelProperty.getJavaClass();

            if (String.class.equals(clazz)) {
                userModelProperty.setValue(user, ldapAttrValue);
            } else if (Boolean.class.equals(clazz) || boolean.class.equals(clazz)) {
                Boolean boolVal = Boolean.valueOf(ldapAttrValue);
                userModelProperty.setValue(user, boolVal);
            } else {
                logger.warnf("Don't know how to set the property '%s' on user '%s' . Value of LDAP attribute is '%s' ", userModelProperty.getName(), user.getUsername(), ldapAttrValue.toString());
            }
        }
    }
}
