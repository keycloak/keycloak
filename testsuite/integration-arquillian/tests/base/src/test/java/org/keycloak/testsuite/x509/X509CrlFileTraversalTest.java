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

package org.keycloak.testsuite.x509;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.util.ContainerAssume;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class X509CrlFileTraversalTest extends AbstractTestRealmKeycloakTest {

    private static final String CONTAINED_CRL = "contained-test.crl";
    private static final String TRAVERSAL_TARGET = "traversal-target.crl";
    private static final String TRAVERSAL_PATH = "../" + TRAVERSAL_TARGET;

    private Path containedCrl;
    private Path outsideCrl;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void createCrlFiles() throws Exception {
        // Not possible to test file CRL on undertow at this moment - jboss config dir doesn't exist
        ContainerAssume.assumeNotAuthServerUndertow();
        Path configDir = Paths.get(testingClient.server().fetch(CrlFileLoaderProbe.configDir(), String.class));
        byte[] crl = CrlGenerator.generateValidCrl();
        containedCrl = Files.write(configDir.resolve(CONTAINED_CRL), crl);
        outsideCrl = Files.write(configDir.getParent().resolve(TRAVERSAL_TARGET), crl);
    }

    @After
    public void deleteCrlFiles() throws Exception {
        if (containedCrl != null) {
            Files.deleteIfExists(containedCrl);
        }
        if (outsideCrl != null) {
            Files.deleteIfExists(outsideCrl);
        }
    }

    @Test
    public void crlRelativePathStayingInConfigDirIsLoaded() {
        String result = testingClient.server().fetch(CrlFileLoaderProbe.loadCrl(CONTAINED_CRL), String.class);
        Assert.assertEquals("A CRL located inside the configuration directory must be loaded", "LOADED:1", result);
    }

    @Test
    public void crlRelativePathEscapingConfigDirIsRejected() {
        String result = testingClient.server().fetch(CrlFileLoaderProbe.loadCrl(TRAVERSAL_PATH), String.class);
        Assert.assertFalse("Path traversal succeeded: a CRL file outside the configuration directory was read via " + TRAVERSAL_PATH + " -> " + result,
                result.startsWith("LOADED"));
        Assert.assertTrue("Expected the traversal path to be rejected by containment, but got " + result,
                result.contains("Unable to load CRL"));
    }
}
