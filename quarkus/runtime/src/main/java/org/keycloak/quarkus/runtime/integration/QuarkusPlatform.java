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
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.keycloak.platform.PlatformProvider;
import org.keycloak.quarkus.runtime.Environment;

import io.quarkus.runtime.Quarkus;
import org.jboss.logging.Logger;

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
            String dataDir = Environment.getDataDir().orElse(null);

            File tmpDir;
            if (dataDir == null) {
                // Should happen just in non-script launch scenarios
                try {
                    tmpDir = Path.of(System.getProperty("java.io.tmpdir"), "keycloak-quarkus-tmp").toFile();
                    if (tmpDir.exists()) {
                        org.apache.commons.io.FileUtils.deleteDirectory(tmpDir);
                    }
                    if (tmpDir.mkdirs()) {
                        tmpDir.deleteOnExit();
                    }
                } catch (IOException ioex) {
                    throw new RuntimeException("It was not possible to create temporary directory keycloak-quarkus-tmp", ioex);
                }
            } else {
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
