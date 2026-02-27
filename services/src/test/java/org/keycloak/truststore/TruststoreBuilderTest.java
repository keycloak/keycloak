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

package org.keycloak.truststore;

import java.io.File;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TruststoreBuilderTest {

    @Test
    public void testMergedTrustStore() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");

        KeyStore storeWithoutDefaults = TruststoreBuilder.createMergedTruststore(new String[] { url.getPath() }, false);
        ArrayList<String> storeWithoutDefaultsAliases = Collections.list(storeWithoutDefaults.aliases());
        assertEquals(2, storeWithoutDefaultsAliases.size());

        KeyStore storeWithDefaults = TruststoreBuilder.createMergedTruststore(new String[] { url.getPath() }, true);
        ArrayList<String> storeWithDefaultsAliases = Collections.list(storeWithDefaults.aliases());
        int certs = storeWithDefaultsAliases.size();
        assertTrue(certs > 2);
        assertTrue(storeWithDefaultsAliases.containsAll(storeWithoutDefaultsAliases));

        // saving / loading should provide the certs even without a password
        File saved = TruststoreBuilder.saveTruststore(storeWithDefaults, "target", null);

        KeyStore savedLoaded = TruststoreBuilder.loadStore(saved.getAbsolutePath(), TruststoreBuilder.PKCS12, null);
        assertEquals(certs, Collections.list(savedLoaded.aliases()).size());
    }

    @Test
    public void testMergedTrustStoreFromDirectory() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");

        KeyStore storeWithoutDefaults = TruststoreBuilder
                .createMergedTruststore(new String[] { new File(url.getPath()).getParent() }, false);
        ArrayList<String> storeWithoutDefaultsAliases = Collections.list(storeWithoutDefaults.aliases());
        assertEquals(2, storeWithoutDefaultsAliases.size());
    }

    @Test
    public void testFailsWithInvalidFile() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/invalid");

        assertThrows(RuntimeException.class, () -> TruststoreBuilder
                .createMergedTruststore(new String[] { new File(url.getPath()).getAbsolutePath() }, false));
    }

    @Test
    public void testKubernetesCaAndServiceCaIncludedWhenFilesExist() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        String existingFile = new File(url.getPath()).getAbsolutePath();

        List<String> trustStores = new ArrayList<>();
        TruststoreBuilder.includeKubernetesTrustStorePaths(trustStores, existingFile, existingFile);

        assertEquals(2, trustStores.size());
        assertEquals(existingFile, trustStores.get(0));
        assertEquals(existingFile, trustStores.get(1));
    }

    @Test
    public void testKubernetesCaAndServiceCaNotIncludedWhenFilesDoNotExist() throws Exception {
        List<String> trustStores = new ArrayList<>();
        TruststoreBuilder.includeKubernetesTrustStorePaths(trustStores, "/non/existing/ca.crt", "/non/existing/service-ca.crt");

        assertTrue(trustStores.isEmpty());
    }

    @Test
    public void testOnlyKubernetesCaIncludedWhenServiceCaDoesNotExist() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        String existingFile = new File(url.getPath()).getAbsolutePath();

        List<String> trustStores = new ArrayList<>();
        TruststoreBuilder.includeKubernetesTrustStorePaths(trustStores, existingFile, "/non/existing/service-ca.crt");

        assertEquals(1, trustStores.size());
        assertEquals(existingFile, trustStores.get(0));
    }

    @Test
    public void testKubernetesCaPreservesExistingTrustStoreEntries() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        String existingFile = new File(url.getPath()).getAbsolutePath();

        List<String> trustStores = new ArrayList<>();
        trustStores.add("/some/existing/truststore.p12");
        TruststoreBuilder.includeKubernetesTrustStorePaths(trustStores, existingFile, "/non/existing/service-ca.crt");

        assertEquals(2, trustStores.size());
        assertEquals("/some/existing/truststore.p12", trustStores.get(0));
        assertEquals(existingFile, trustStores.get(1));
    }

    @Test
    public void testKubernetesCaIgnoresDirectories() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        String directory = new File(url.getPath()).getParent();

        List<String> trustStores = new ArrayList<>();
        TruststoreBuilder.includeKubernetesTrustStorePaths(trustStores, directory, "/non/existing/service-ca.crt");

        assertTrue(trustStores.isEmpty());
    }

}
