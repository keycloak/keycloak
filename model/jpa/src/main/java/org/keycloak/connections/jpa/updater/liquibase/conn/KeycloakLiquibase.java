/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.jpa.updater.liquibase.conn;

import liquibase.Liquibase;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.database.Database;
import liquibase.resource.ResourceAccessor;

/**
 * Custom subclass to expose protected liquibase API.
 */
public class KeycloakLiquibase extends Liquibase {

    public KeycloakLiquibase(String changeLogFile, ResourceAccessor resourceAccessor, Database database) {
        super(changeLogFile, resourceAccessor, database);
        this.setShowSummaryOutput(UpdateSummaryOutputEnum.LOG);
    }

    @Override
    public void resetServices() {
        // expose protected method for use without reflection
        super.resetServices();
    }
}
