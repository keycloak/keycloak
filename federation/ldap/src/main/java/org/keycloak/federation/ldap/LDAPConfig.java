package org.keycloak.federation.ldap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.directory.SearchControls;

import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserFederationProviderModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 *
 * TODO: init properties at startup instead of always compute them
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
        // hardcoded for now
        return "simple";
    }

    public String getSecurityProtocol() {
        // hardcoded for now
        return config.get(LDAPConstants.SECURITY_PROTOCOL);
    }

    public Collection<String> getUserDns() {
        String value = config.get(LDAPConstants.USER_DNS);
        if (value == null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(value.split(LDAPConstants.CONFIG_DIVIDER));
        }
    }

    public String getSingleUserDn() {
        Collection<String> dns = getUserDns();
        if (dns.size() == 0) {
            throw new IllegalStateException("No user DN configured. User DNS value is " + config.get(LDAPConstants.USER_DNS));
        }
        return dns.iterator().next();
    }

    public Collection<String> getObjectClasses() {
        String objClassesCfg = config.get(LDAPConstants.USER_OBJECT_CLASSES);
        String objClassesStr = (objClassesCfg != null && objClassesCfg.length() > 0) ? objClassesCfg.trim() : "inetOrgPerson,organizationalPerson";

        String[] objectClasses = objClassesStr.split(",");

        // Trim them
        Set<String> userObjClasses = new HashSet<String>();
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

    public String getUuidAttributeName() {
        String uuidAttrName = config.get(LDAPConstants.UUID_ATTRIBUTE_NAME);
        if (uuidAttrName == null) {
            // Differences of unique attribute among various vendors
            String vendor = getVendor();
            if (vendor != null) {
                switch (vendor) {
                    case LDAPConstants.VENDOR_RHDS:
                        uuidAttrName = "nsuniqueid";
                        break;
                    case LDAPConstants.VENDOR_TIVOLI:
                        uuidAttrName = "uniqueidentifier";
                        break;
                    case LDAPConstants.VENDOR_NOVELL_EDIRECTORY:
                        uuidAttrName = "guid";
                        break;
                    case LDAPConstants.VENDOR_ACTIVE_DIRECTORY:
                        uuidAttrName = LDAPConstants.OBJECT_GUID;
                }
            }

            if (uuidAttrName == null) {
                uuidAttrName = LDAPConstants.ENTRY_UUID;
            }
        }

        return uuidAttrName;
    }

    // TODO: Remove and use mapper instead
    public boolean isUserAccountControlsAfterPasswordUpdate() {
        String userAccountCtrls = config.get(LDAPConstants.USER_ACCOUNT_CONTROLS_AFTER_PASSWORD_UPDATE);
        return userAccountCtrls==null ? false : Boolean.parseBoolean(userAccountCtrls);
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
        }
        return rdn;
    }
}
