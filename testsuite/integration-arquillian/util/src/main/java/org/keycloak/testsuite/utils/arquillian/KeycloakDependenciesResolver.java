/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.utils.arquillian;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependencies;

/**
 * @author mhajas
 */
public class KeycloakDependenciesResolver {

    private static final Map<String, File[]> dependencies = new HashMap<>();

    protected static final Logger log = org.jboss.logging.Logger.getLogger(KeycloakDependenciesResolver.class);

    public static File[] resolveDependencies(String canonicalForm) {
        if (dependencies.containsKey(canonicalForm)) {
            return dependencies.get(canonicalForm);
        }

        log.info("Resolving " + canonicalForm + "'s dependencies");
        PomEquippedResolveStage resolver = Maven.configureResolverViaPlugin();

        File[] files = resolver.addDependency(MavenDependencies.createDependency(canonicalForm, ScopeType.COMPILE, false))
                .resolve().withTransitivity().asFile();

        dependencies.put(canonicalForm, files);

        log.info("Resolving dependencies is finished with " + files.length + " files");

        return dependencies.get(canonicalForm);
    }
}
