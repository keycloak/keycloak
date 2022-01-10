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

import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.ApplicationPath;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.quarkus.runtime.services.resources.QuarkusWelcomeResource;
import org.keycloak.services.resources.WelcomeResource;

@ApplicationPath("/")
public class QuarkusKeycloakApplication extends KeycloakApplication {

    private static final String KEYCLOAK_ADMIN_ENV_VAR = "KEYCLOAK_ADMIN";
    private static final String KEYCLOAK_ADMIN_PASSWORD_ENV_VAR = "KEYCLOAK_ADMIN_PASSWORD";

    private static boolean filterSingletons(Object o) {
        return !WelcomeResource.class.isInstance(o);
    }

    @Inject
    Instance<EntityManagerFactory> entityManagerFactory;

    @Override
    protected void startup() {
        forceEntityManagerInitialization();
        initializeKeycloakSessionFactory();
        setupScheduledTasks(sessionFactory);
        createAdminUser();
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = super.getSingletons().stream()
                .filter(QuarkusKeycloakApplication::filterSingletons)
                .collect(Collectors.toSet());

        singletons.add(new QuarkusWelcomeResource());

        return singletons;
    }

    private void initializeKeycloakSessionFactory() {
        QuarkusKeycloakSessionFactory instance = QuarkusKeycloakSessionFactory.getInstance();
        sessionFactory = instance;
        instance.init();
        sessionFactory.publish(new PostMigrationEvent());
    }

    private void forceEntityManagerInitialization() {
        // also forces an initialization of the entity manager so that providers don't need to wait for any initialization logic
        // when first creating an entity manager
        entityManagerFactory.get().createEntityManager().close();
    }

    private void createAdminUser() {
        String adminUserName = System.getenv(KEYCLOAK_ADMIN_ENV_VAR);
        String adminPassword = System.getenv(KEYCLOAK_ADMIN_PASSWORD_ENV_VAR);

        if ((adminUserName == null || adminUserName.trim().length() == 0)
                || (adminPassword == null || adminPassword.trim().length() == 0)) {
            return;
        }

        KeycloakSessionFactory sessionFactory = KeycloakApplication.getSessionFactory();
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
