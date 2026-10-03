/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage.ldap.mappers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.models.utils.reflection.Property;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.userprofile.UserProfileUtil;

import org.jboss.logging.Logger;


public class HardcodedAttributeMapper extends AbstractLDAPStorageMapper {

    private static final Logger logger = Logger.getLogger(HardcodedAttributeMapper.class);

    public HardcodedAttributeMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider) {
        super(mapperModel, ldapProvider);
    }

    private static final Map<String, Property<Object>> userModelProperties = LDAPUtils.getUserModelProperties();

    public static final String USER_MODEL_ATTRIBUTE = "user.model.attribute";
    public static final String ATTRIBUTE_VALUE = "attribute.value";

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {
        String userModelAttrName = getUserModelAttribute();

        String attributeValue = getAttributeValue();
        Property<Object> userModelProperty = userModelProperties.get(userModelAttrName.toLowerCase());

        if (userModelProperty != null) {
            setPropertyOnUserModel(userModelProperty, user, attributeValue);
        } else {
            user.setAttribute(userModelAttrName, Arrays.asList(attributeValue));
        }
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser, RealmModel realm) {

    }

    @Override
    public Set<String> getUserProfileAttributes() {
        // Not returned from getUserAttributes(): this attribute has no backing LDAP attribute - its value is
        // never read from LDAP, only hardcoded on import - so it must never be searchable via searchLDAPByAttributes,
        // which would otherwise build a filter against a non-existent (or unrelated) LDAP attribute.
        String userModelAttrName = getUserModelAttribute();
        if (userModelAttrName == null) {
            return Collections.emptySet();
        }

        // This mapper can also target a UserModel bean property (e.g. "enabled", "emailVerified" - see
        // setPropertyOnUserModel()) rather than a custom attribute, and that lookup is case-insensitive - a
        // mapper configured as e.g. "FirstName" still updates the real firstName property on import. Canonicalize
        // to the property's real name before deciding what to expose, or such a differently-cased root attribute
        // would be exposed under its configured spelling as a separate attribute from the real one
        // decorateUserProfile() already knows, defeating the read-only bypass override on the real attribute.
        //
        // Only expose it here if it is either a genuine custom attribute (not a model property at all) or one of
        // the few model properties User Profile itself manages as a root attribute (username, email, firstName,
        // lastName, locale). Any other model property has no corresponding User Profile attribute, so exposing it
        // here would make decorateUserProfile() fabricate a bogus, disconnected attribute (e.g. an "enabled" field
        // whose edits write a plain custom attribute instead of actually enabling/disabling the user) instead of
        // leaving it alone.
        Property<Object> userModelProperty = userModelProperties.get(userModelAttrName.toLowerCase());
        if (userModelProperty == null) {
            return Set.of(userModelAttrName);
        }

        String propertyName = userModelProperty.getName();
        return UserProfileUtil.isRootAttribute(propertyName) ? Set.of(propertyName) : Collections.emptySet();
    }

    @Override
    public boolean isUserAttributeReadOnly(String attrName) {
        // onImportUserFromLDAP() unconditionally overwrites this attribute with the configured hardcoded value on
        // every import, discarding any edit made through the User Profile in the meantime - the same
        // never-actually-persisted behavior UserAttributeLDAPStorageMapper's read-only mappers have.
        return true;
    }

    @Override
    public UserModel proxy(final LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        String userModelAttrName = getUserModelAttribute();
        String attributeValue = getAttributeValue();

        delegate = new UserModelDelegate(delegate) {

            @Override
            public Stream<String> getAttributeStream(String name) {
                if(userModelAttrName.equals(name)){
                    return Stream.of(attributeValue);
                }
                return super.getAttributeStream(name);
            }

            @Override
            public boolean isEmailVerified() {
                if(userModelAttrName.equals("emailVerified")){
                    return Boolean.valueOf(attributeValue);
                }
                return super.isEmailVerified();
            }

            @Override
            public boolean isEnabled() {
                if(userModelAttrName.equals("enabled")){
                    return Boolean.valueOf(attributeValue);
                }
                return super.isEnabled();
            }
           
       };
       return delegate;
   }

   private String getUserModelAttribute() {
       return mapperModel.getConfig().getFirst(USER_MODEL_ATTRIBUTE);
   }

   String getAttributeValue() {
      return mapperModel.getConfig().getFirst(ATTRIBUTE_VALUE);
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

    @Override
    public void beforeLDAPQuery(LDAPQuery query) {
       
    }

}