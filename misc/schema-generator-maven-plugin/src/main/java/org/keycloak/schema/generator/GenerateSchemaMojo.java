package org.keycloak.schema.generator;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.representations.migration.MigrationManifest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate-schema", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class GenerateSchemaMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "changelogFile", defaultValue = "META-INF/jpa-changelog-master.xml")
    private String changelogFile;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.outputDirectory}/META-INF/schema")
    private File outputDirectory;

    @Parameter(property = "dialects")
    private List<String> dialects;

    /**
     * Optional manifest of a previous release. When set, the build fails if a change-set in that manifest was
     * removed or its checksum changed, as such edits are never applied to existing databases.
     */
    @Parameter(property = "baselineManifest")
    private File baselineManifest;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Generating Keycloak schema migration scripts and manifest from " + changelogFile);
        try {
            System.setProperty("liquibase.duplicateFileMode", "WARN");

            ClassLoader classLoader = getProjectClassLoader();
            SchemaGenerator generator = new SchemaGenerator(changelogFile, outputDirectory, dialects, classLoader);
            MigrationManifest manifest = generator.generate();
            getLog().info("Generated " + manifest.getChangeSets().size() + " change sets in manifest at " + new File(outputDirectory, "migration-manifest.json").getAbsolutePath());

            if (baselineManifest != null) {
                MigrationManifest baseline = new ObjectMapper().readValue(baselineManifest, MigrationManifest.class);
                List<String> violations = ManifestDriftChecker.check(baseline, manifest);
                if (!violations.isEmpty()) {
                    violations.forEach(v -> getLog().error(v));
                    throw new MojoFailureException("Migration manifest is not compatible with baseline " + baselineManifest);
                }
            }
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            getLog().error("Failed to generate schema migration scripts", e);
            throw new MojoExecutionException("Schema generation failed: " + e.getMessage(), e);
        }
    }

    private ClassLoader getProjectClassLoader() throws Exception {
        Set<URL> urls = new LinkedHashSet<>();
        File classesDir = new File(project.getBuild().getOutputDirectory());
        File changelogInClasses = new File(classesDir, changelogFile);

        if (!changelogInClasses.exists()) {
            File resourcesDir = new File(project.getBasedir(), "src/main/resources");
            if (resourcesDir.exists()) {
                urls.add(resourcesDir.toURI().toURL());
            }
        }

        List<String> classpathElements = project.getCompileClasspathElements();
        for (String element : classpathElements) {
            File file = new File(element);
            if (file.exists()) {
                urls.add(file.toURI().toURL());
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
    }
}
