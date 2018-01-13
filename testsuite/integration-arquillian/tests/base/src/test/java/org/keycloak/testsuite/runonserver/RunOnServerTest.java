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

package org.keycloak.testsuite.runonserver;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * This checks running code on the server for tests works and is not a test of the actual server
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RunOnServerTest extends AbstractKeycloakTest {

    @Deployment
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(RunOnServerTest.class);
    }

    @Test
    public void runOnServerString() throws IOException {
        String string = testingClient.server().fetch(session -> "Hello world!", String.class);
        assertEquals("Hello world!", string);
    }

    @Test
    public void runOnServerRep() throws IOException {
        final String realmName = "master";

        RealmRepresentation realmRep = testingClient.server().fetch(session -> {
            RealmModel master = session.realms().getRealm(realmName);
            return ModelToRepresentation.toRepresentation(master, true);
        }, RealmRepresentation.class);

        assertEquals(realmName, realmRep.getRealm());
    }

    @Test
    public void runOnServerHelpers() throws IOException {
        RealmRepresentation realmRep = testingClient.server().fetch(RunHelpers.internalRealm());
        assertEquals("master", realmRep.getRealm());
    }

    @Test
    public void runOnServerNoResponse() throws IOException {
        testingClient.server().run(session -> System.out.println("Hello world!"));
    }

    @Test
    public void runOnServerAssertOnServer() throws IOException {
        try {
            testingClient.server().run(session -> assertEquals("foo", "bar"));
            fail("Expected exception");
        } catch (ComparisonFailure e) {
            assertEquals("expected:<[foo]> but was:<[bar]>", e.getMessage());
        }
    }

    @Test
    public void runOnServerExceptionOnServer() throws IOException {
        try {
            testingClient.server().run(session -> {
                throw new ModelException("Something went wrong");
            });
            fail("Expected exception");
        } catch (RunOnServerException e) {
            assertTrue(e.getCause() instanceof ModelException);
            assertEquals("Something went wrong", e.getCause().getMessage());
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

}
