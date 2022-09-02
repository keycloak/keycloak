/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.ldap.config;

import org.keycloak.Config;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.map.storage.ldap.role.config.LdapMapRoleMapperConfig;

import javax.naming.directory.SearchControls;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class LdapMapConfig {
    private final MultivaluedHashMap<String, String> config;

    public LdapMapConfig(Config.Scope config) {
        this.config = hm(config);
    }

    private static MultivaluedHashMap<String, String> hm(Config.Scope config) {
        return new MultivaluedHashMap<String, String>() {
            @Override
            public String getFirst(String key) {
                return config.get(key);
            }
        };
    }

    // from: RoleMapperConfig
    public Collection<String> getRoleObjectClasses() {
        String objectClasses = config.getFirst(LdapMapRoleMapperConfig.ROLE_OBJECT_CLASSES);
        if (objectClasses == null) {
            // For Active directory, the default is 'group' . For other servers 'groupOfNames'
            objectClasses = isActiveDirectory() ? LDAPConstants.GROUP : LDAPConstants.GROUP_OF_NAMES;
        }

        return getConfigValues(objectClasses);
    }

    // from: RoleMapperConfig
    protected Set<String> getConfigValues(String str) {
        String[] objClasses = str.split(",");
        Set<String> trimmed = new HashSet<>();
        for (String objectClass : objClasses) {
            objectClass = objectClass.trim();
            if (objectClass.length() > 0) {
                trimmed.add(objectClass);
            }
        }
        return trimmed;
    }

    private final Set<String> binaryAttributeNames = new HashSet<>();

    public String getConnectionUrl() {
        return config.getFirst(LDAPConstants.CONNECTION_URL);
    }

    public String getFactoryName() {
        // hardcoded for now
        return "com.sun.jndi.ldap.LdapCtxFactory";
    }

    public String getAuthType() {
        String value = config.getFirst(LDAPConstants.AUTH_TYPE);
        if (value == null) {
            return LDAPConstants.AUTH_TYPE_SIMPLE;
        } else {
            return value;
        }
    }

    public boolean useExtendedPasswordModifyOp() {
        String value = config.getFirst(LDAPConstants.USE_PASSWORD_MODIFY_EXTENDED_OP);
        return Boolean.parseBoolean(value);
    }

    public String getUseTruststoreSpi() {
        return config.getFirst(LDAPConstants.USE_TRUSTSTORE_SPI);
    }

    public String getUsersDn() {
        String usersDn = config.getFirst(LDAPConstants.USERS_DN);

        if (usersDn == null) {
            // Just for the backwards compatibility 1.2 -> 1.3 . Should be removed later.
            usersDn = config.getFirst("userDnSuffix");
        }

        return usersDn;
    }

    public Collection<String> getUserObjectClasses() {
        String objClassesCfg = config.getFirst(LDAPConstants.USER_OBJECT_CLASSES);
        String objClassesStr = (objClassesCfg != null && objClassesCfg.length() > 0) ? objClassesCfg.trim() : "inetOrgPerson,organizationalPerson";

        String[] objectClasses = objClassesStr.split(",");

        // Trim them
        Set<String> userObjClasses = new HashSet<>();
        for (String objectClass : objectClasses) {
            userObjClasses.add(objectClass.trim());
        }
        return userObjClasses;
    }

    public String getBindDN() {
        return config.getFirst(LDAPConstants.BIND_DN);
    }

    public String getBindCredential() {
        return config.getFirst(LDAPConstants.BIND_CREDENTIAL);
    }

    public String getVendor() {
        return config.getFirst(LDAPConstants.VENDOR);
    }

    public boolean isActiveDirectory() {
        String vendor = getVendor();
        return vendor != null && vendor.equals(LDAPConstants.VENDOR_ACTIVE_DIRECTORY);
    }

    public boolean isValidatePasswordPolicy() {
        String validatePPolicy = config.getFirst(LDAPConstants.VALIDATE_PASSWORD_POLICY);
        return Boolean.parseBoolean(validatePPolicy);
    }

    public boolean isTrustEmail(){
        String trustEmail = config.getFirst(LDAPConstants.TRUST_EMAIL);
        return Boolean.parseBoolean(trustEmail);
    }

    public String getConnectionPooling() {
        if(isStartTls()) {
            return null;
        } else {
            return config.getFirst(LDAPConstants.CONNECTION_POOLING);
        }
    }

    public String getConnectionPoolingAuthentication() {
        return config.getFirst(LDAPConstants.CONNECTION_POOLING_AUTHENTICATION);
    }

    public String getConnectionPoolingDebug() {
        return config.getFirst(LDAPConstants.CONNECTION_POOLING_DEBUG);
    }

    public String getConnectionPoolingInitSize() {
        return config.getFirst(LDAPConstants.CONNECTION_POOLING_INITSIZE);
    }

    public String getConnectionPoolingMaxSize() {
        return config.getFirst(LDAPConstants.CONNECTION_POOLING_MAXSIZE);
    }

    public String getConnectionPoolingPrefSize() {
        return config.getFirst(LDAPConstants.CONNECTION_POOLING_PREFSIZE);
    }

    public String getConnectionPoolingProtocol() {
        return config.getFirst(LDAPConstants.CONNECTION_POOLING_PROTOCOL);
    }

    public String getConnectionPoolingTimeout() {
        return config.getFirst(LDAPConstants.CONNECTION_POOLING_TIMEOUT);
    }

    public String getConnectionTimeout() {
        return config.getFirst(LDAPConstants.CONNECTION_TIMEOUT);
    }

    public String getReadTimeout() {
        return config.getFirst(LDAPConstants.READ_TIMEOUT);
    }

    public Properties getAdditionalConnectionProperties() {
        // not supported for now
        return null;
    }

    public int getSearchScope() {
        String searchScope = config.getFirst(LDAPConstants.SEARCH_SCOPE);
        return searchScope == null ? SearchControls.SUBTREE_SCOPE : Integer.parseInt(searchScope);
    }

    public String getUuidLDAPAttributeName() {
        String uuidAttrName = config.getFirst(LDAPConstants.UUID_LDAP_ATTRIBUTE);
        if (uuidAttrName == null) {
            // Differences of unique attribute among various vendors
            String vendor = getVendor();
            uuidAttrName = LDAPConstants.getUuidAttributeName(vendor);
        }

        return uuidAttrName;
    }

    public boolean isObjectGUID() {
        return getUuidLDAPAttributeName().equalsIgnoreCase(LDAPConstants.OBJECT_GUID);
    }

    public boolean isEdirectoryGUID() {
        return isEdirectory() && getUuidLDAPAttributeName().equalsIgnoreCase(LDAPConstants.NOVELL_EDIRECTORY_GUID);
    }

    public boolean isPagination() {
        String pagination = config.getFirst(LDAPConstants.PAGINATION);
        return Boolean.parseBoolean(pagination);
    }

    public int getBatchSizeForSync() {
        String pageSizeConfig = config.getFirst(LDAPConstants.BATCH_SIZE_FOR_SYNC);
        return pageSizeConfig!=null ? Integer.parseInt(pageSizeConfig) : LDAPConstants.DEFAULT_BATCH_SIZE_FOR_SYNC;
    }

    public String getUsernameLdapAttribute() {
        String username = config.getFirst(LDAPConstants.USERNAME_LDAP_ATTRIBUTE);
        if (username == null) {
            username = isActiveDirectory() ? LDAPConstants.CN : LDAPConstants.UID;
        }
        return username;
    }

    public String getRdnLdapAttribute() {
        String rdn = config.getFirst(LDAPConstants.RDN_LDAP_ATTRIBUTE);
        if (rdn == null) {
            rdn = getUsernameLdapAttribute();

            if (rdn.equalsIgnoreCase(LDAPConstants.SAM_ACCOUNT_NAME)) {
                // Just for the backwards compatibility 1.2 -> 1.3 . Should be removed later.
                rdn = LDAPConstants.CN;
            }

        }
        return rdn;
    }


    public String getCustomUserSearchFilter() {
        String customFilter = config.getFirst(LDAPConstants.CUSTOM_USER_SEARCH_FILTER);
        if (customFilter != null) {
            customFilter = customFilter.trim();
            if (customFilter.length() > 0) {
                return customFilter;
            }
        }
        return null;
    }

    public boolean isStartTls() {
        return Boolean.parseBoolean(config.getFirst(LDAPConstants.START_TLS));
    }

    public void addBinaryAttribute(String attrName) {
        binaryAttributeNames.add(attrName);
    }

    public Set<String> getBinaryAttributeNames() {
        return binaryAttributeNames;
    }


    public boolean isEdirectory() {
        return LDAPConstants.VENDOR_NOVELL_EDIRECTORY.equalsIgnoreCase(getVendor());
    }

    @Override
    public String toString() {
        MultivaluedHashMap<String, String> copy = new MultivaluedHashMap<>(config);
        copy.remove(LDAPConstants.BIND_CREDENTIAL);
        return copy + ", binaryAttributes: " + binaryAttributeNames;
    }
}
