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
package org.keycloak.tests.migration;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Test;

/**
 * Tests that we can import json file from previous version.  MigrationTest only tests DB.
 */
@KeycloakIntegrationTest
public class JsonFileImport483MigrationTest extends AbstractJsonFileImportMigrationTest {

    @InjectRealm(ref = "migration", fromJson = "migration-realm-4.8.3.Final-Migration.json")
    ManagedRealm migrationManagedRealm;

    @InjectRealm(ref = "migration2", fromJson = "migration-realm-4.8.3.Final-Migration2.json")
    ManagedRealm migration2ManagedRealm;

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm masterManagedRealm;


    @TestSetup
    public void setupDependencies() {
        oauthClient = createOAuthClient();
    }

    @Test
    void migration4_8_3Test() {
        checkRealmsImported();
        testMigrationTo5_x();
        testMigrationTo6_x();
        testMigrationTo7_x(true);
        testMigrationTo8_x();
        testMigrationTo9_x();
        testMigrationTo12_x(true);
        testMigrationTo18_x();
        testMigrationTo20_x();
        testMigrationTo21_x();
        testMigrationTo22_x();
        testMigrationTo23_x(false);
        testMigrationTo24_x();
        testMigrationTo25_0_0();
        testMigrationTo26_0_0(false);
        testMigrationTo26_3_0();
    }

}
