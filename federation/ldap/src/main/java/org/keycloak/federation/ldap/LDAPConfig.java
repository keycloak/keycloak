package org.keycloak.federation.ldap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.directory.SearchControls;

import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserFederationProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 *
 */
public class LDAPConfig {

    private final Map<String, String> config;

    public LDAPConfig(Map<String, String> config) {
        this.config = config;
    }

    public String getConnectionUrl() {
        return config.get(LDAPConstants.CONNECTION_URL);
    }

    public String getFactoryName() {
        // hardcoded for now
        return "com.sun.jndi.ldap.LdapCtxFactory";
    }

    public String getAuthType() {
        String value = config.get(LDAPConstants.AUTH_TYPE);
        if (value == null) {
            return LDAPConstants.AUTH_TYPE_SIMPLE;
        } else {
            return value;
        }
    }

    public String getSecurityProtocol() {
        // hardcoded for now
        return config.get(LDAPConstants.SECURITY_PROTOCOL);
    }

    public String getUsersDn() {
        String usersDn = config.get(LDAPConstants.USERS_DN);

        if (usersDn == null) {
            // Just for the backwards compatibility 1.2 -> 1.3 . Should be removed later.
            usersDn = config.get("userDnSuffix");
        }

        return usersDn;
    }

    public Collection<String> getUserObjectClasses() {
        String objClassesCfg = config.get(LDAPConstants.USER_OBJECT_CLASSES);
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
        return config.get(LDAPConstants.BIND_DN);
    }

    public String getBindCredential() {
        return config.get(LDAPConstants.BIND_CREDENTIAL);
    }

    public String getVendor() {
        return config.get(LDAPConstants.VENDOR);
    }

    public boolean isActiveDirectory() {
        String vendor = getVendor();
        return vendor != null && vendor.equals(LDAPConstants.VENDOR_ACTIVE_DIRECTORY);
    }

    public String getConnectionPooling() {
        return config.get(LDAPConstants.CONNECTION_POOLING);
    }

    public Properties getAdditionalConnectionProperties() {
        // not supported for now
        return null;
    }

    public int getSearchScope() {
        String searchScope = config.get(LDAPConstants.SEARCH_SCOPE);
        return searchScope == null ? SearchControls.SUBTREE_SCOPE : Integer.parseInt(searchScope);
    }

    public String getUuidLDAPAttributeName() {
        String uuidAttrName = config.get(LDAPConstants.UUID_LDAP_ATTRIBUTE);
        if (uuidAttrName == null) {
            // Differences of unique attribute among various vendors
            String vendor = getVendor();
            uuidAttrName = LDAPConstants.getUuidAttributeName(vendor);
        }

        return uuidAttrName;
    }

    public boolean isPagination() {
        String pagination = config.get(LDAPConstants.PAGINATION);
        return pagination==null ? false : Boolean.parseBoolean(pagination);
    }

    public String getUsernameLdapAttribute() {
        String username = config.get(LDAPConstants.USERNAME_LDAP_ATTRIBUTE);
        if (username == null) {
            username = isActiveDirectory() ? LDAPConstants.CN : LDAPConstants.UID;
        }
        return username;
    }

    public String getRdnLdapAttribute() {
        String rdn = config.get(LDAPConstants.RDN_LDAP_ATTRIBUTE);
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
        String customFilter = config.get(LDAPConstants.CUSTOM_USER_SEARCH_FILTER);
        if (customFilter != null) {
            customFilter = customFilter.trim();
            if (customFilter.length() > 0) {
                return customFilter;
            }
        }
        return null;
    }

    public UserFederationProvider.EditMode getEditMode() {
        String editModeString = config.get(LDAPConstants.EDIT_MODE);
        if (editModeString == null) {
            return UserFederationProvider.EditMode.READ_ONLY;
        } else {
            return UserFederationProvider.EditMode.valueOf(editModeString);
        }
    }
}
