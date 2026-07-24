package org.keycloak.db.compatibility.verifier;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

abstract class AbstractMojo extends org.apache.maven.plugin.AbstractMojo {

    final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(property = "db.verify.supportedFile", required = true)
    protected String supportedFile;

    @Parameter(property = "db.verify.unsupportedFile", required = true)
    protected String unsupportedFile;

    @Parameter(property = "db.verify.skip", defaultValue = "false")
    protected boolean skip;

    ClassLoader classLoader() throws DependencyResolutionRequiredException, MalformedURLException {
        List<String> elements = project.getRuntimeClasspathElements();
        URL[] urls = new URL[elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            urls[i] = new File(elements.get(i)).toURI().toURL();
        }
        return new URLClassLoader(urls, null);
    }
}
