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

package org.keycloak.protocol.oauth2.cimd.clientpolicy.executor;

import java.lang.reflect.Proxy;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;

import org.junit.Assert;
import org.junit.Test;

public class AbstractClientIdMetadataDocumentExecutorFactoryTest {

    @Test
    public void shouldRejectUnknownCimdProvider() {
        ClientIdMetadataDocumentExecutorFactory factory = new ClientIdMetadataDocumentExecutorFactory();
        factory.init(new TestConfig("unknown-cimd"));

        KeycloakSessionFactory sessionFactory = (KeycloakSessionFactory) Proxy.newProxyInstance(
                KeycloakSessionFactory.class.getClassLoader(),
                new Class<?>[] { KeycloakSessionFactory.class },
                (proxy, method, args) -> null);

        IllegalStateException exception = Assert.assertThrows(IllegalStateException.class,
                () -> factory.postInit(sessionFactory));
        Assert.assertEquals("Client ID Metadata Document provider 'unknown-cimd' configured with 'cimd-provider-name' was not found",
                exception.getMessage());
    }

    private static class TestConfig extends Config.AbstractScope {

        private final String providerName;

        private TestConfig(String providerName) {
            this.providerName = providerName;
        }

        @Override
        public String get(String key) {
            return AbstractClientIdMetadataDocumentExecutorFactory.CONFIG_CIMD_PROVIDER_NAME.equals(key) ? providerName : null;
        }

        @Override
        public Config.Scope scope(String... scope) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Set<String> getPropertyNames() {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Config.Scope root() {
            throw new UnsupportedOperationException("not implemented");
        }
    }
}
