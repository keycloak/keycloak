package org.keycloak.picketlink.ldap;

import org.jboss.logging.Logger;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.picketlink.idm.KeycloakEventBridge;
import org.keycloak.picketlink.idm.LDAPKeycloakCredentialHandler;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.config.LDAPMappingConfigurationBuilder;
import org.picketlink.idm.config.LDAPStoreConfigurationBuilder;
import org.picketlink.idm.internal.DefaultPartitionManager;
import org.picketlink.idm.model.basic.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static org.picketlink.common.constants.LDAPConstants.*;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PartitionManagerRegistry {

    private static final Logger logger = Logger.getLogger(PartitionManagerRegistry.class);

    private Map<String, PartitionManagerContext> partitionManagers = new ConcurrentHashMap<String, PartitionManagerContext>();

    public PartitionManager getPartitionManager(UserFederationProviderModel model) {
        PartitionManagerContext context = partitionManagers.get(model.getId());

        // Ldap config might have changed for the realm. In this case, we must re-initialize
        Map<String, String> config = model.getConfig();
        if (context == null || !config.equals(context.config)) {
            logLDAPConfig(model.getId(), config);

            PartitionManager manager = createPartitionManager(config);
            context = new PartitionManagerContext(config, manager);
            partitionManagers.put(model.getId(), context);
        }
        return context.partitionManager;
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
    public static PartitionManager createPartitionManager(Map<String,String> ldapConfig) {
        IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();

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
            ldapLoginNameMapping = activeDirectory ? CN : UID;
        }

        String ldapFirstNameMapping = activeDirectory ?  "givenName" : CN;
        String createTimestampMapping = activeDirectory ? "whenCreated" : CREATE_TIMESTAMP;
        String modifyTimestampMapping = activeDirectory ? "whenChanged" : MODIFY_TIMESTAMP;
        String[] userObjectClasses = getUserObjectClasses(ldapConfig);

        boolean pagination = ldapConfig.containsKey(LDAPConstants.PAGINATION) ? Boolean.parseBoolean(ldapConfig.get(LDAPConstants.PAGINATION)) : false;

        // Use same mapping for User and Agent for now
        LDAPStoreConfigurationBuilder ldapStoreBuilder =
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
                        .pagination(pagination);

        // RHDS is using "nsuniqueid" as unique identifier instead of "entryUUID"
        if (vendor != null && vendor.equals(LDAPConstants.VENDOR_RHDS)) {
            ldapStoreBuilder.uniqueIdentifierAttributeName("nsuniqueid");
        } else if (LDAPConstants.VENDOR_TIVOLI.equals(vendor)) {
            ldapStoreBuilder.uniqueIdentifierAttributeName("uniqueidentifier");
        }

        LDAPMappingConfigurationBuilder ldapUserMappingBuilder = ldapStoreBuilder
            .mapping(User.class)
                .baseDN(ldapConfig.get(LDAPConstants.USER_DN_SUFFIX))
                .objectClasses(userObjectClasses)
                .attribute("loginName", ldapLoginNameMapping, true)
                .attribute("firstName", ldapFirstNameMapping)
                .attribute("lastName", SN)
                .attribute("email", EMAIL)
                .readOnlyAttribute("createdDate", createTimestampMapping)
                .readOnlyAttribute("modifyDate", modifyTimestampMapping);

        if (activeDirectory && ldapLoginNameMapping.equals("sAMAccountName")) {
            ldapUserMappingBuilder.bindingAttribute("fullName", CN);
            logger.infof("Using 'cn' attribute for DN of user and 'sAMAccountName' for username");
        }

        KeycloakEventBridge eventBridge = new KeycloakEventBridge(activeDirectory && "true".equals(ldapConfig.get(LDAPConstants.USER_ACCOUNT_CONTROLS_AFTER_PASSWORD_UPDATE)));
        return new DefaultPartitionManager(builder.buildAll(), eventBridge, null);
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

    private class PartitionManagerContext {

        private PartitionManagerContext(Map<String,String> config, PartitionManager manager) {
            this.config = config;
            this.partitionManager = manager;
        }

        private Map<String,String> config;
        private PartitionManager partitionManager;
    }
}
