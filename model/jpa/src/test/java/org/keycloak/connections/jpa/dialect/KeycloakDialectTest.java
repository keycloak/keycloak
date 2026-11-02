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

package org.keycloak.connections.jpa.dialect;

import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class KeycloakDialectTest {
    @Parameterized.Parameters(name = "{0}")
    public static List<Class<? extends Dialect>> dialects() {
        return List.of(KeycloakH2Dialect.class, KeycloakOracleDialect.class, KeycloakSQLServerDialect.class);
    }

    @Parameterized.Parameter
    public Class<? extends Dialect> dialect;

    @Test
    public void exposesPublicConstructorsUsedByHibernate() throws Exception {
        // Constructors are not inherited. Hibernate falls back to no-argument construction
        // when the dialect does not expose the metadata constructor, losing database capabilities.
        assertEquals(dialect, dialect.getConstructor(DialectResolutionInfo.class).getDeclaringClass());
        assertEquals(dialect, dialect.getConstructor().getDeclaringClass());
    }
}
