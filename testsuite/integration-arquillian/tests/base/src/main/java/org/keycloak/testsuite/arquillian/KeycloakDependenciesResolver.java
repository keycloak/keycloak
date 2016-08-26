package org.keycloak.testsuite.arquillian;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependencies;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mhajas
 */
public class KeycloakDependenciesResolver {

    private static Map<String, File[]> dependencies = new HashMap<>();

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
