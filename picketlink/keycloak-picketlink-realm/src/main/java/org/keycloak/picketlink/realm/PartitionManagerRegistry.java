package org.keycloak.picketlink.realm;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;
import org.keycloak.picketlink.idm.LDAPAgentIgnoreCredentialHandler;
import org.keycloak.picketlink.idm.LdapConstants;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.internal.DefaultPartitionManager;
import org.picketlink.idm.model.basic.User;

import static org.picketlink.common.constants.LDAPConstants.CN;
import static org.picketlink.common.constants.LDAPConstants.CREATE_TIMESTAMP;
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
            logger.infof("Creating new partition manager for the realm: %s, LDAP Connection URL: %s, LDAP Base DN: %s", realm.getId(),
                    ldapConfig.get(LdapConstants.CONNECTION_URL), ldapConfig.get(LdapConstants.BASE_DN));
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

        // Use same mapping for User and Agent for now
        builder
            .named("SIMPLE_LDAP_STORE_CONFIG")
                .stores()
                    .ldap()
                        .connectionProperties(connectionProps)
                        .addCredentialHandler(LDAPAgentIgnoreCredentialHandler.class)
                        .baseDN(ldapConfig.get(LdapConstants.BASE_DN))
                        .bindDN(ldapConfig.get(LdapConstants.BIND_DN))
                        .bindCredential(ldapConfig.get(LdapConstants.BIND_CREDENTIAL))
                        .url(ldapConfig.get(LdapConstants.CONNECTION_URL))
                        .supportAllFeatures()
                        .mapping(User.class)
                            .baseDN(ldapConfig.get(LdapConstants.USER_DN_SUFFIX))
                            .objectClasses("inetOrgPerson", "organizationalPerson")
                            .attribute("loginName", UID, true)
                            .attribute("firstName", CN)
                            .attribute("lastName", SN)
                            .attribute("email", EMAIL)
                            .readOnlyAttribute("createdDate", CREATE_TIMESTAMP);

        return new DefaultPartitionManager(builder.buildAll());
    }

    private void checkSystemProperty(String name, String defaultValue) {
        if (System.getProperty(name) == null) {
            System.setProperty(name, defaultValue);
        }
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
