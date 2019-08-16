/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.jpa.updater.liquibase;

import java.util.HashSet;
import java.util.Set;
import liquibase.database.core.MariaDBDatabase;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UpdatedMariaDBDatabase extends MariaDBDatabase {

    private static final Set<String> RESERVED_WORDS = new HashSet<>();

    @Override
    public boolean isReservedWord(String string) {
        return super.isReservedWord(string) || RESERVED_WORDS.contains(string.toUpperCase());
    }

    @Override
    public int getPriority() {
        return super.getPriority() + 1; // Always take precedence over factory MariaDBDatabase
    }

    static {
        RESERVED_WORDS.add("PERIOD");
    }
}
