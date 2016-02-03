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

package org.keycloak.federation.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
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
            logLDAPConfig(model.getDisplayName(), config);

            LDAPIdentityStore store = createLdapIdentityStore(config);
            context = new LDAPIdentityStoreContext(config, store);
            ldapStores.put(model.getId(), context);
        }
        return context.store;
    }

    // Don't log LDAP password
    private void logLDAPConfig(String fedProviderDisplayName, Map<String, String> ldapConfig) {
        Map<String, String> copy = new HashMap<String, String>(ldapConfig);
        copy.remove(LDAPConstants.BIND_CREDENTIAL);
        logger.infof("Creating new LDAP based partition manager for the Federation provider: " + fedProviderDisplayName + ", LDAP Configuration: " + copy);
    }

    /**
     * @param ldapConfig from realm
     * @return PartitionManager instance based on LDAP store
     */
    public static LDAPIdentityStore createLdapIdentityStore(Map<String,String> ldapConfig) {
        LDAPConfig cfg = new LDAPConfig(ldapConfig);

        checkSystemProperty("com.sun.jndi.ldap.connect.pool.authentication", "none simple");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.initsize", "1");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.maxsize", "1000");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.prefsize", "5");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.timeout", "300000");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.protocol", "plain");
        checkSystemProperty("com.sun.jndi.ldap.connect.pool.debug", "off");

        return new LDAPIdentityStore(cfg);
    }

    private static void checkSystemProperty(String name, String defaultValue) {
        if (System.getProperty(name) == null) {
            System.setProperty(name, defaultValue);
        }
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
