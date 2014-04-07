package org.keycloak.spi.picketlink.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.internal.DefaultPartitionManager;
import org.picketlink.idm.model.basic.Agent;
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

        // Use same mapping for User and Agent for now
        builder
            .named("SIMPLE_LDAP_STORE_CONFIG")
                .stores()
                    .ldap()
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

    private class PartitionManagerContext {

        private PartitionManagerContext(Map<String,String> config, PartitionManager manager) {
            this.config = config;
            this.partitionManager = manager;
        }

        private Map<String,String> config;
        private PartitionManager partitionManager;
    }
}
