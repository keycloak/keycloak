/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.themeverifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "verify-theme", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true)
public class ThemeVerifierMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    @Parameter(defaultValue = "false", readonly = true)
    private boolean validateMessageFormatQuotes;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Iterator<Resource> resources = mavenProject.getResources().iterator();
        List<String> messages = new ArrayList<>();
        while (resources.hasNext()) {
            Resource resource = resources.next();
            File dir = new File(resource.getDirectory());
            Iterator<File> fileIterator = FileUtils.iterateFiles(dir, MessagePropertiesFilter.INSTANCE, DirectoryFileFilter.INSTANCE);
            while (fileIterator.hasNext()) {
                File file = fileIterator.next();
                messages.addAll(new VerifyMessageProperties(file).withValidateMessageFormatQuotes(validateMessageFormatQuotes).verify());
            }
        }
        if (!messages.isEmpty()) {
            throw new MojoFailureException("Validation errors: " + messages.stream().collect(Collectors.joining(System.lineSeparator())));
        }
    }
}
