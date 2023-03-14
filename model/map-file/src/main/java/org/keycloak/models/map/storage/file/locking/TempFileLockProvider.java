/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.file.locking;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.util.Time;
import org.keycloak.models.RealmModel;

import static org.keycloak.models.map.storage.ModelEntityUtil.getModelName;
import static org.keycloak.models.map.storage.ModelEntityUtil.getModelNames;

/**
 * A {@link FileLockProvider} that creates {@link TempFileLock}s.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class TempFileLockProvider implements FileLockProvider {

    private static final String LOCK_SUFFIX = ".lock";

    private Map<String, Function<String, Path>> fileStoreDirectories;

    private Map<String, Function<String, Path>> lockDirectories = new HashMap<>();

    private Path rootLocksDirectory;

    private Duration maxAcquireTime;

    public TempFileLockProvider(final Map<String, Function<String, Path>> fileStoreDirectories, final Config.Scope config) {
        this.fileStoreDirectories = fileStoreDirectories;

        // define the root locks dir. If not configured, it will be resolved as root-realms-dir/.locks
        String locksDir = config.get("dir");
        if (locksDir == null) {
            Path rootRealmsDir = fileStoreDirectories.get(getModelName(RealmModel.class)).apply(null);
            this.rootLocksDirectory = rootRealmsDir.resolve(".locks");
        } else {
            this.rootLocksDirectory = Path.of(locksDir);
        }

        // maximum time in seconds for the lock to be acquired.
        int maxTime = config.getInt("max-acquire-time", 10);
        this.maxAcquireTime = Duration.ofSeconds(maxTime);

        // define all the lock directory path producers
        getModelNames().stream()
                .filter(n -> ! Objects.equals(n, getModelName(RealmModel.class)))
                .forEach(n -> lockDirectories.put(n, getRootDir(rootLocksDirectory, n)));
        lockDirectories.put(getModelName(RealmModel.class), realmId -> realmId == null ? rootLocksDirectory : rootLocksDirectory.resolve(realmId));
    }

    @Override
    public Lock createLock(final String areaName, final String realmId, final Path path) {
        // use the file store area path to extract the section of the path that corresponds to the encoded id.
        Path areaStorePath = this.fileStoreDirectories.get(areaName).apply(realmId);
        if (!path.startsWith(areaStorePath)) {
            throw new IllegalArgumentException("File " + path + " doesn't match path created with area " + areaName + " and realm " + realmId);
        }
        Path encodedIdPath = path.subpath(areaStorePath.getNameCount(), path.getNameCount());

        // create the lock file path using the area, realm, and the encoded id of the original path.
        String lockFileName = encodedIdPath.getFileName() + LOCK_SUFFIX;
        Path lockFilePath = this.lockDirectories.get(areaName).apply(realmId);
        lockFilePath = encodedIdPath.getParent() != null ? lockFilePath.resolve(encodedIdPath.getParent()).resolve(lockFileName)
                : lockFilePath.resolve(lockFileName);
        return new TempFileLock(path, lockFilePath, this.maxAcquireTime);
    }

    @Override
    public void releaseAllLocks() {
        // walk from the root locks dir, deleting all lock files and directories found.
        if (Files.exists(rootLocksDirectory)) {
            try {
                Files.walk(rootLocksDirectory)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(f -> {
                            boolean deleted = f.delete();
                            if (!deleted) {
                                throw new RuntimeException("Unable to delete lock file " + f);
                            }
                        });
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    /**
     * Obtains the lock directory for a particular area.
     *
     * @param rootLocksDirectory the root locks directory.
     * @param areaName the name of the area (model type).
     * @return the {@link Path} for the area lock directory.
     */
    private Function<String, Path> getRootDir(Path rootLocksDirectory, String areaName) {
        Path areaPath = areaName.startsWith("authz-") ? Path.of("authz", areaName.substring(6)) : Path.of(areaName);
        return realmId -> rootLocksDirectory.resolve(realmId).resolve(areaPath);
    }
}

/**
 * A {@link Lock} implementation that uses temporary files as locks. A thread owns the lock for a particular file if it
 * is able to create the temporary lock file. Once it finishes its job, the thread must remove the temporary lock file so
 * other threads can attempt to acquire it.
 * </p>
 * The lock acquisition process uses a backoff algorithm in which the thread sleeps for increasing amounts of time before
 * re-attempting to create the lock file. It does that until it either obtains the lock or the maximum configured acquisition
 * time is reached, in which case it fails with a runtime exception.
 */
class TempFileLock implements Lock {

    private static final Logger LOG = Logger.getLogger(TempFileLock.class);

    private Path originalFilePath;
    private Path lockFilePath;
    private Duration maxAcquireTime;
    private boolean locked;

    public TempFileLock(final Path originalFilePath, final Path lockFilePath, final Duration maxAcquireTime) {
        this.originalFilePath = originalFilePath;
        this.lockFilePath = lockFilePath;
        this.maxAcquireTime = maxAcquireTime;
    }

    @Override
    public Lock acquire() {
        long maximumTime = Time.currentTimeMillis() + maxAcquireTime.toMillis();
        int iteration = 0;

        Path parentDir = lockFilePath.getParent();
        if (!Files.isDirectory(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException ex) {
                throw new IllegalStateException("Directory does not exist and cannot be created: " + parentDir, ex);
            }
        }

        while (true) {
            if (Time.currentTimeMillis() >= maximumTime) {
                throw new LockAcquisitionException(originalFilePath, maxAcquireTime);
            }
            try {
                Files.createFile(lockFilePath);
                LOG.debugf("%s successfully acquired lock for file %s", Thread.currentThread().getName(), originalFilePath);
                locked = true;
                break;
            } catch (FileAlreadyExistsException e) {
                iteration++;
                try {
                    int delay = computeBackoffInterval(50, iteration);
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }
        return this;
    }

    @Override
    public Lock release() {
        if (locked) {
            try {
                if (Files.exists(lockFilePath))
                    Files.delete(lockFilePath);
                LOG.debugf("%s successfully released lock for file %s", Thread.currentThread().getName(), originalFilePath);
                this.locked = false;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return this;
    }

    @Override
    public boolean isActive() {
        return this.locked;
    }

    private static int computeBackoffInterval(int base, int iteration) {
        int bound = base * (1 << iteration);
        return new Random().nextInt(bound);
    }

    @Override
    public String toString() {
        return "[TempFileLock]: " + lockFilePath;
    }
}