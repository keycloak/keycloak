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

package org.keycloak.it.jpa.migration;

import java.util.List;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

public class MigrationTestJpaEntityProvider implements JpaEntityProvider {

    @Override
    public List<Class<?>> getEntities() {
        return List.of(MigrationTestEntity.class);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/migration-test-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return MigrationTestJpaEntityProviderFactory.ID;
    }

    @Override
    public void close() {
    }
}
