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

package org.keycloak.provider.wildfly;

import java.io.File;

import org.jboss.logging.Logger;
import org.keycloak.platform.PlatformProvider;
import org.keycloak.services.ServicesLogger;

public class WildflyPlatform implements PlatformProvider {

    private static final Logger log = Logger.getLogger(WildflyPlatform.class);

    Runnable shutdownHook;

    private File tmpDir;

    @Override
    public void onStartup(Runnable startupHook) {
        startupHook.run();
    }

    @Override
    public void onShutdown(Runnable shutdownHook) {
        this.shutdownHook = shutdownHook;
    }

    @Override
    public void exit(Throwable cause) {
        ServicesLogger.LOGGER.fatal("Error during startup", cause);
        exit(1);
    }

    private void exit(int status) {
        new Thread() {
            @Override
            public void run() {
                System.exit(status);
            }
        }.start();
    }

    @Override
    public File getTmpDirectory() {
        if (tmpDir == null) {
            String tmpDirName = System.getProperty("jboss.server.temp.dir");
            if (tmpDirName == null) {
                throw new RuntimeException("System property jboss.server.temp.dir not set");
            }

            File tmpDir = new File(tmpDirName);
            if (tmpDir.isDirectory()) {
                this.tmpDir = tmpDir;
                log.debugf("Using server tmp directory: %s", tmpDir.getAbsolutePath());
            } else {
                throw new RuntimeException("Wildfly temp directory not exists under path: " + tmpDirName);
            }
        }
        return tmpDir;
    }
}
