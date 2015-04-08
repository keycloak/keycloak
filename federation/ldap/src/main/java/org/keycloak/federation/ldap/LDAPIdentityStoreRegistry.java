package org.keycloak.federation.ldap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.idm.model.LDAPUser;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStoreConfiguration;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPMappingConfiguration;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserFederationProviderModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPIdentityStoreRegistry {

    private static final Logger logger = Logger.getLogger(LDAPIdentityStoreRegistry.class);

    private Map<String, LDAPIdentityStoreContext> ldapStores = new ConcurrentHashMap<String, LDAPIdentityStoreContext>();

    public LDAPIdentityStore getLdapStore(UserFederationProviderModel model) {
        LDAPIdentityStoreContext context = ldapStores.get(model.getId());

        // Ldap config might have changed for the realm. In this case, we must re-initialize
        Map<String, String> config = model.getConfig();
        if (context == null || !config.equals(context.config)) {
            logLDAPConfig(model.getId(), config);

            LDAPIdentityStore store = createLdapIdentityStore(config);
            context = new LDAPIdentityStoreContext(config, store);
            ldapStores.put(model.getId(), context);
        }
        return context.store;
    }

    // Don't log LDAP password
    private void logLDAPConfig(String fedProviderId, Map<String, String> ldapConfig) {
        Map<String, String> copy = new HashMap<String, String>(ldapConfig);
        copy.remove(LDAPConstants.BIND_CREDENTIAL);
        logger.infof("Creating new LDAP based partition manager for the Federation provider: " + fedProviderId + ", LDAP Configuration: " + copy);
    }

    /**
     * @param ldapConfig from realm
     * @return PartitionManager instance based on LDAP store
     */
    public static LDAPIdentityStore createLdapIdentityStore(Map<String,String> ldapConfig) {
        Properties connectionProps = new Properties();
        if (ldapConfig.containsKey(LDAPConstants.CONNECTION_POOLING)) {
            connectionProps.put("com.sun.jndi.ldap.connect.pool", ldapConfig.get(LDAPConstants.CONNECTION_POOLING));
        }

        checkSystemProperty("com.sun.jndi.ldap.connect.pool.authentication", "none simple");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.initsize", "1");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.maxsize", "1000");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.prefsize", "5");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.timeout", "300000");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.protocol", "plain");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.debug", "off");

        String vendor = ldapConfig.get(LDAPConstants.VENDOR);

        boolean activeDirectory = vendor != null && vendor.equals(LDAPConstants.VENDOR_ACTIVE_DIRECTORY);

        String ldapLoginNameMapping = ldapConfig.get(LDAPConstants.USERNAME_LDAP_ATTRIBUTE);
        if (ldapLoginNameMapping == null) {
            ldapLoginNameMapping = activeDirectory ? LDAPConstants.CN : LDAPConstants.UID;
        }

        String ldapFirstNameMapping = activeDirectory ?  "givenName" : LDAPConstants.CN;
        String createTimestampMapping = activeDirectory ? "whenCreated" : LDAPConstants.CREATE_TIMESTAMP;
        String modifyTimestampMapping = activeDirectory ? "whenChanged" : LDAPConstants.MODIFY_TIMESTAMP;
        String[] userObjectClasses = getUserObjectClasses(ldapConfig);

        boolean pagination = ldapConfig.containsKey(LDAPConstants.PAGINATION) ? Boolean.parseBoolean(ldapConfig.get(LDAPConstants.PAGINATION)) : false;
        boolean userAccountControlsAfterPasswordUpdate = ldapConfig.containsKey(LDAPConstants.USER_ACCOUNT_CONTROLS_AFTER_PASSWORD_UPDATE) ?
                Boolean.parseBoolean(ldapConfig.get(LDAPConstants.USER_ACCOUNT_CONTROLS_AFTER_PASSWORD_UPDATE)) : false;

        // Differences of unique attribute among various vendors
        String uniqueIdentifierAttributeName = LDAPConstants.ENTRY_UUID;
        if (vendor != null) {
            switch (vendor) {
                case LDAPConstants.VENDOR_RHDS:
                    uniqueIdentifierAttributeName = "nsuniqueid";
                    break;
                case LDAPConstants.VENDOR_TIVOLI:
                    uniqueIdentifierAttributeName = "uniqueidentifier";
                    break;
                case LDAPConstants.VENDOR_ACTIVE_DIRECTORY:
                    uniqueIdentifierAttributeName = LDAPConstants.OBJECT_GUID;
            }
        }

        LDAPIdentityStoreConfiguration ldapStoreConfig = new LDAPIdentityStoreConfiguration()
                .setConnectionProperties(connectionProps)
                .setBaseDN(ldapConfig.get(LDAPConstants.BASE_DN))
                .setBindDN(ldapConfig.get(LDAPConstants.BIND_DN))
                .setBindCredential(ldapConfig.get(LDAPConstants.BIND_CREDENTIAL))
                .setLdapURL(ldapConfig.get(LDAPConstants.CONNECTION_URL))
                .setActiveDirectory(activeDirectory)
                .setPagination(pagination)
                .setUniqueIdentifierAttributeName(uniqueIdentifierAttributeName)
                .setFactoryName("com.sun.jndi.ldap.LdapCtxFactory")
                .setAuthType("simple")
                .setUserAccountControlsAfterPasswordUpdate(userAccountControlsAfterPasswordUpdate);

        LDAPMappingConfiguration ldapUserMappingConfig = ldapStoreConfig
                .mappingConfig(LDAPUser.class)
                .setBaseDN(ldapConfig.get(LDAPConstants.USER_DN_SUFFIX))
                .setObjectClasses(new HashSet<String>(Arrays.asList(userObjectClasses)))
                .setIdPropertyName("loginName")
                .addAttributeMapping("loginName", ldapLoginNameMapping)
                .addAttributeMapping("firstName", ldapFirstNameMapping)
                .addAttributeMapping("lastName", LDAPConstants.SN)
                .addAttributeMapping("email", LDAPConstants.EMAIL)
                .addReadOnlyAttributeMapping("createdDate", createTimestampMapping)
                .addReadOnlyAttributeMapping("modifyDate", modifyTimestampMapping);

        if (activeDirectory && ldapLoginNameMapping.equals("sAMAccountName")) {
            ldapUserMappingConfig.setBindingPropertyName("fullName");
            ldapUserMappingConfig.addAttributeMapping("fullName", LDAPConstants.CN);
            logger.infof("Using 'cn' attribute for DN of user and 'sAMAccountName' for username");
        }

        return new LDAPIdentityStore(ldapStoreConfig);
    }

    private static void checkSystemProperty(String name, String defaultValue) {
        if (System.getProperty(name) == null) {
            System.setProperty(name, defaultValue);
        }
    }

    // Parse array of strings like [ "inetOrgPerson", "organizationalPerson" ] from the string like: "inetOrgPerson, organizationalPerson"
    private static String[] getUserObjectClasses(Map<String,String> ldapConfig) {
        String objClassesCfg = ldapConfig.get(LDAPConstants.USER_OBJECT_CLASSES);
        String objClassesStr = (objClassesCfg != null && objClassesCfg.length() > 0) ? objClassesCfg.trim() : "inetOrgPerson, organizationalPerson";

        String[] objectClasses = objClassesStr.split(",");

        // Trim them
        String[] userObjectClasses = new String[objectClasses.length];
        for (int i=0 ; i<objectClasses.length ; i++) {
            userObjectClasses[i] = objectClasses[i].trim();
        }
        return userObjectClasses;
    }

    private class LDAPIdentityStoreContext {

        private LDAPIdentityStoreContext(Map<String,String> config, LDAPIdentityStore store) {
            this.config = config;
            this.store = store;
        }

        private Map<String,String> config;
        private LDAPIdentityStore store;
    }
}
