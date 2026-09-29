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
package org.keycloak.quarkus.runtime.configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.quarkus.runtime.configuration.HibernateOrmProperties.HibernateOrmProperty;

import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * The properties that {@link HibernateOrmPropertiesGenerator} generates into this module.
 */
public class HibernateOrmPropertiesTest {

    @Test
    public void thePropertiesAreGeneratedIntoTheModule() {
        Map<String, HibernateOrmProperty> properties = HibernateOrmProperties.getProperties();

        // build time properties of a persistence unit and of the extension as a whole
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.query.query-plan-cache-max-size", true, true, Integer.class), properties.get("quarkus.hibernate-orm.query.query-plan-cache-max-size"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.jdbc.statement-batch-size", true, true, Integer.class), properties.get("quarkus.hibernate-orm.jdbc.statement-batch-size"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.dialect", true, true, String.class), properties.get("quarkus.hibernate-orm.dialect"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.packages", true, true, String.class), properties.get("quarkus.hibernate-orm.packages"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.enabled", true, false, Boolean.class), properties.get("quarkus.hibernate-orm.enabled"));
        // run time properties, with the types that Keycloak validates
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.log.sql", false, true, Boolean.class), properties.get("quarkus.hibernate-orm.log.sql"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.log.queries-slower-than-ms", false, true, Long.class), properties.get("quarkus.hibernate-orm.log.queries-slower-than-ms"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.schema-management.strategy", false, true, String.class), properties.get("quarkus.hibernate-orm.schema-management.strategy"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.database.default-schema", false, true, String.class), properties.get("quarkus.hibernate-orm.database.default-schema"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.request-scoped.enabled", false, false, Boolean.class), properties.get("quarkus.hibernate-orm.request-scoped.enabled"));
        assertEquals("log.sql", properties.get("quarkus.hibernate-orm.log.sql").suffix());
        // no map keys, no deprecated properties
        assertTrue(properties.toString(), properties.keySet().stream().noneMatch(name -> name.contains("*")));
        assertThat(properties, not(hasKey("quarkus.hibernate-orm.database.generation")));
        assertThat(properties, not(hasKey("quarkus.hibernate-orm.blocking")));
        assertTrue(properties.toString(), properties.keySet().stream().allMatch(name -> name.startsWith("quarkus.hibernate-orm.")));

        assertThrows(UnsupportedOperationException.class, () -> properties.remove("quarkus.hibernate-orm.enabled"));
    }

    @Test
    public void theGeneratorCollectsThePropertiesOfTheExtension() {
        // both modules of the extension are on the class path, as during the build
        Map<String, HibernateOrmProperty> discovered = HibernateOrmPropertiesGenerator.discover(Thread.currentThread().getContextClassLoader());

        assertEquals(HibernateOrmProperties.getProperties(), discovered);
        assertTrue(discovered.toString(), discovered.values().stream().anyMatch(HibernateOrmProperty::buildTime));
        assertTrue(discovered.toString(), discovered.values().stream().anyMatch(property -> !property.buildTime()));
        // the run time root alone
        Map<String, HibernateOrmProperty> runTime = HibernateOrmPropertiesGenerator.collect(List.of(HibernateOrmRuntimeConfig.class));
        assertTrue(runTime.toString(), runTime.values().stream().noneMatch(HibernateOrmProperty::buildTime));
        assertThat(runTime, hasKey("quarkus.hibernate-orm.log.sql"));
        assertThat(runTime, not(hasKey("quarkus.hibernate-orm.enabled")));
        runTime.forEach((name, property) -> assertEquals(property, discovered.get(name)));
    }

    @Test
    public void serializesAndParsesTheProperties() throws IOException {
        List<HibernateOrmProperty> all = new ArrayList<>(List.of(
                new HibernateOrmProperty("quarkus.hibernate-orm.log.sql", false, true, Boolean.class),
                new HibernateOrmProperty("quarkus.hibernate-orm.log.queries-slower-than-ms", false, true, Long.class),
                new HibernateOrmProperty("quarkus.hibernate-orm.enabled", true, false, Boolean.class),
                new HibernateOrmProperty("quarkus.hibernate-orm.query.query-plan-cache-max-size", true, true, Integer.class),
                new HibernateOrmProperty("quarkus.hibernate-orm.dialect", true, true, String.class)));

        byte[] serialized = HibernateOrmPropertiesGenerator.serialize(all);
        String text = new String(serialized, StandardCharsets.UTF_8);
        assertEquals(text, "# Auto-generated, DO NOT change this file: the Quarkus Hibernate ORM properties collected during the build\n"
                + "quarkus.hibernate-orm.dialect=build-time,unit,string\n"
                + "quarkus.hibernate-orm.enabled=build-time,global,boolean\n"
                + "quarkus.hibernate-orm.log.queries-slower-than-ms=run-time,unit,long\n"
                + "quarkus.hibernate-orm.log.sql=run-time,unit,boolean\n"
                + "quarkus.hibernate-orm.query.query-plan-cache-max-size=build-time,unit,integer\n", text);
        // sorted by name, independent of the input order
        Collections.reverse(all);
        assertEquals(text, new String(HibernateOrmPropertiesGenerator.serialize(all), StandardCharsets.UTF_8));

        Map<String, HibernateOrmProperty> parsed = HibernateOrmProperties.parse(new ByteArrayInputStream(serialized));
        assertThat(parsed.keySet(), containsInAnyOrder(all.stream().map(HibernateOrmProperty::name).toArray()));
        all.forEach(property -> assertEquals(property, parsed.get(property.name())));
    }

    @Test
    public void parsesTheResourceFormat() throws IOException {
        String recorded = "# comment\n"
                + "quarkus.hibernate-orm.enabled=build-time,global,boolean\n"
                + "quarkus.hibernate-orm.query.query-plan-cache-max-size = build-time, unit, integer\n"
                + "quarkus.hibernate-orm.jdbc.timezone=build-time,unit,string\n";

        Map<String, HibernateOrmProperty> parsed = HibernateOrmProperties.parse(new ByteArrayInputStream(recorded.getBytes(StandardCharsets.UTF_8)));

        assertEquals(3, parsed.size());
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.enabled", true, false, Boolean.class), parsed.get("quarkus.hibernate-orm.enabled"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.query.query-plan-cache-max-size", true, true, Integer.class), parsed.get("quarkus.hibernate-orm.query.query-plan-cache-max-size"));
        assertEquals(new HibernateOrmProperty("quarkus.hibernate-orm.jdbc.timezone", true, true, String.class), parsed.get("quarkus.hibernate-orm.jdbc.timezone"));
    }

    @Test
    public void rejectsInvalidRecords() {
        assertThrows(IllegalArgumentException.class, () -> HibernateOrmProperties.parse(new ByteArrayInputStream("quarkus.hibernate-orm.enabled=build-time".getBytes(StandardCharsets.UTF_8))));
        assertThrows(IllegalArgumentException.class, () -> HibernateOrmProperties.parse(new ByteArrayInputStream("quarkus.other=build-time,global,boolean".getBytes(StandardCharsets.UTF_8))));
        assertThrows(IllegalArgumentException.class, () -> new HibernateOrmProperty("quarkus.datasource.db-kind", true, false, String.class));
    }
}
