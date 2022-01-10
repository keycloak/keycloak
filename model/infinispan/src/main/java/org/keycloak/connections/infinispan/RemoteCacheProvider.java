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

import java.util.stream.Collectors;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;

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

    public static final String SCRIPT_CACHE_NAME = "___script_cache";

    protected static final Logger logger = Logger.getLogger(RemoteCacheProvider.class);

    private final Config.Scope config;
    private final EmbeddedCacheManager cacheManager;

    private final Map<String, RemoteCache> availableCaches = new HashMap<>();

    // Enlist secured managers, which are managed by us and should be shutdown on stop
    private final Map<String, RemoteCacheManager> managedManagers = new HashMap<>();

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
        logger.debugf("Shutdown %d registered secured remoteCache managers", managedManagers.size());

        for (RemoteCacheManager mgr : managedManagers.values()) {
            mgr.stop();
        }
    }


    protected synchronized RemoteCache loadRemoteCache(String cacheName) {
        RemoteCache remoteCache = InfinispanUtil.getRemoteCache(cacheManager.getCache(cacheName));

        if (remoteCache != null) {
            logger.infof("Hotrod version for remoteCache %s: %s", remoteCache.getName(), remoteCache.getRemoteCacheManager().getConfiguration().version());
        }

        Boolean remoteStoreSecurity = config.getBoolean("remoteStoreSecurityEnabled");
        if (remoteStoreSecurity == null) {
            try {
                logger.debugf("Detecting remote security settings of HotRod server, cache %s. Disable by explicitly setting \"remoteStoreSecurityEnabled\" property in spi=connectionsInfinispan/provider=default", cacheName);
                remoteStoreSecurity = false;
                final RemoteCache<Object, Object> scriptCache = remoteCache.getRemoteCacheManager().getCache(SCRIPT_CACHE_NAME);
                if (scriptCache == null) {
                    logger.debug("Cannot detect remote security settings of HotRod server, disabling.");
                } else {
                    scriptCache.containsKey("");
                }
            } catch (HotRodClientException ex) {
                logger.debug("Seems that HotRod server requires authentication, enabling.");
                remoteStoreSecurity = true;
            }
        }

        if (remoteStoreSecurity) {
            logger.infof("Remote store security for cache %s is enabled. Disable by setting \"remoteStoreSecurityEnabled\" property to \"false\" in spi=connectionsInfinispan/provider=default", cacheName);
            RemoteCacheManager securedMgr = getOrCreateSecuredRemoteCacheManager(config, cacheName, remoteCache.getRemoteCacheManager());
            return securedMgr.getCache(remoteCache.getName());
        } else {
            logger.infof("Remote store security for cache %s is disabled. If server fails to connect to remote JDG server, enable it.", cacheName);
            return remoteCache;
        }
    }


    protected RemoteCacheManager getOrCreateSecuredRemoteCacheManager(Config.Scope config, String cacheName, RemoteCacheManager origManager) {
        String serverName = config.get("remoteStoreSecurityServerName", "keycloak-jdg-server");
        String realm = config.get("remoteStoreSecurityRealm", "AllowScriptManager");

        String username = config.get("remoteStoreSecurityUsername", "___script_manager");
        String password = config.get("remoteStoreSecurityPassword", "not-so-secret-password");

        // Create configuration template from the original configuration provided at remoteStore level
        Configuration origConfig = origManager.getConfiguration();

        ConfigurationBuilder cfgBuilder = new ConfigurationBuilder()
                .read(origConfig);

        String securedHotRodEndpoint = origConfig.servers().stream()
              .map(serverConfiguration -> serverConfiguration.host() + ":" + serverConfiguration.port())
              .collect(Collectors.joining(";"));

        if (managedManagers.containsKey(securedHotRodEndpoint)) {
            return managedManagers.get(securedHotRodEndpoint);
        }

        logger.infof("Creating secured RemoteCacheManager for Server: '%s', Cache: '%s', Realm: '%s', Username: '%s', Secured HotRod endpoint: '%s'", serverName, cacheName, realm, username, securedHotRodEndpoint);

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

        final RemoteCacheManager remoteCacheManager = new RemoteCacheManager(newConfig);
        managedManagers.put(securedHotRodEndpoint, remoteCacheManager);
        return remoteCacheManager;
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
