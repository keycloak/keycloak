/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.integration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;
import org.keycloak.platform.PlatformProvider;
import org.keycloak.quarkus.runtime.Environment;

import io.quarkus.runtime.Quarkus;

public class QuarkusPlatform implements PlatformProvider {

    private static final Logger log = Logger.getLogger(QuarkusPlatform.class);

    @Override
    public String name() {
        return "Quarkus";
    }

    private AtomicBoolean started = new AtomicBoolean(false);
    private File tmpDir;

    @Override
    public void exit(Throwable cause) {
        Quarkus.asyncExit(1);
    }

    /**
     * Called when Quarkus platform is started
     */
    public void started() {
        this.started.set(true);
    }

    public boolean isStarted() {
        return started.get();
    }

    @Override
    public File getTmpDirectory() {
        if (tmpDir == null) {
            String homeDir = Environment.getHomeDir();

            File tmpDir;
            if (homeDir == null) {
                // Should happen just in the unit tests
                try {
                    // Use "tmp" directory in case it points to the "target" directory (which is usually the case with quarkus unit tests)
                    // Trying to use "target" subdirectory to avoid the situation when separate subdirectory will be created in the "/tmp" for each build and hence "/tmp" directory being swamped with many subdirectories
                    String tmpDirProp = System.getProperty("java.io.tmpdir");
                    if (tmpDirProp == null || !tmpDirProp.endsWith("target")) {
                        // Fallback to "target" inside "user.dir"
                        String userDirProp = System.getProperty("user.dir");
                        if (userDirProp != null) {
                            File userDir = new File(userDirProp, "target");
                            if (userDir.exists()) {
                                tmpDirProp = userDir.getAbsolutePath();
                            }
                        }
                    }
                    // Finally fallback to system tmp directory. Always create dedicated directory for current user
                    Path path = tmpDirProp != null ? Files.createTempDirectory(new File(tmpDirProp).toPath(), "keycloak-quarkus-tmp") :
                            Files.createTempDirectory("keycloak-quarkus-tmp");
                    tmpDir = path.toFile();
                } catch (IOException ioex) {
                    throw new RuntimeException("It was not possible to create temporary directory keycloak-quarkus-tmp", ioex);
                }
            } else {
                String dataDir = Environment.getDataDir();
                tmpDir = new File(dataDir, "tmp");
                tmpDir.mkdirs();
            }

            if (tmpDir.isDirectory()) {
                this.tmpDir = tmpDir;
                log.debugf("Using server tmp directory: %s", tmpDir.getAbsolutePath());
            } else {
                throw new RuntimeException("Temporary directory " + tmpDir.getAbsolutePath() + " does not exist and it was not possible to create it.");
            }
        }
        return tmpDir;
    }

}
