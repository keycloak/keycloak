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

package org.keycloak.provider.quarkus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;
import org.keycloak.platform.Platform;
import org.keycloak.platform.PlatformProvider;
import org.keycloak.util.Environment;

public class QuarkusPlatform implements PlatformProvider {

    private static final Logger log = Logger.getLogger(QuarkusPlatform.class);

    public static void addInitializationException(Throwable throwable) {
        QuarkusPlatform platform = (QuarkusPlatform) Platform.getPlatform();
        platform.addDeferredException(throwable);
    }

    /**
     * <p>Throws a {@link InitializationException} exception to indicate errors during the startup.
     * 
     * <p>Calling this method after the server is started has no effect but just the exception being thrown.
     * 
     * @throws InitializationException the exception holding all errors during startup.
     */
    public static void exitOnError() throws InitializationException {
        QuarkusPlatform platform = (QuarkusPlatform) Platform.getPlatform();
        
        // Check if we had any exceptions during initialization phase
        if (!platform.getDeferredExceptions().isEmpty()) {
            InitializationException quarkusException = new InitializationException();
            for (Throwable inner : platform.getDeferredExceptions()) {
                quarkusException.addSuppressed(inner);
            }
            throw quarkusException;
        }
    }

    /**
     * Similar behavior as per {@code #exitOnError} but convenient to throw a {@link InitializationException} with a single
     * {@code cause}
     * 
     * @param cause the cause
     * @throws InitializationException the initialization exception with the given {@code cause}.
     */
    public static void exitOnError(Throwable cause) throws InitializationException{
        addInitializationException(cause);
        exitOnError();
    }

    Runnable startupHook;
    Runnable shutdownHook;

    private AtomicBoolean started = new AtomicBoolean(false);
    private List<Throwable> deferredExceptions = new CopyOnWriteArrayList<>();
    private File tmpDir;

    @Override
    public void onStartup(Runnable startupHook) {
        this.startupHook = startupHook;
    }

    @Override
    public void onShutdown(Runnable shutdownHook) {
        this.shutdownHook = shutdownHook;
    }

    @Override
    public void exit(Throwable cause) {
        throw new RuntimeException(cause);
    }

    /**
     * Called when Quarkus platform is started
     */
    void started() {
        this.started.set(true);
    }

    public boolean isStarted() {
        return started.get();
    }

    /**
     * Add the exception, which  won't be thrown right-away, but should be thrown later after QuarkusPlatform is initialized (including proper logging)
     *
     * @param t
     */
    private void addDeferredException(Throwable t) {
        deferredExceptions.add(t);
    }

    List<Throwable> getDeferredExceptions() {
        return deferredExceptions;
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
                tmpDir = new File(homeDir, "tmp");
                tmpDir.mkdir();
            }

            if (tmpDir.isDirectory()) {
                this.tmpDir = tmpDir;
                log.debugf("Using server tmp directory: %s", tmpDir.getAbsolutePath());
            } else {
                throw new RuntimeException("Temporary directory " + tmpDir.getAbsolutePath() + " does not exists and it was not possible to create it.");
            }
        }
        return tmpDir;
    }
}
