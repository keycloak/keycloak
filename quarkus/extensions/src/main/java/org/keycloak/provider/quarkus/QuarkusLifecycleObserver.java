/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.provider.quarkus;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.platform.Platform;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;

@ApplicationScoped
public class QuarkusLifecycleObserver {

    private static final String KEYCLOAK_ADMIN_ENV_VAR = "KEYCLOAK_ADMIN";
    private static final String KEYCLOAK_ADMIN_PASSWORD_ENV_VAR = "KEYCLOAK_ADMIN_PASSWORD";

    @Inject
    ServletContext servletContext;

    private void onStartupEvent(@Observes StartupEvent event) {

        Runnable startupHook = ((QuarkusPlatform) Platform.getPlatform()).startupHook;

        if (startupHook != null) {
            startupHook.run();
            createAdminUser();
        }

    }

    private void onShutdownEvent(@Observes ShutdownEvent event) {

        Runnable shutdownHook = ((QuarkusPlatform) Platform.getPlatform()).shutdownHook;

        if (shutdownHook != null)
            shutdownHook.run();

    }

    private void createAdminUser() {
        String adminUserName = System.getenv(KEYCLOAK_ADMIN_ENV_VAR);
        String adminPassword = System.getenv(KEYCLOAK_ADMIN_PASSWORD_ENV_VAR);

        if ((adminUserName == null || adminUserName.trim().length() == 0)
                || (adminPassword == null || adminPassword.trim().length() == 0)) {
            return;
        }

        KeycloakSessionFactory sessionFactory = (KeycloakSessionFactory) servletContext
                .getAttribute(KeycloakSessionFactory.class.getName());
        KeycloakSession session = sessionFactory.create();
        KeycloakTransactionManager transaction = session.getTransactionManager();

        try {
            transaction.begin();

            new ApplianceBootstrap(session).createMasterRealmUser(adminUserName, adminPassword);
            ServicesLogger.LOGGER.addUserSuccess(adminUserName, Config.getAdminRealm());

            transaction.commit();
        } catch (IllegalStateException e) {
            session.getTransactionManager().rollback();
            ServicesLogger.LOGGER.addUserFailedUserExists(adminUserName, Config.getAdminRealm());
        } catch (Throwable t) {
            session.getTransactionManager().rollback();
            ServicesLogger.LOGGER.addUserFailed(t, adminUserName, Config.getAdminRealm());
        } finally {
            session.close();
        }
    }
}
