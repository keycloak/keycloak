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

package org.keycloak.theme;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.encoding.ResourceEncodingProvider;
import org.keycloak.encoding.ResourceEncodingProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.InvalidationHandler;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.theme.freemarker.DefaultFreeMarkerProvider;
import org.keycloak.theme.freemarker.DefaultFreeMarkerProviderFactory;
import org.keycloak.theme.freemarker.FreeMarkerProvider;
import org.keycloak.theme.freemarker.FreeMarkerProviderFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// See https://github.com/keycloak/keycloak/issues/51066
public class DefaultThemeManagerFactoryTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void clearCacheRemovesCachedTemplates() throws Exception {
        File themeDir = temporaryFolder.newFolder("login");
        File templateFile = new File(themeDir, "test.ftl");
        Files.write(templateFile.toPath(), "VERSION-ONE".getBytes(StandardCharsets.UTF_8));

        FolderTheme theme = new FolderTheme(themeDir, "test", Theme.Type.LOGIN);
        DefaultFreeMarkerProviderFactory factory = new DefaultFreeMarkerProviderFactory();
        DefaultFreeMarkerProvider provider = factory.create(null);

        assertEquals("VERSION-ONE", provider.processTemplate(new HashMap<>(), "test.ftl", theme));

        // the template is cached, so a change on disk is not picked up yet
        Files.write(templateFile.toPath(), "VERSION-TWO".getBytes(StandardCharsets.UTF_8));
        assertEquals("VERSION-ONE", provider.processTemplate(new HashMap<>(), "test.ftl", theme));

        factory.clearCache();

        assertEquals("VERSION-TWO", provider.processTemplate(new HashMap<>(), "test.ftl", theme));
    }

    @Test
    public void clearCacheDelegatesToFreeMarkerAndResourceEncodingFactories() {
        AtomicBoolean freeMarkerCleared = new AtomicBoolean();
        AtomicBoolean gzipCleared = new AtomicBoolean();

        FreeMarkerProviderFactory freeMarkerProviderFactory = new FreeMarkerProviderFactory() {
            @Override
            public FreeMarkerProvider create(KeycloakSession session) {
                return null;
            }

            @Override
            public void init(Config.Scope config) {
            }

            @Override
            public void postInit(KeycloakSessionFactory factory) {
            }

            @Override
            public void close() {
            }

            @Override
            public void clearCache() {
                freeMarkerCleared.set(true);
            }

            @Override
            public String getId() {
                return "default";
            }
        };

        ResourceEncodingProviderFactory resourceEncodingProviderFactory = new ResourceEncodingProviderFactory() {
            @Override
            public ResourceEncodingProvider create(KeycloakSession session) {
                return null;
            }

            @Override
            public boolean encodeContentType(String contentType) {
                return true;
            }

            @Override
            public void clearCache() {
                gzipCleared.set(true);
            }

            @Override
            public String getId() {
                return "gzip";
            }
        };

        DefaultThemeManagerFactory themeManagerFactory = new DefaultThemeManagerFactory();
        themeManagerFactory.postInit(new StubSessionFactory(freeMarkerProviderFactory, resourceEncodingProviderFactory));

        themeManagerFactory.clearCache();

        assertTrue(freeMarkerCleared.get());
        assertTrue(gzipCleared.get());
    }

    private static class StubSessionFactory implements KeycloakSessionFactory {

        private final FreeMarkerProviderFactory freeMarkerProviderFactory;
        private final ResourceEncodingProviderFactory resourceEncodingProviderFactory;

        StubSessionFactory(FreeMarkerProviderFactory freeMarkerProviderFactory, ResourceEncodingProviderFactory resourceEncodingProviderFactory) {
            this.freeMarkerProviderFactory = freeMarkerProviderFactory;
            this.resourceEncodingProviderFactory = resourceEncodingProviderFactory;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz) {
            return clazz == FreeMarkerProvider.class ? (ProviderFactory<T>) freeMarkerProviderFactory : null;
        }

        @Override
        public <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz, String id) { throw new UnsupportedOperationException(); }

        @Override
        public <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz, String realmId, String componentId, Function<KeycloakSessionFactory, ComponentModel> modelGetter) { throw new UnsupportedOperationException(); }

        @Override
        @SuppressWarnings("unchecked")
        public Stream<ProviderFactory> getProviderFactoriesStream(Class<? extends Provider> clazz) {
            return clazz == ResourceEncodingProvider.class ? Stream.of(resourceEncodingProviderFactory) : Stream.empty();
        }

        @Override
        public KeycloakSession create() { throw new UnsupportedOperationException(); }

        @Override
        public Set<Spi> getSpis() { throw new UnsupportedOperationException(); }

        @Override
        public Spi getSpi(Class<? extends Provider> providerClass) { throw new UnsupportedOperationException(); }

        @Override
        public long getServerStartupTimestamp() { throw new UnsupportedOperationException(); }

        @Override
        public void close() {
        }

        @Override
        public void register(ProviderEventListener listener) { throw new UnsupportedOperationException(); }

        @Override
        public void unregister(ProviderEventListener listener) { throw new UnsupportedOperationException(); }

        @Override
        public void publish(ProviderEvent event) { throw new UnsupportedOperationException(); }

        @Override
        public void invalidate(KeycloakSession session, InvalidationHandler.InvalidableObjectType type, Object... params) { throw new UnsupportedOperationException(); }
    }

}
