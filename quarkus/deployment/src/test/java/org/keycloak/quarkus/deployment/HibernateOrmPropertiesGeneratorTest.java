/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.quarkus.deployment;

import java.util.Map;

import org.keycloak.quarkus.runtime.configuration.HibernateOrmProperties;
import org.keycloak.quarkus.runtime.configuration.HibernateOrmProperties.HibernateOrmProperty;
import org.keycloak.quarkus.runtime.configuration.HibernateOrmPropertiesGenerator;

import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link HibernateOrmPropertiesGenerator} with both modules of the Hibernate ORM extension on the class path, as when the
 * runtime module is built.
 */
public class HibernateOrmPropertiesGeneratorTest {

    private static final Map<String, HibernateOrmProperty> DISCOVERED = HibernateOrmPropertiesGenerator.discover(Thread.currentThread().getContextClassLoader());

    @Test
    public void collectsTheBuildTimeAndTheRunTimeProperties() {
        assertTrue(DISCOVERED.values().stream().anyMatch(HibernateOrmProperty::buildTime), DISCOVERED.toString());
        assertTrue(DISCOVERED.values().stream().anyMatch(property -> !property.buildTime()), DISCOVERED.toString());
        assertTrue(DISCOVERED.keySet().stream().allMatch(name -> name.startsWith(HibernateOrmProperties.PREFIX + ".")), DISCOVERED.toString());
        assertTrue(DISCOVERED.keySet().stream().noneMatch(name -> name.contains("*")), DISCOVERED.toString());

        // build time properties of a persistence unit and of the extension as a whole
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.query.query-plan-cache-max-size", true, true, Integer.class), DISCOVERED.get("quarkus.hibernate-orm.query.query-plan-cache-max-size"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.jdbc.statement-batch-size", true, true, Integer.class), DISCOVERED.get("quarkus.hibernate-orm.jdbc.statement-batch-size"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.dialect", true, true, String.class), DISCOVERED.get("quarkus.hibernate-orm.dialect"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.packages", true, true, String.class), DISCOVERED.get("quarkus.hibernate-orm.packages"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.second-level-caching-enabled", true, true, Boolean.class), DISCOVERED.get("quarkus.hibernate-orm.second-level-caching-enabled"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.enabled", true, false, Boolean.class), DISCOVERED.get("quarkus.hibernate-orm.enabled"));
        // run time properties
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.log.sql", false, true, Boolean.class), DISCOVERED.get("quarkus.hibernate-orm.log.sql"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.schema-management.strategy", false, true, String.class), DISCOVERED.get("quarkus.hibernate-orm.schema-management.strategy"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.log.queries-slower-than-ms", false, true, Long.class), DISCOVERED.get("quarkus.hibernate-orm.log.queries-slower-than-ms"));
        // no map keys, no deprecated properties
        assertFalse(DISCOVERED.containsKey("quarkus.hibernate-orm.unsupported-properties.*"));
        assertFalse(DISCOVERED.containsKey("quarkus.hibernate-orm.database.generation"));
        assertFalse(DISCOVERED.containsKey("quarkus.hibernate-orm.blocking"));
    }

    @Test
    public void theGeneratedPropertiesAreTheDiscoveredOnes() {
        // the resource generated into the runtime module
        assertEquals(DISCOVERED, HibernateOrmProperties.getProperties());
    }

    @Test
    public void collectsThePropertiesOfTheConfigRootsOfTheExtension() {
        // the config roots of the extension
        assertEquals(DISCOVERED, HibernateOrmPropertiesGenerator.discover(new ClassLoader(HibernateOrmConfig.class.getClassLoader()) {
        }));
        assertEquals("io.quarkus.hibernate.orm.deployment.HibernateOrmConfig", HibernateOrmConfig.class.getName());
        assertEquals("io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig", HibernateOrmRuntimeConfig.class.getName());
    }
}
