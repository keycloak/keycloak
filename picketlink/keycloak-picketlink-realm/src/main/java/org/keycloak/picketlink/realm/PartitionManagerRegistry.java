package org.keycloak.picketlink.realm;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.picketlink.idm.KeycloakLDAPIdentityStore;
import org.keycloak.picketlink.idm.LDAPKeycloakCredentialHandler;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.AbstractIdentityStoreConfiguration;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.config.IdentityStoreConfiguration;
import org.picketlink.idm.config.LDAPIdentityStoreConfiguration;
import org.picketlink.idm.internal.DefaultPartitionManager;
import org.picketlink.idm.model.basic.User;

import static org.picketlink.common.constants.LDAPConstants.CN;
import static org.picketlink.common.constants.LDAPConstants.EMAIL;
import static org.picketlink.common.constants.LDAPConstants.SN;
import static org.picketlink.common.constants.LDAPConstants.UID;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PartitionManagerRegistry {

    private static final Logger logger = Logger.getLogger(PartitionManagerRegistry.class);

    private Map<String, PartitionManagerContext> partitionManagers = new ConcurrentHashMap<String, PartitionManagerContext>();

    public PartitionManager getPartitionManager(RealmModel realm) {
        Map<String,String> ldapConfig = realm.getLdapServerConfig();
        if (ldapConfig == null || ldapConfig.isEmpty()) {
            logger.warnf("Ldap configuration is missing for realm '%s'", realm.getName());
            return null;
        }

        PartitionManagerContext context = partitionManagers.get(realm.getId());

        // Ldap config might have changed for the realm. In this case, we must re-initialize
        if (context == null || !ldapConfig.equals(context.config)) {
            logger.infof("Creating new partition manager for the realm: %s, LDAP Connection URL: %s, LDAP Base DN: %s, LDAP Vendor: %s", realm.getId(),
                    ldapConfig.get(LDAPConstants.CONNECTION_URL), ldapConfig.get(LDAPConstants.BASE_DN), ldapConfig.get(LDAPConstants.VENDOR));
            PartitionManager manager = createPartitionManager(ldapConfig);
            context = new PartitionManagerContext(ldapConfig, manager);
            partitionManagers.put(realm.getId(), context);
        }
        return context.partitionManager;
    }

    /**
     * @param ldapConfig from realm
     * @return PartitionManager instance based on LDAP store
     */
    protected PartitionManager createPartitionManager(Map<String,String> ldapConfig) {
        IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();

        Properties connectionProps = new Properties();
        connectionProps.put("com.sun.jndi.ldap.connect.pool", "true");

        checkSystemProperty("com.sun.jndi.ldap.connect.pool.authentication", "none simple");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.initsize", "1");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.maxsize", "10");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.prefsize", "5");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.timeout", "300000");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.protocol", "plain");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.debug", "off");

        String vendor = ldapConfig.get(LDAPConstants.VENDOR);

        // RHDS is using "nsuniqueid" as unique identifier instead of "entryUUID"
        if (vendor != null && vendor.equals(LDAPConstants.VENDOR_RHDS)) {
            checkSystemProperty(LDAPIdentityStoreConfiguration.ENTRY_IDENTIFIER_ATTRIBUTE_NAME, "nsuniqueid");
        }

        boolean activeDirectory = vendor != null && vendor.equals(LDAPConstants.VENDOR_ACTIVE_DIRECTORY);

        String ldapLoginNameMapping = ldapConfig.get(LDAPConstants.USERNAME_LDAP_ATTRIBUTE);
        if (ldapLoginNameMapping == null) {
            ldapLoginNameMapping = activeDirectory ? CN : UID;
        }

        // Try to compute properties based on LDAP server type, but still allow to override them through System properties TODO: Should allow better way than overriding from System properties. Perhaps init from XML?
        ldapLoginNameMapping = getNameOfLDAPAttribute("keycloak.ldap.idm.loginName", ldapLoginNameMapping, ldapLoginNameMapping, activeDirectory);
        String ldapFirstNameMapping = getNameOfLDAPAttribute("keycloak.ldap.idm.firstName", CN, "givenName", activeDirectory);
        String ldapLastNameMapping = getNameOfLDAPAttribute("keycloak.ldap.idm.lastName", SN, SN, activeDirectory);
        String ldapEmailMapping =  getNameOfLDAPAttribute("keycloak.ldap.idm.email", EMAIL, EMAIL, activeDirectory);

        String[] userObjectClasses = getUserObjectClasses(ldapConfig);

        logger.infof("LDAP Attributes mapping: loginName: %s, firstName: %s, lastName: %s, email: %s", ldapLoginNameMapping, ldapFirstNameMapping, ldapLastNameMapping, ldapEmailMapping);

        // Use same mapping for User and Agent for now
        builder
            .named("SIMPLE_LDAP_STORE_CONFIG")
                .stores()
                    .ldap()
                        .connectionProperties(connectionProps)
                        .addCredentialHandler(LDAPKeycloakCredentialHandler.class)
                        .baseDN(ldapConfig.get(LDAPConstants.BASE_DN))
                        .bindDN(ldapConfig.get(LDAPConstants.BIND_DN))
                        .bindCredential(ldapConfig.get(LDAPConstants.BIND_CREDENTIAL))
                        .url(ldapConfig.get(LDAPConstants.CONNECTION_URL))
                        .activeDirectory(activeDirectory)
                        .supportAllFeatures()
                        .mapping(User.class)
                            .baseDN(ldapConfig.get(LDAPConstants.USER_DN_SUFFIX))
                            .objectClasses(userObjectClasses)
                            .attribute("loginName", ldapLoginNameMapping, true)
                            .attribute("firstName", ldapFirstNameMapping)
                            .attribute("lastName", ldapLastNameMapping)
                            .attribute("email", ldapEmailMapping);

        // Workaround to override the LDAPIdentityStore with our own :/
        List<IdentityConfiguration> identityConfigs = builder.buildAll();
        IdentityStoreConfiguration identityStoreConfig = identityConfigs.get(0).getStoreConfiguration().get(0);
        ((AbstractIdentityStoreConfiguration)identityStoreConfig).setIdentityStoreType(KeycloakLDAPIdentityStore.class);

        return new DefaultPartitionManager(identityConfigs);
    }

    private void checkSystemProperty(String name, String defaultValue) {
        if (System.getProperty(name) == null) {
            System.setProperty(name, defaultValue);
        }
    }

    private String getNameOfLDAPAttribute(String systemPropertyName, String defaultAttrName, String defaultAttrNameInActiveDirectory, boolean activeDirectory) {
        // System property has biggest priority if available
        String sysProperty = System.getProperty(systemPropertyName);
        if (sysProperty != null) {
            return sysProperty;
        }

        return activeDirectory ? defaultAttrNameInActiveDirectory : defaultAttrName;
    }

    // Parse array of strings like [ "inetOrgPerson", "organizationalPerson" ] from the string like: "inetOrgPerson, organizationalPerson"
    private String[] getUserObjectClasses(Map<String,String> ldapConfig) {
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

    private class PartitionManagerContext {

        private PartitionManagerContext(Map<String,String> config, PartitionManager manager) {
            this.config = config;
            this.partitionManager = manager;
        }

        private Map<String,String> config;
        private PartitionManager partitionManager;
    }
}
