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
package org.keycloak.examples.storage.user;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JndiEntityManagerLookup;
import org.keycloak.connections.jpa.JpaKeycloakTransaction;
import org.keycloak.connections.jpa.entityprovider.ProxyClassLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.storage.UserStorageProviderFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ExampleUserStorageProviderFactory implements UserStorageProviderFactory<ExampleUserStorageProvider> {

    EntityManagerFactory emf = null;

    @Override
    public ExampleUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        EntityManager em = emf.createEntityManager();
        session.getTransactionManager().enlist(new JpaKeycloakTransaction(em));
        return new ExampleUserStorageProvider(em, model, session);
    }

    @Override
    public String getId() {
        return "example-user-storage";
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //emf = Persistence.createEntityManagerFactory("user-storage-jpa-example");
        emf = createEntityManagerFactory("user-storage-jpa-example");
        //emf = Bootstrap.getEntityManagerFactoryBuilder()

    }

    public static EntityManagerFactory createEntityManagerFactory(String unitName) {
        PersistenceXmlParser parser = new PersistenceXmlParser(new ClassLoaderServiceImpl(ExampleUserStorageProviderFactory.class.getClassLoader()), PersistenceUnitTransactionType.RESOURCE_LOCAL);
        List<ParsedPersistenceXmlDescriptor> persistenceUnits = parser.doResolve(new HashMap());
        for (ParsedPersistenceXmlDescriptor persistenceUnit : persistenceUnits) {
            if (persistenceUnit.getName().equals(unitName)) {
                return Bootstrap.getEntityManagerFactoryBuilder(persistenceUnit, new HashMap(), ExampleUserStorageProviderFactory.class.getClassLoader()).build();
            }
        }
        throw new RuntimeException("Persistence unit '" + unitName + "' not found");
    }


    @Override
    public void close() {
        emf.close();

    }
}
