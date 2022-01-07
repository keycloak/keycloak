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

package org.keycloak.quarkus.runtime.storage.database.liquibase;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import liquibase.database.DatabaseFactory;
import liquibase.exception.ServiceNotFoundException;

import liquibase.database.Database;
import liquibase.logging.Logger;
import liquibase.servicelocator.DefaultPackageScanClassResolver;
import liquibase.servicelocator.ServiceLocator;

public class FastServiceLocator extends ServiceLocator {

    private final Map<String, List<String>> services;

    public FastServiceLocator(Map<String, List<String>> services) {
        super(new DefaultPackageScanClassResolver() {
            @Override
            public Set<Class<?>> findImplementations(Class parent, String... packageNames) {
                List<String> found = services.get(parent.getName());

                if (found == null) {
                    return super.findImplementations(parent, packageNames);
                }

                Set<Class<?>> ret = new HashSet<>();
                for (String i : found) {
                    try {
                        ret.add(Class.forName(i, false, Thread.currentThread().getContextClassLoader()));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                return ret;
            }
        });

        if (!System.getProperties().containsKey("liquibase.scan.packages")) {
            if (getPackages().remove("liquibase.core")) {
                addPackageToScan("liquibase.core.xml");
            }

            if (getPackages().remove("liquibase.parser")) {
                addPackageToScan("liquibase.parser.core.xml");
            }

            if (getPackages().remove("liquibase.serializer")) {
                addPackageToScan("liquibase.serializer.core.xml");
            }

            getPackages().remove("liquibase.ext");
            getPackages().remove("liquibase.sdk");
        }

        // we only need XML parsers
        getPackages().remove("liquibase.parser.core.yaml");
        getPackages().remove("liquibase.serializer.core.yaml");
        getPackages().remove("liquibase.parser.core.json");
        getPackages().remove("liquibase.serializer.core.json");

        // register only the implementations related to the chosen db
        for (String databaseImpl : services.get(Database.class.getName())) {
            try {
                register((Database) getClass().getClassLoader().loadClass(databaseImpl).getDeclaredConstructor().newInstance());
            } catch (Exception cause) {
                throw new RuntimeException("Failed to load database implementation", cause);
            }
        }

        this.services = services;
    }

    @Override
    public Object newInstance(Class requiredInterface) throws ServiceNotFoundException {
        if (Logger.class.equals(requiredInterface)) {
            return new KeycloakLogger();
        }
        return super.newInstance(requiredInterface);
    }

    @Override
    public <T> Class<? extends T>[] findClasses(Class<T> requiredInterface) throws ServiceNotFoundException {
        List<String> found = services.get(requiredInterface.getName());

        if (found == null) {
            return super.findClasses(requiredInterface);
        }

        Set<Class<?>> ret = new HashSet<>();
        for (String i : found) {
            try {
                ret.add(Class.forName(i, false, Thread.currentThread().getContextClassLoader()));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return ret.toArray(new Class[ret.size()]);
    }

    public void register(Database database) {
        DatabaseFactory.getInstance().register(database);
    }
}
