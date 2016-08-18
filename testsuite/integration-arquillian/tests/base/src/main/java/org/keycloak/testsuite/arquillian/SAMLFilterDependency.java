package org.keycloak.testsuite.arquillian;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PackagingType;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependency;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependencyExclusion;

import java.io.File;
import java.util.Collections;
import java.util.Set;

/**
 * @author mhajas
 */
public class SAMLFilterDependency implements MavenDependency {

    private static File[] files;

    protected final Logger log = org.jboss.logging.Logger.getLogger(this.getClass());

    @Override
    public Set<MavenDependencyExclusion> getExclusions() {
        return Collections.EMPTY_SET;
    }

    @Override
    public ScopeType getScope() {
        return ScopeType.COMPILE;
    }

    @Override
    public boolean isOptional() {
        return false;
    }

    @Override
    public PackagingType getPackaging() {
        return PackagingType.JAR;
    }

    @Override
    public PackagingType getType() {
        return PackagingType.JAR;
    }

    @Override
    public String getClassifier() {
        return null;
    }

    @Override
    public String getVersion() {
        return System.getProperty("project.version");
    }

    @Override
    public String getGroupId() {
        return "org.keycloak";
    }

    @Override
    public String getArtifactId() {
        return "keycloak-saml-servlet-filter-adapter";
    }

    @Override
    public String toCanonicalForm() {
        return getGroupId() + ":" + getArtifactId() +  ":" + getVersion();
    }

    private void resolve() {
        log.info("Resolving SAMLFilter dependencies");
        files = Maven.configureResolver().addDependency(this)
                .resolve().withTransitivity().asFile();
        log.info("Resolving dependencies is finished with " + files.length + " files");
    }

    public File[] getDependencies() {
        if (files == null) {
            resolve();
        }

        return files;
    }
}
