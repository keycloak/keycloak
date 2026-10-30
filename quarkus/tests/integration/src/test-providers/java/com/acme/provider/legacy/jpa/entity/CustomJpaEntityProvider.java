/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package com.acme.provider.legacy.jpa.entity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.keycloak.it.TestProvider;

import com.acme.provider.legacy.jpa.unused.IgnoredJarEntity;

public class CustomJpaEntityProvider implements TestProvider {

    @Override
    public Class[] getClasses() {
        return new Class[] { Realm.class, KeycloakItH2Dialect.class, UnlistedEntity.class, OrmMappedEntity.class, ValidationModeObserver.class };
    }

    @Override
    public Map<String, String> getManifestResources() {
        try {
            var jar = Files.createTempFile("keycloak-ignored-persistence-", ".jar");
            jar.toFile().deleteOnExit();
            String classFile = IgnoredJarEntity.class.getName().replace('.', '/') + ".class";
            try (var output = new JarOutputStream(Files.newOutputStream(jar));
                    var input = IgnoredJarEntity.class.getClassLoader().getResourceAsStream(classFile)) {
                output.putNextEntry(new JarEntry(classFile));
                input.transferTo(output);
                output.closeEntry();
            }
            return Map.of("persistence.xml", "persistence.xml", "orm.xml", "orm.xml", "custom.xml", "custom.xml",
                    jar.toString(), "ignored.jar");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
