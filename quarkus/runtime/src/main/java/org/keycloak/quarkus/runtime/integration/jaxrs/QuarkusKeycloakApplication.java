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

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Blocking;

import org.keycloak.Config;
import org.keycloak.config.BootstrapAdminOptions;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.platform.Platform;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.command.AbstractNonServerCommand;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;
import org.keycloak.quarkus.runtime.integration.QuarkusPlatform;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.utils.StringUtil;

import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.ApplicationPath;

@ApplicationPath("/")
@Blocking
public class QuarkusKeycloakApplication extends KeycloakApplication {

    private static final String KEYCLOAK_ADMIN_ENV_VAR = "KEYCLOAK_ADMIN";
    private static final String KEYCLOAK_ADMIN_PROP_VAR = "keycloakAdmin";
    private static final String KEYCLOAK_ADMIN_PASSWORD_ENV_VAR = "KEYCLOAK_ADMIN_PASSWORD";
    private static final String KEYCLOAK_ADMIN_PASSWORD_PROP_VAR = "keycloakAdminPassword";

    void onStartupEvent(@Observes StartupEvent event) {
        QuarkusPlatform platform = (QuarkusPlatform) Platform.getPlatform();
        platform.started();
        startup();
        Environment.getParsedCommand().ifPresent(ac -> {
            if (ac instanceof AbstractNonServerCommand) {
                ((AbstractNonServerCommand)ac).onStart(this);
            }
        });
    }

    void onShutdownEvent(@Observes ShutdownEvent event) {
        shutdown();
    }

    @Override
    public KeycloakSessionFactory createSessionFactory() {
        QuarkusKeycloakSessionFactory instance = QuarkusKeycloakSessionFactory.getInstance();
        instance.init();
        return instance;
    }

    @Override
    protected void loadConfig() {
        // no need to load config provider because we force quarkus impl
    }

    @Override
    protected void createTemporaryAdmin(KeycloakSession session) {
        var adminUsername = Configuration.getOptionalKcValue(BootstrapAdminOptions.USERNAME.getKey()).orElse(getEnvOrProp(KEYCLOAK_ADMIN_ENV_VAR, KEYCLOAK_ADMIN_PROP_VAR));
        var adminPassword = Configuration.getOptionalKcValue(BootstrapAdminOptions.PASSWORD.getKey()).orElse(getEnvOrProp(KEYCLOAK_ADMIN_PASSWORD_ENV_VAR, KEYCLOAK_ADMIN_PASSWORD_PROP_VAR));

        var clientId = Configuration.getOptionalKcValue(BootstrapAdminOptions.CLIENT_ID.getKey()).orElse(null);
        var clientSecret = Configuration.getOptionalKcValue(BootstrapAdminOptions.CLIENT_SECRET.getKey()).orElse(null);

        try {
            //Integer expiration = Configuration.getOptionalKcValue(BootstrapAdminOptions.EXPIRATION.getKey()).map(Integer::valueOf).orElse(null);
            if (StringUtil.isNotBlank(adminPassword)) {
                createTemporaryMasterRealmAdminUser(adminUsername, adminPassword, /*expiration,*/ session);
            }
            if (StringUtil.isNotBlank(clientSecret)) {
                createTemporaryMasterRealmAdminService(clientId, clientSecret, /*expiration,*/ session);
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid admin expiration value provided. An integer is expected.", e);
        }
    }

    public void createTemporaryMasterRealmAdminUser(String adminUserName, String adminPassword, /*Integer adminExpiration,*/ KeycloakSession existingSession) {
        KeycloakSessionTask task = session -> {
            new ApplianceBootstrap(session).createTemporaryMasterRealmAdminUser(adminUserName, adminPassword /*, adminExpiration*/, false);
        };

        runAdminTask(adminUserName, existingSession, task);
    }

    public void createTemporaryMasterRealmAdminService(String clientId, String clientSecret, /*Integer adminExpiration,*/ KeycloakSession existingSession) {
        KeycloakSessionTask task = session -> {
            new ApplianceBootstrap(session).createTemporaryMasterRealmAdminService(clientId, clientSecret /*, adminExpiration*/);
        };

        runAdminTask(clientId, existingSession, task);
    }

    private void runAdminTask(String adminUserName, KeycloakSession existingSession, KeycloakSessionTask task) {
        try {
            if (existingSession == null) {
                KeycloakSessionFactory sessionFactory = KeycloakApplication.getSessionFactory();
                KeycloakModelUtils.runJobInTransaction(sessionFactory, task);
            } else {
                task.run(existingSession);
            }
        } catch (Throwable t) {
            ServicesLogger.LOGGER.addUserFailed(t, adminUserName, Config.getAdminRealm());
        }
    }

    private String getEnvOrProp(String envKey, String propKey) {
        String value = System.getenv(envKey);
        return value != null ? value : System.getProperty(propKey);
    }

}
