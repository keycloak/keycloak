/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.migration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;

/**
 * Tests that we can import json file from previous version.  MigrationTest only tests DB.
 */
public class JsonFileImport903MigrationTest extends AbstractJsonFileImportMigrationTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        Map<String, RealmRepresentation> reps = null;
        try {
            reps = ImportUtils.getRealmsFromStream(JsonSerialization.mapper, IOUtil.class.getResourceAsStream("/migration-test/migration-realm-9.0.3.json"));
            masterRep = reps.remove("master");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (RealmRepresentation rep : reps.values()) {
            testRealms.add(rep);
        }
    }

    @Test
    public void migration9_0_3Test() throws Exception {
        checkRealmsImported();
        testMigrationTo12_x(true);
        testMigrationTo18_x();
        testMigrationTo20_x();
        testMigrationTo21_x();
        testMigrationTo22_x();
        testMigrationTo23_x(false);
        testMigrationTo24_x(false);
        testMigrationTo25_0_0();
        testMigrationTo26_0_0(false);
        testMigrationTo26_3_0();
    }

}
