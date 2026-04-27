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

package org.keycloak.quarkus.runtime.storage.database.liquibase.database;

import liquibase.database.core.MSSQLDatabase;

public class CustomMSSQLDatabase extends MSSQLDatabase {

    private static String ENGINE_EDITION;

    @Override
    public String getEngineEdition() {
        // no need to query engine edition every time
        // it should be safe to update without any synchronization code as liquibase runs from a single thread
        if (ENGINE_EDITION == null) {
            return ENGINE_EDITION = super.getEngineEdition();
        }
        return ENGINE_EDITION;
    }
}
