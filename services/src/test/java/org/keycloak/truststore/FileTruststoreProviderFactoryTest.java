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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.common.enums.HostnameVerificationPolicy;
import org.keycloak.utils.ScopeUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FileTruststoreProviderFactoryTest {

    @Test
    public void testFallbackToSystemTruststore() throws IOException {
        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        factory.init(ScopeUtil.createScope(new HashMap<>()));
        TruststoreProvider provider = factory.create(null);
        assertNotNull(provider.getTruststore());
        assertEquals(HostnameVerificationPolicy.DEFAULT, provider.getPolicy());
    }

    @Test
    public void testFallbackToSystemTruststoreWithHostnameVerification() throws IOException {
        Map<String, String> values = new HashMap<>();
        values.put(FileTruststoreProviderFactory.HOSTNAME_VERIFICATION_POLICY,
                HostnameVerificationPolicy.ANY.name());
        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        factory.init(ScopeUtil.createScope(values));
        TruststoreProvider provider = factory.create(null);
        assertNotNull(provider.getTruststore());
        assertEquals(HostnameVerificationPolicy.ANY, provider.getPolicy());
    }

}
