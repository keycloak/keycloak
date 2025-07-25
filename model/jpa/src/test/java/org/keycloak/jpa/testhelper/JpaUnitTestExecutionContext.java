/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.jpa.testhelper;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.RealmAdapter;
import org.keycloak.models.jpa.UserAdapterTest;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.jpa.entities.UserEntity;

import jakarta.persistence.EntityManager;

/**
 * Helper class that can be used with a ClassRule annotation inside a test class. It establishes a Realm, a KeycloakSession and
 * an in-memory Keycloak database that can be accessed via an EntityManager.
 */
public class JpaUnitTestExecutionContext implements TestRule {

    KeycloakSession keycloakSession;
    EntityManager entityManager;
    RealmModel realm;

    public JpaUnitTestExecutionContext() {
        super();
    }

    public RealmModel getRealm() {
        return realm;
    }

    public KeycloakSession getKeycloakSession() {
        return keycloakSession;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    @SuppressWarnings("resource")
    public UserEntity findUserEntityById(String userId) {
        return getEntityManager().find(UserEntity.class, userId);
    }

    @SuppressWarnings("resource")
    public RoleEntity findRoleEntityById(String roleId) {
        return getEntityManager().find(RoleEntity.class, roleId);
    }

    @SuppressWarnings("resource")
    public GroupEntity findGroupEntityById(String groupId) {
        return getEntityManager().find(GroupEntity.class, groupId);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                init();
                try {
                    base.evaluate();
                } finally {
                    cleanup();
                }
            }
        };
    }

    private void init() {
        CryptoIntegration.init(UserAdapterTest.class.getClassLoader());
        Profile.defaults();

        UnitTestKeycloakSessionFactory sessionFactory = new UnitTestKeycloakSessionFactory();
        sessionFactory.init();

        keycloakSession = sessionFactory.create();
        Assert.assertNotNull(keycloakSession);

        JpaConnectionProvider provider = keycloakSession.getProvider(JpaConnectionProvider.class);
        Assert.assertNotNull(provider);

        entityManager = provider.getEntityManager();
        Assert.assertNotNull(entityManager);
        entityManager.getTransaction().begin();

        realm = createDefaultRealm();
    }

    private RealmModel createDefaultRealm() {
        RealmEntity realmEntity = new RealmEntity();
        realmEntity.setId("jpaTest");
        realmEntity.setName("jpaTest");
        return new RealmAdapter(keycloakSession, entityManager, realmEntity);
    }

    private void cleanup() {
        entityManager.getTransaction().commit();
        entityManager.close();
        keycloakSession.close();
    }
}
