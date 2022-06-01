package org.keycloak.operator.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name = "keycloak-operator-codegen", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CodegenMojo extends AbstractMojo {

    @Parameter(property = "operator.codegen.target", defaultValue = "${basedir}/target/generated-sources/java")
    private File target;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Log log = getLog();

            ServerConfigGen serverConfigGen = new ServerConfigGen();

            serverConfigGen.generate(log, target);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoFailureException("Failed to generate code", e);
        }
    }
}
