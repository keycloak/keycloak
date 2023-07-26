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
package org.keycloak.models.map.storage.file;

import org.jboss.logging.Logger;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.common.StringKeyConverter.StringKey;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.common.delegate.EntityFieldDelegate;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapStorage;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.storage.ReadOnlyException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.keycloak.models.map.storage.file.FileCrudOperations.ID_COMPONENT_SEPARATOR;

/**
 * {@link MapStorage} implementation used with the file map storage.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class FileMapStorage<V extends AbstractEntity & UpdatableEntity, M>
  extends ConcurrentHashMapStorage<String, V, M, FileCrudOperations<V, M>> {

    private static final Logger LOG = Logger.getLogger(FileMapStorage.class);

    private final List<Path> createdPaths = new LinkedList<>();
    private final List<Path> pathsToDelete = new LinkedList<>();
    private final Map<Path, Path> renameOnCommit = new LinkedHashMap<>();
    private final Map<Path, FileTime> lastModified = new HashMap<>();

    private final String txId = StringKey.INSTANCE.yieldNewUniqueKey();

    public static <V extends AbstractEntity & UpdatableEntity, M> FileMapStorage<V, M> newInstance(Class<V> entityClass,
      Function<String, Path> dataDirectoryFunc, Function<V, String[]> suggestedPath,
      boolean isExpirableEntity) {
        Crud<V, M> crud = new Crud<>(entityClass, dataDirectoryFunc, suggestedPath, isExpirableEntity);
        FileMapStorage<V, M> store = new FileMapStorage<>(entityClass, crud);
        crud.store = store;
        return store;
    }

    private FileMapStorage(Class<V> entityClass, Crud<V, M> crud) {
        super(
          crud,
          StringKeyConverter.StringKey.INSTANCE,
          DeepCloner.DUMB_CLONER,
          MapFieldPredicates.getPredicates(ModelEntityUtil.getModelType(entityClass)),
          ModelEntityUtil.getRealmIdField(entityClass)
        );
    }

    @Override
    public void rollback() {
        // remove all temporary and empty files that were created.
        this.renameOnCommit.keySet().forEach(FileMapStorage::silentDelete);
        this.createdPaths.forEach(FileMapStorage::silentDelete);
        super.rollback();
    }

    @Override
    public void commit() {
        super.commit();
        // check it is still safe to update/delete before moving the temp files into the actual files or deleting them.
        Set<Path> allChangedPaths = new HashSet<>();
        allChangedPaths.addAll(this.renameOnCommit.values());
        allChangedPaths.addAll(this.pathsToDelete);
        allChangedPaths.forEach(this::checkIsSafeToModify);
        try {
            this.renameOnCommit.forEach(FileMapStorage::move);
            this.pathsToDelete.forEach(FileMapStorage::silentDelete);
            // TODO: catch exception thrown by move and try to restore any previously completed moves.
        } finally {
            // ensure all temp files are removed.
            this.renameOnCommit.keySet().forEach(FileMapStorage::silentDelete);
            // remove any created files that may have been left empty.
            this.createdPaths.forEach(path -> silenteDelete(path, true));
        }
    }

    private static void move(Path from, Path to) {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void silentDelete(Path p) {
        silenteDelete(p, false);
    }

    private static void silenteDelete(final Path path, final boolean checkEmpty) {
        try {
            if (Files.exists(path)) {
                if (!checkEmpty || Files.size(path) == 0) {
                    Files.delete(path);
                }
            }
        } catch(IOException e) {
            // swallow the exception.
        }
    }

    public void touch(String proposedId, Path path) throws IOException {
        if (Optional.ofNullable(tasks.get(proposedId)).map(MapTaskWithValue::getOperation).orElse(null) == MapOperation.DELETE) {
            // If deleted in the current transaction before this operation, then do not touch
            return;
        }
        Files.createFile(path);
        createdPaths.add(path);
    }

    public boolean removeIfExists(Path path) {
        final boolean res = ! pathsToDelete.contains(path) && Files.exists(path);
        pathsToDelete.add(path);
        return res;
    }

    void registerRenameOnCommit(Path from, Path to) {
        pathsToDelete.remove(to);
        this.renameOnCommit.put(from, to);
    }

    /**
     * Obtains and stores the last modified time of the file identified by the supplied {@link Path}. This value is used
     * to determine if the file was changed by another transaction after it was read by this transaction.
     *
     * @param path the {@link Path} to the file.
     */
    FileTime getLastModifiedTime(final Path path) {
        try {
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
            FileTime lastModifiedTime = attr.lastModifiedTime();
            this.lastModified.put(path, lastModifiedTime);
            return lastModifiedTime;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read file attributes " + path, ex);
        }
    }

    /**
     * Checks if it is safe to modify the file identified by the supplied {@link Path}. In particular, this method
     * verifies if the file was changed (removed, updated) after it was read by this transaction. Being it the case, this
     * transaction should refrain from performing further updates as it must assume its data has become stale.
     *
     * @param path the {@link Path} to the file that will be updated.
     * @throws IllegalStateException if the file was altered by another transaction.
     */
    void checkIsSafeToModify(final Path path) {
        try {
            // path wasn't previously loaded - log a message and return.
            if (this.lastModified.get(path) == null) {
                LOG.debugf("File %s was not previously loaded, skipping validation prior to writing", path);
                return;
            }
            // check if the original file was deleted by another transaction.
            if (!Files.exists(path)) {
                throw new IllegalStateException("File " + path + " was removed by another transaction");
            }
            // check if the original file was modified by another transaction.
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
            long lastModifiedTime = attr.lastModifiedTime().toMillis();
            if (this.lastModified.get(path).toMillis() < lastModifiedTime) {
                throw new IllegalStateException("File " + path + " was changed by another transaction");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public V registerEntityForChanges(V origEntity) {
        final V watchedValue = super.registerEntityForChanges(origEntity);
        return DeepCloner.DUMB_CLONER.entityFieldDelegate(watchedValue, new IdProtector(watchedValue));
    }

    private static class Crud<V extends AbstractEntity & UpdatableEntity, M> extends FileCrudOperations<V, M> {

        private FileMapStorage<V, M> store;

        public Crud(Class<V> entityClass, Function<String, Path> dataDirectoryFunc, Function<V, String[]> suggestedPath, boolean isExpirableEntity) {
            super(entityClass, dataDirectoryFunc, suggestedPath, isExpirableEntity);
        }

        @Override
        protected void touch(String proposedId, Path sp) throws IOException {
            store.touch(proposedId, sp);
        }

        @Override
        protected void registerRenameOnCommit(Path from, Path to) {
            store.registerRenameOnCommit(from, to);
        }

        @Override
        protected boolean removeIfExists(Path sp) {
            return store.removeIfExists(sp);
        }

        @Override
        protected String getTxId() {
            return store.txId;
        }

        @Override
        protected FileTime getLastModifiedTime(final Path sp) {
            return store.getLastModifiedTime(sp);
        }

        @Override
        protected void checkIsSafeToModify(final Path sp) {
            store.checkIsSafeToModify(sp);
        }
    }

    private class IdProtector extends EntityFieldDelegate.WithEntity<V> {

        public IdProtector(V entity) {
            super(entity);
        }

        @Override
        public <T, EF extends java.lang.Enum<? extends org.keycloak.models.map.common.EntityField<V>> & org.keycloak.models.map.common.EntityField<V>> void set(EF field, T value) {
            String id = entity.getId();
            super.set(field, value);
            checkIdMatches(id, field);
        }

        @Override
        public <T, EF extends java.lang.Enum<? extends org.keycloak.models.map.common.EntityField<V>> & org.keycloak.models.map.common.EntityField<V>> void collectionAdd(EF field, T value) {
            String id = entity.getId();
            super.collectionAdd(field, value);
            checkIdMatches(id, field);
        }

        @Override
        public <T, EF extends java.lang.Enum<? extends org.keycloak.models.map.common.EntityField<V>> & org.keycloak.models.map.common.EntityField<V>> Object collectionRemove(EF field, T value) {
            String id = entity.getId();
            final Object res = super.collectionRemove(field, value);
            checkIdMatches(id, field);
            return res;
        }

        @Override
        public <K, T, EF extends java.lang.Enum<? extends org.keycloak.models.map.common.EntityField<V>> & org.keycloak.models.map.common.EntityField<V>> void mapPut(EF field, K key, T value) {
            String id = entity.getId();
            super.mapPut(field, key, value);
            checkIdMatches(id, field);
        }

        @Override
        public <K, EF extends java.lang.Enum<? extends org.keycloak.models.map.common.EntityField<V>> & org.keycloak.models.map.common.EntityField<V>> Object mapRemove(EF field, K key) {
            String id = entity.getId();
            final Object res = super.mapRemove(field, key);
            checkIdMatches(id, field);
            return res;
        }

        private <EF extends java.lang.Enum<? extends org.keycloak.models.map.common.EntityField<V>> & org.keycloak.models.map.common.EntityField<V>> void checkIdMatches(String id, EF field) throws ReadOnlyException {
            final String idNow = map.determineKeyFromValue(entity, "");
            if (! Objects.equals(id, idNow)) {
                if (idNow.endsWith(ID_COMPONENT_SEPARATOR) && id.startsWith(idNow)) {
                    return;
                }

                throw new ReadOnlyException("Cannot change " + field + " as that would change primary key");
            }
        }

        @Override
        public String toString() {
            return super.toString() + " [protected ID]";
        }
    }
}
