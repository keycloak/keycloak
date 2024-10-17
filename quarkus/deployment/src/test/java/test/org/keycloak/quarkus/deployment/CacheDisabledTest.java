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
package test.org.keycloak.quarkus.deployment;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.Instance;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.inject.Inject;
import org.keycloak.quarkus.runtime.storage.infinispan.CacheManagerFactory;
import org.keycloak.storage.DatastoreProviderFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CacheDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProvider(DatastoreProviderFactory.class, TestDatastoreFactory.class)
                .addClass(TestDatastore.class)
                .addClass(TestDatastoreFactory.class)
                .addAsResource("keycloak.conf", "META-INF/keycloak.conf"))
            .overrideConfigKey("quarkus.class-loading.removed-artifacts", "io.quarkus:quarkus-jdbc-oracle,io.quarkus:quarkus-jdbc-oracle-deployment") // config works a bit odd in unit tests, so this is to ensure we exclude Oracle to avoid ClassNotFound ex
            .overrideConfigKey("kc.spi-datastore-legacy-enabled","false")
            .overrideConfigKey("kc.spi-datastore-provider","test")
            .overrideConfigKey("kc.spi-connections-infinispan-quarkus-enabled","false")
            .overrideConfigKey("kc.spi-connections-infinispan-default-enabled","false")
            .overrideConfigKey("kc.spi-authentication-sessions-infinispan-enabled","false")
            .overrideConfigKey("kc.spi-user-sessions-infinispan-enabled","false")
            .overrideConfigKey("kc.spi-single-use-object-infinispan-enabled","false")
            .overrideConfigKey("kc.spi-login-failure-infinispan-enabled","false");

    @Inject
    Instance<CacheManagerFactory> cacheManagerFactory;

    @Test
    void testCacheDisabled() {
        assertFalse(cacheManagerFactory.isResolvable());
    }
}
