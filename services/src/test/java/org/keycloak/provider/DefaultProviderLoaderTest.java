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

package org.keycloak.provider;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

/**
 * Verifies the dual-mode discovery in {@link DefaultProviderLoader}: factories installed
 * into the build-time {@link GeneratedProviderRegistry} are combined with
 * {@link java.util.ServiceLoader}-discovered factories, with deduplication by factory class.
 *
 * The registry is populated in {@link #installRegistry()} as the Quarkus deployment
 * processor would at build time. ServiceLoader picks up factories from
 * {@code src/test/resources/META-INF/services/org.keycloak.provider.DefaultProviderLoaderTest$TestProviderFactory}.
 */
public class DefaultProviderLoaderTest {

    @BeforeClass
    public static void installRegistry() {
        GeneratedProviderRegistry.install(Set.of(
                BothFactory.class,
                RegistryOnlyFactory.class,
                UnrelatedFactory.class));
    }

    @Test
    public void loadCombinesRegistryAndServiceLoaderWithDedup() {
        DefaultProviderLoader loader = new DefaultProviderLoader(
                KeycloakDeploymentInfo.create().services(),
                getClass().getClassLoader());

        List<ProviderFactory> factories = loader.load(new TestSpi());
        Set<Class<?>> classes = factories.stream().map(Object::getClass).collect(Collectors.toSet());

        Assertions.assertTrue(classes.contains(RegistryOnlyFactory.class),
                "factory listed only in the generated registry must be loaded");
        Assertions.assertTrue(classes.contains(ServiceLoaderOnlyFactory.class),
                "factory listed only in META-INF/services must be loaded");
        Assertions.assertTrue(classes.contains(BothFactory.class),
                "factory listed in both sources must be loaded");
        Assertions.assertFalse(classes.contains(UnrelatedFactory.class),
                "factory implementing a different SPI must be filtered out");

        long bothCount = factories.stream().filter(f -> f.getClass() == BothFactory.class).count();
        Assertions.assertEquals(1, bothCount,
                "factory listed in registry AND META-INF/services must be deduplicated by class");
    }

    public interface TestProvider extends Provider {
    }

    public interface TestProviderFactory extends ProviderFactory<TestProvider> {
    }

    public static final class TestSpi implements Spi {
        @Override public boolean isInternal() { return true; }
        @Override public String getName() { return "default-provider-loader-test"; }
        @Override public Class<? extends Provider> getProviderClass() { return TestProvider.class; }
        @Override public Class<? extends ProviderFactory> getProviderFactoryClass() { return TestProviderFactory.class; }
    }

    /** Listed only in the generated registry resource. */
    public static final class RegistryOnlyFactory implements TestProviderFactory {
        @Override public TestProvider create(KeycloakSession session) { return null; }
        @Override public void init(Config.Scope config) { }
        @Override public void postInit(KeycloakSessionFactory factory) { }
        @Override public void close() { }
        @Override public String getId() { return "registry-only"; }
    }

    /** Listed only in META-INF/services. */
    public static final class ServiceLoaderOnlyFactory implements TestProviderFactory {
        @Override public TestProvider create(KeycloakSession session) { return null; }
        @Override public void init(Config.Scope config) { }
        @Override public void postInit(KeycloakSessionFactory factory) { }
        @Override public void close() { }
        @Override public String getId() { return "service-loader-only"; }
    }

    /** Listed in both sources — must be returned exactly once. */
    public static final class BothFactory implements TestProviderFactory {
        @Override public TestProvider create(KeycloakSession session) { return null; }
        @Override public void init(Config.Scope config) { }
        @Override public void postInit(KeycloakSessionFactory factory) { }
        @Override public void close() { }
        @Override public String getId() { return "both"; }
    }

    public interface OtherProvider extends Provider {
    }

    public interface OtherProviderFactory extends ProviderFactory<OtherProvider> {
    }

    /** In the registry but for a different SPI — must be filtered out when loading TestSpi. */
    public static final class UnrelatedFactory implements OtherProviderFactory {
        @Override public OtherProvider create(KeycloakSession session) { return null; }
        @Override public void init(Config.Scope config) { }
        @Override public void postInit(KeycloakSessionFactory factory) { }
        @Override public void close() { }
        @Override public String getId() { return "unrelated"; }
    }
}
