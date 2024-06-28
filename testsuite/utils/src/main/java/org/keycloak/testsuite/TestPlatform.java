/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.profile.PropertiesFileProfileConfigResolver;
import org.keycloak.common.profile.PropertiesProfileConfigResolver;
import org.keycloak.platform.PlatformProvider;

public class TestPlatform implements PlatformProvider {

    private static final Logger log = Logger.getLogger(TestPlatform.class);

    private File tmpDir;

    public TestPlatform() {
        Profile.configure(
                new PropertiesProfileConfigResolver(System.getProperties()),
                new PropertiesFileProfileConfigResolver()
        );
    }

    @Override
    public String name() {
        return "Undertow";
    }

    @Override
    public void onStartup(Runnable startupHook) {
        startupHook.run();
    }

    @Override
    public void onShutdown(Runnable shutdownHook) {
    }

    @Override
    public void exit(Throwable cause) {
        throw new RuntimeException(cause);
    }

    @Override
    public File getTmpDirectory() {
        if (tmpDir == null) {
            String projectBuildDir = System.getProperty("project.build.directory");
            File tmpDir;
            if (projectBuildDir != null) {
                tmpDir = new File(projectBuildDir, "server-tmp");
                tmpDir.mkdir();
            } else {
                try {
                    tmpDir = Files.createTempDirectory("keycloak-server-").toFile();
                    tmpDir.deleteOnExit();
                } catch (IOException ioe) {
                    throw new RuntimeException("Could not create temporary directory", ioe);
                }
            }

            if (tmpDir.isDirectory()) {
                this.tmpDir = tmpDir;
                log.infof("Using server tmp directory: %s", tmpDir.getAbsolutePath());
            } else {
                throw new RuntimeException("Directory " + tmpDir + " was not created and does not exists");
            }
        }
        return tmpDir;
    }

    @Override
    public ClassLoader getScriptEngineClassLoader(Config.Scope scriptProviderConfig) {
        // It is fine to return null as nashorn should be automatically included on the classpath of testsuite utils
        return null;
    }
}
