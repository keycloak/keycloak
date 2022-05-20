/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import org.junit.Assert;
import org.junit.Before;
import org.keycloak.representations.idm.RealmRepresentation;

import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractJsonFileImportMigrationTest extends AbstractMigrationTest {
    protected RealmRepresentation masterRep;
    protected String masterTestClientId;

    @Before
    public void beforeMigrationTest() {
        migrationRealm = adminClient.realms().realm(MIGRATION);
        migrationRealm2 = adminClient.realms().realm(MIGRATION2);
        masterRealm = adminClient.realms().realm(MASTER);
    }

    /**
     * The method will throw javax.ws.rs.NotFoundException in case the realm is not successfully imported
     */
    protected void checkRealmsImported() {
        assertThat(migrationRealm.toRepresentation().getRealm(), is(equalTo("Migration")));
        assertThat(migrationRealm2.toRepresentation().getRealm(), is(equalTo("Migration2")));
    }

    @Override
    protected void testMigrationTo13_0_0(boolean testRealmAttributesMigration) {
        testDefaultRoles(migrationRealm);

        testDefaultRolesNameWhenTaken();
        if (testRealmAttributesMigration) {
            testRealmAttributesMigration();
        }
    }
}
