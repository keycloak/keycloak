/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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


import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.common.StringKeyConverter.StringKey;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapKeycloakTransaction;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder.UpdatePredicatesFunc;
import org.keycloak.storage.SearchableModelField;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * {@link MapKeycloakTransaction} implementation used with the file map storage.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class FileMapKeycloakTransaction<V extends AbstractEntity & UpdatableEntity, M>
  extends ConcurrentHashMapKeycloakTransaction<String, V, M> {

    private final List<Path> touchedPaths = new LinkedList<>();
    private Map<Path, Path> renameOnCommit = new IdentityHashMap<>();

    private final String txId = StringKey.INSTANCE.yieldNewUniqueKey();

    public static <V extends AbstractEntity & UpdatableEntity, M> FileMapKeycloakTransaction<V, M> newInstance(Class<V> entityClass,
      Function<String, Path> dataDirectoryFunc, Function<V, String[]> suggestedPath,
      boolean isExpirableEntity, Map<SearchableModelField<? super M>, UpdatePredicatesFunc<String, V, M>> fieldPredicates) {
        Crud<V, M> crud = new Crud<>(entityClass, dataDirectoryFunc, suggestedPath, isExpirableEntity, fieldPredicates);
        FileMapKeycloakTransaction<V, M> tx = new FileMapKeycloakTransaction<>(entityClass, crud);
        crud.tx = tx;
        return tx;
    }

    private FileMapKeycloakTransaction(Class<V> entityClass, Crud crud) {
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
        this.touchedPaths.forEach(FileMapKeycloakTransaction::silentDelete);
        super.rollback();
    }

    @Override
    public void commit() {
        super.commit();
        renameOnCommit.forEach(FileMapKeycloakTransaction::silentMove);
        this.touchedPaths.forEach(FileMapKeycloakTransaction::silentDelete);
    }

    private static void silentMove(Path from, Path to) {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            // Swallow the exception
        }
    }

    private static void silentDelete(Path p) {
        try {
            if (Files.exists(p) && Files.size(p) == 0) {
                Files.delete(p);
            }
        } catch (IOException e) {
            // Swallow the exception
        }
    }

    public void touch(Path path) throws FileAlreadyExistsException, IOException {
        Files.createFile(path);
        touchedPaths.add(path);
    }

    void registerRenameOnCommit(Path from, Path to) {
        this.renameOnCommit.put(from, to);
        this.touchedPaths.add(from);
    }

    private static class Crud<V extends AbstractEntity & UpdatableEntity, M> extends FileMapStorage.Crud<V, M> {

        private FileMapKeycloakTransaction tx;

        public Crud(Class<V> entityClass, Function<String, Path> dataDirectoryFunc, Function<V, String[]> suggestedPath, boolean isExpirableEntity, Map<SearchableModelField<? super M>, UpdatePredicatesFunc<String, V, M>> fieldPredicates) {
            super(entityClass, dataDirectoryFunc, suggestedPath, isExpirableEntity, fieldPredicates);
        }

        @Override
        protected void touch(Path sp) throws IOException {
            tx.touch(sp);
        }

        @Override
        protected void registerRenameOnCommit(Path from, Path to) {
            tx.registerRenameOnCommit(from, to);
        }

        @Override
        protected String getTxId() {
            return tx.txId;
        }

    }
}
