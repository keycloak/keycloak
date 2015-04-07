package org.keycloak.federation.ldap.idm.store.ldap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.keycloak.federation.ldap.idm.model.AttributedType;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;

/**
 * A configuration for the LDAP store.
 *
 * @author anil saldhana
 * @since Sep 6, 2012
 */

public class LDAPIdentityStoreConfiguration {

    private String ldapURL;
    private String factoryName = "com.sun.jndi.ldap.LdapCtxFactory";
    private String authType = "simple";
    private String protocol;
    private String bindDN;
    private String bindCredential;
    private boolean activeDirectory;
    private Properties connectionProperties;
    private boolean pagination;
    private String uniqueIdentifierAttributeName;
    private boolean userAccountControlsAfterPasswordUpdate;

    private String baseDN;
    private Map<Class<? extends AttributedType>, LDAPMappingConfiguration> mappingConfig = new HashMap<Class<? extends AttributedType>, LDAPMappingConfiguration>();

    public String getLdapURL() {
        return this.ldapURL;
    }

    public String getFactoryName() {
        return this.factoryName;
    }

    public String getAuthType() {
        return this.authType;
    }

    public String getBaseDN() {
        return this.baseDN;
    }

    public String getBindDN() {
        return this.bindDN;
    }

    public String getBindCredential() {
        return this.bindCredential;
    }

    public boolean isActiveDirectory() {
        return this.activeDirectory;
    }

    public Properties getConnectionProperties() {
        return this.connectionProperties;
    }

    public LDAPMappingConfiguration mappingConfig(Class<? extends AttributedType> clazz) {
        LDAPMappingConfiguration mappingConfig = new LDAPMappingConfiguration(clazz);
        this.mappingConfig.put(clazz, mappingConfig);
        return mappingConfig;
    }

    public Class<? extends AttributedType> getSupportedTypeByBaseDN(String entryDN, List<String> objectClasses) {
        String entryBaseDN = entryDN.substring(entryDN.indexOf(LDAPConstants.COMMA) + 1);

        for (LDAPMappingConfiguration mappingConfig : this.mappingConfig.values()) {
            if (mappingConfig.getBaseDN() != null) {

                if (mappingConfig.getBaseDN().equalsIgnoreCase(entryDN)
                        || mappingConfig.getParentMapping().values().contains(entryDN)) {
                    return mappingConfig.getMappedClass();
                }

                if (mappingConfig.getBaseDN().equalsIgnoreCase(entryBaseDN)
                        || mappingConfig.getParentMapping().values().contains(entryBaseDN)) {
                    return mappingConfig.getMappedClass();
                }
            }
        }

        for (LDAPMappingConfiguration mappingConfig : this.mappingConfig.values()) {
            for (String objectClass : objectClasses) {
                if (mappingConfig.getObjectClasses().contains(objectClass)) {
                    return mappingConfig.getMappedClass();
                }
            }
        }

        throw new ModelException("No type found with Base DN [" + entryDN + "] or objectClasses [" + objectClasses + ".");
    }

    public LDAPMappingConfiguration getMappingConfig(Class<? extends AttributedType> attributedType) {
        for (LDAPMappingConfiguration mappingConfig : this.mappingConfig.values()) {
            if (attributedType.equals(mappingConfig.getMappedClass())) {
                return mappingConfig;
            }
        }

        return null;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getUniqueIdentifierAttributeName() {
        return uniqueIdentifierAttributeName;
    }

    public boolean isPagination() {
        return pagination;
    }

    public boolean isUserAccountControlsAfterPasswordUpdate() {
        return userAccountControlsAfterPasswordUpdate;
    }

    public LDAPIdentityStoreConfiguration setLdapURL(String ldapURL) {
        this.ldapURL = ldapURL;
        return this;
    }

    public LDAPIdentityStoreConfiguration setFactoryName(String factoryName) {
        this.factoryName = factoryName;
        return this;
    }

    public LDAPIdentityStoreConfiguration setAuthType(String authType) {
        this.authType = authType;
        return this;
    }

    public LDAPIdentityStoreConfiguration setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public LDAPIdentityStoreConfiguration setBindDN(String bindDN) {
        this.bindDN = bindDN;
        return this;
    }

    public LDAPIdentityStoreConfiguration setBindCredential(String bindCredential) {
        this.bindCredential = bindCredential;
        return this;
    }

    public LDAPIdentityStoreConfiguration setActiveDirectory(boolean activeDirectory) {
        this.activeDirectory = activeDirectory;
        return this;
    }

    public LDAPIdentityStoreConfiguration setPagination(boolean pagination) {
        this.pagination = pagination;
        return this;
    }

    public LDAPIdentityStoreConfiguration setConnectionProperties(Properties connectionProperties) {
        this.connectionProperties = connectionProperties;
        return this;
    }

    public LDAPIdentityStoreConfiguration setUniqueIdentifierAttributeName(String uniqueIdentifierAttributeName) {
        this.uniqueIdentifierAttributeName = uniqueIdentifierAttributeName;
        return this;
    }

    public LDAPIdentityStoreConfiguration setUserAccountControlsAfterPasswordUpdate(boolean userAccountControlsAfterPasswordUpdate) {
        this.userAccountControlsAfterPasswordUpdate = userAccountControlsAfterPasswordUpdate;
        return this;
    }

    public LDAPIdentityStoreConfiguration setBaseDN(String baseDN) {
        this.baseDN = baseDN;
        return this;
    }
}
