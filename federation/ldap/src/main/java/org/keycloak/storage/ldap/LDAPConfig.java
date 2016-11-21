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

package org.keycloak.storage.ldap;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.LDAPConstants;
import org.keycloak.storage.UserStorageProvider;

import javax.naming.directory.SearchControls;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 *
 */
public class LDAPConfig {

    private final MultivaluedHashMap<String, String> config;

    public LDAPConfig(MultivaluedHashMap<String, String> config) {
        this.config = config;
    }

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
        for (int i=0 ; i<objectClasses.length ; i++) {
            userObjClasses.add(objectClasses[i].trim());
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

    public String getConnectionPooling() {
        return config.getFirst(LDAPConstants.CONNECTION_POOLING);
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

    public UserStorageProvider.EditMode getEditMode() {
        String editModeString = config.getFirst(LDAPConstants.EDIT_MODE);
        if (editModeString == null) {
            return UserStorageProvider.EditMode.READ_ONLY;
        } else {
            return UserStorageProvider.EditMode.valueOf(editModeString);
        }
    }
}
