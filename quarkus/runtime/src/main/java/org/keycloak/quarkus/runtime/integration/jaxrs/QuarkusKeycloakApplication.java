/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.integration.jaxrs;

import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.ApplicationPath;

import org.keycloak.config.BootstrapAdminOptions;
import org.keycloak.config.ServerOptions;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;
import org.keycloak.quarkus.runtime.configuration.PropertyMappingInterceptor;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;
import org.keycloak.quarkus.runtime.storage.database.jpa.QuarkusJpaConnectionProviderFactory;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.utils.StringUtil;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownDelayInitiatedEvent;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Blocking;
import org.jboss.logging.Logger;

import static org.keycloak.common.util.Environment.isDevMode;
import static org.keycloak.common.util.Environment.isNonServerMode;
import static org.keycloak.quarkus.runtime.Environment.hasEarlyExitLaunchMode;

@ApplicationPath("/")
@Blocking
public class QuarkusKeycloakApplication extends KeycloakApplication {

    private static final String KEYCLOAK_ADMIN_ENV_VAR = "KEYCLOAK_ADMIN";
    private static final String KEYCLOAK_ADMIN_PASSWORD_ENV_VAR = "KEYCLOAK_ADMIN_PASSWORD";

    private static final Logger logger = Logger.getLogger(QuarkusKeycloakApplication.class);

    @Override
    protected String getDataDir() {
        return Environment.getDataDir().orElseGet(() -> {
            logger.warnf("%s is not set, the system temporary directory will be used as the Keycloak data directory.", Environment.KC_HOME_DIR);
            return System.getProperty("java.io.tmpdir");
        });
    }

    @Override
    protected void exit(Throwable cause) {
        Quarkus.asyncExit(1);
    }

    void onStartupEvent(@Observes StartupEvent event) {
        startup();
    }

    void onShutdownEvent(@Observes ShutdownEvent event) {
        shutdown();
    }

    void onShutdownDelayInitiatedEvent(@Observes ShutdownDelayInitiatedEvent event) {
        shutdownDelayInitiated();
    }

    @Override
    public DefaultKeycloakSessionFactory createSessionFactory() {
        return Arc.container().instance(QuarkusKeycloakSessionFactory.class).get();
    }

    @Override
    protected void initAndStart() {
        // no need - is handled by Quarkus logic and onStartupEvent
    }

    @Override
    protected void createTemporaryAdmin(KeycloakSession session) {
        var adminUsername = getOption(BootstrapAdminOptions.USERNAME.getKey(), KEYCLOAK_ADMIN_ENV_VAR);
        var adminPassword = getOption(BootstrapAdminOptions.PASSWORD.getKey(), KEYCLOAK_ADMIN_PASSWORD_ENV_VAR);

        var clientId = Configuration.getOptionalKcValue(BootstrapAdminOptions.CLIENT_ID.getKey()).orElse(null);
        var clientSecret = Configuration.getOptionalKcValue(BootstrapAdminOptions.CLIENT_SECRET.getKey()).orElse(null);

        try {
            //Integer expiration = Configuration.getOptionalKcValue(BootstrapAdminOptions.EXPIRATION.getKey()).map(Integer::valueOf).orElse(null);
            if (StringUtil.isNotBlank(adminPassword) && !createTemporaryMasterRealmAdminUser(adminUsername, adminPassword, /*expiration,*/ session)) {
                throw new RuntimeException("Aborting startup and the creation of the master realm, because the temporary admin user account could not be created.");
            }
            if (StringUtil.isNotBlank(clientSecret) && !createTemporaryMasterRealmAdminService(clientId, clientSecret, /*expiration,*/ session)) {
                throw new RuntimeException("Aborting startup and the creation of the master realm, because the temporary admin service account could not be created.");
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid admin expiration value provided. An integer is expected.", e);
        }
    }

    @Override
    protected boolean supportsAsyncInitialization() {
        var asyncBootstrap = Configuration.getOptionalKcValue(ServerOptions.SERVER_ASYNC_BOOTSTRAP)
                .map(Boolean::parseBoolean)
                .orElse(Boolean.TRUE);
        // skip async bootstrap in dev and non-server mode
        return !isDevMode() && !isNonServerMode() && !hasEarlyExitLaunchMode() && asyncBootstrap;
    }

    @Override
    protected int getTransactionTimeout(DefaultKeycloakSessionFactory sessionFactory) {
        return ((QuarkusJpaConnectionProviderFactory) sessionFactory.getProviderFactory(JpaConnectionProvider.class)).getMigrationTransactionTimeout();
    }

    private String getOption(String option, String envVar) {
        PropertyMappingInterceptor.disable(); // disable default handling
        try {
            return Configuration.getOptionalKcValue(option).orElseGet(() -> {
                String value = System.getenv(envVar);
                if (value != null) {
                    ServicesLogger.LOGGER.usingDeprecatedEnvironmentVariable(envVar, Configuration.toEnvVarFormat(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + option));
                }
                return value;
            });
        } finally {
            PropertyMappingInterceptor.enable();
        }
    }

    public boolean createTemporaryMasterRealmAdminUser(String adminUserName, String adminPassword, /*Integer adminExpiration,*/ KeycloakSession session) {
        return new ApplianceBootstrap(session).createMasterRealmAdminUser(adminUserName, adminPassword, true /*, adminExpiration*/, false);
    }

    public boolean createTemporaryMasterRealmAdminService(String clientId, String clientSecret, /*Integer adminExpiration,*/ KeycloakSession session) {
        return new ApplianceBootstrap(session).createTemporaryMasterRealmAdminService(clientId, clientSecret /*, adminExpiration*/);
    }

}
