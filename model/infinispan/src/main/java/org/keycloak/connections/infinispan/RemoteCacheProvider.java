/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.infinispan;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;

/**
 * Get either just remoteCache associated with remoteStore associated with infinispan cache of given name. If security is enabled, then
 * return secured remoteCache based on the template provided by remoteStore configuration but with added "authentication" configuration
 * of secured hotrod endpoint (RemoteStore doesn't yet allow to configure "security" of hotrod endpoints)
 *
 * TODO: Remove this class once we upgrade to infinispan version, which allows to configure security for remoteStore itself
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RemoteCacheProvider {

    protected static final Logger logger = Logger.getLogger(RemoteCacheProvider.class);

    private final Config.Scope config;
    private final EmbeddedCacheManager cacheManager;

    private final Map<String, RemoteCache> availableCaches = new HashMap<>();

    // Enlist secured managers, which are managed by us and should be shutdown on stop
    private final List<RemoteCacheManager> managedManagers = new LinkedList<>();

    public RemoteCacheProvider(Config.Scope config, EmbeddedCacheManager cacheManager) {
        this.config = config;
        this.cacheManager = cacheManager;
    }

    public RemoteCache getRemoteCache(String cacheName) {
        if (availableCaches.get(cacheName) == null) {
            synchronized (this) {
                if (availableCaches.get(cacheName) == null) {
                    RemoteCache remoteCache = loadRemoteCache(cacheName);
                    availableCaches.put(cacheName, remoteCache);
                }
            }
        }

        return availableCaches.get(cacheName);
    }

    public void stop() {
        // TODO:mposolda
        logger.infof("Shutdown %d registered secured remoteCache managers", managedManagers.size());

        for (RemoteCacheManager mgr : managedManagers) {
            mgr.stop();
        }
    }


    protected RemoteCache loadRemoteCache(String cacheName) {
        RemoteCache remoteCache = InfinispanUtil.getRemoteCache(cacheManager.getCache(cacheName));

        if (config.getBoolean("remoteStoreSecurityEnabled", false)) {
            // TODO:mposolda
            logger.info("Remote store security is enabled");
            RemoteCacheManager securedMgr = createSecuredRemoteCacheManager(config, remoteCache.getRemoteCacheManager());
            managedManagers.add(securedMgr);
            return securedMgr.getCache(remoteCache.getName());
        } else {
            // TODO:mposolda
            logger.info("Remote store security is disabled");
            return remoteCache;
        }
    }


    protected RemoteCacheManager createSecuredRemoteCacheManager(Config.Scope config, RemoteCacheManager origManager) {
        String serverName = config.get("remoteStoreSecurityServerName", "keycloak-server");
        String realm = config.get("remoteStoreSecurityRealm", "ApplicationRealm");

        String securedHotRodEndpoint = config.get("remoteStoreSecurityHotRodEndpoint");
        String username = config.get("remoteStoreSecurityUsername");
        String password = config.get("remoteStoreSecurityPassword");

        // TODO:mposolda
        logger.infof("Server: '%s', Realm: '%s', Username: '%s', Secured HotRod endpoint: '%s'", serverName, realm, username, securedHotRodEndpoint);

        // Create configuration template from the original configuration provided at remoteStore level
        Configuration origConfig = origManager.getConfiguration();

        ConfigurationBuilder cfgBuilder = new ConfigurationBuilder()
                .read(origConfig);

        // Workaround as I need a way to override servers and it's not possible to remove existing :/
        try {
            Field serversField = cfgBuilder.getClass().getDeclaredField("servers");
            Reflections.setAccessible(serversField);
            List origServers = Reflections.getFieldValue(serversField, cfgBuilder, List.class);
            origServers.clear();
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        }

        // Create configuration based on the configuration template from remoteStore. Just add security and override secured endpoint
        Configuration newConfig = cfgBuilder
                .addServers(securedHotRodEndpoint)
                .security()
                  .authentication()
                    .serverName(serverName) //define server name, should be specified in XML configuration on JDG side
                    .saslMechanism("DIGEST-MD5") // define SASL mechanism, in this example we use DIGEST with MD5 hash
                    .callbackHandler(new LoginHandler(username, password.toCharArray(), realm)) // define login handler, implementation defined
                    .enable()
                .build();

        return new RemoteCacheManager(newConfig);
    }


    private static class LoginHandler implements CallbackHandler {
        final private String login;
        final private char[] password;
        final private String realm;

        private LoginHandler(String login, char[] password, String realm) {
            this.login = login;
            this.password = password;
            this.realm = realm;
        }

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    ((NameCallback) callback).setName(login);
                } else if (callback instanceof PasswordCallback) {
                    ((PasswordCallback) callback).setPassword(password);
                } else if (callback instanceof RealmCallback) {
                    ((RealmCallback) callback).setText(realm);
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
        }
    }
}
