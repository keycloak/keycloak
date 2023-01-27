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

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.ExpirableEntity;
import org.keycloak.models.map.common.ExpirationUtils;
import org.keycloak.models.map.common.HasRealmId;
import org.keycloak.models.map.common.StringKeyConverter.StringKey;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapCrudOperations;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder.UpdatePredicatesFunc;
import org.keycloak.models.map.storage.file.yaml.parser.YamlContextAwareParser;
import org.keycloak.models.map.storage.file.yaml.parser.map.MapEntityYamlContext;
import org.keycloak.storage.SearchableModelField;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jboss.logging.Logger;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.emitter.Emitter;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

/**
 * A file-based {@link MapStorage}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class FileMapStorage<V extends AbstractEntity & UpdatableEntity, M> implements MapStorage<V, M> {

    private static final Logger LOG = Logger.getLogger(FileMapStorage.class);

    // any REALM_ID field would do, they share the same name
    private static final String SEARCHABLE_FIELD_REALM_ID_FIELD_NAME = ClientModel.SearchableFields.REALM_ID.getName();
    private static final String FILE_SUFFIX = ".yaml";

    private final Class<V> entityClass;
    private final Crud crud = new Crud();
    private final Function<String, Path> dataDirectoryFunc;
    private final boolean isExpirableEntity;
    private final Map<SearchableModelField<? super M>, UpdatePredicatesFunc<String, V, M>> fieldPredicates;

    // TODO: Add auxiliary directory for indices etc.
    // private final String auxiliaryFilesDirectory;

    public FileMapStorage(Class<V> entityClass, Function<String, Path> dataDirectoryFunc) {
        this.entityClass = entityClass;
        this.fieldPredicates = new IdentityHashMap<>(MapFieldPredicates.getPredicates(ModelEntityUtil.getModelType(entityClass)));
        this.fieldPredicates.keySet().stream()   // Ignore realmId since this is treated in reading differently
          .filter(f -> Objects.equals(SEARCHABLE_FIELD_REALM_ID_FIELD_NAME, f.getName()))
          .findAny()
          .ifPresent(key -> this.fieldPredicates.replace(key, (builder, op, params) -> builder));
        this.dataDirectoryFunc = dataDirectoryFunc;
        this.isExpirableEntity = ExpirableEntity.class.isAssignableFrom(entityClass);
    }

    @Override
    public MapKeycloakTransaction<V, M> createTransaction(KeycloakSession session) {
        @SuppressWarnings("unchecked")
        MapKeycloakTransaction<V, M> sessionTransaction = session.getAttribute("file-map-transaction-" + hashCode(), MapKeycloakTransaction.class);

        if (sessionTransaction == null) {
            sessionTransaction = createTransactionInternal(session);
            session.setAttribute("file-map-transaction-" + hashCode(), sessionTransaction);
        }
        return sessionTransaction;
    }

    public MapKeycloakTransaction<V, M> createTransactionInternal(KeycloakSession session) {
        return new FileMapKeycloakTransaction<>(entityClass, crud);
    }

    private static boolean canParseFile(Path p) {
        final String fn = p.getFileName().toString();
        try {
            return Files.isRegularFile(p)
              && Files.size(p) > 0L
              && ! fn.startsWith(".")
              && fn.endsWith(FILE_SUFFIX)
              && Files.isReadable(p);
        } catch (IOException ex) {
            return false;
        }
    }

    private class Crud implements ConcurrentHashMapCrudOperations<V, M>, HasRealmId {

        private String defaultRealmId;

        protected Path getPathForSanitizedId(Path sanitizedIdPath) {
            final Path dataDirectory = getDataDirectory();
            final Path dataDirectoryWithChildren = dataDirectory.resolve(sanitizedIdPath).getParent();

            if (! Files.isDirectory(dataDirectoryWithChildren)) {
                try {
                    Files.createDirectories(dataDirectoryWithChildren);
                } catch (IOException ex) {
                    throw new IllegalStateException("Directory does not exist and cannot be created: " + dataDirectory, ex);
                }
            }
            return dataDirectoryWithChildren.resolve(sanitizedIdPath.getFileName() + FILE_SUFFIX);
        }

        protected Path getPathForSanitizedId(String sanitizedId) {
            if (sanitizedId == null) {
                throw new IllegalStateException("Invalid ID to sanitize");
            }

            return getPathForSanitizedId(Path.of(sanitizedId));
        }

        protected String sanitizeId(String id) {
            Objects.requireNonNull(id, "ID must be non-null");

            // TODO: sanitize
//            id = id
//              .replaceAll("=", "=e")
//              .replaceAll(":", "=c")
//              .replaceAll("/", "=s")
//              .replaceAll("\\\\", "=b")
//            ;
            final Path pId = Path.of(id);

            // Do not allow absolute paths
            if (pId.isAbsolute()) {
                throw new IllegalStateException("Illegal ID requested: " + id);
            }

            return id;
        }

        protected String desanitizeId(String sanitizedId) {
            if (sanitizedId == null) {
                return null;
            }

            return sanitizedId
              .replaceAll("=c", ":")
              .replaceAll("=s", "/")
              .replaceAll("=b", "\\\\")
              .replaceAll("=e", "=")
            ;

        }

        protected V parse(Path fileName) {
            final V parsedObject = YamlContextAwareParser.parse(fileName, new MapEntityYamlContext<>(entityClass));
            if (parsedObject == null) {
                return null;
            }

            final String fileNameStr = fileName.getFileName().toString();
            String id = determineKeyFromValue(parsedObject, false);
            final String desanitizedId = desanitizeId(fileNameStr.substring(0, fileNameStr.length() - FILE_SUFFIX.length()));
            if (id == null) {
                LOG.debugf("Determined ID from filename: %s", desanitizedId);
                id = desanitizedId;
            } else if (! id.endsWith(desanitizedId)) {
                LOG.warnf("Filename \"%s\" does not end with expected id \"%s\". Fix the file name.", fileNameStr, id);
            }

            parsedObject.setId(id);
            parsedObject.clearUpdatedFlag();

            return parsedObject;
        }

        @Override
        public V create(V value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public V read(String key) {
            return Optional.ofNullable(sanitizeId(key))
              .map(this::getPathForSanitizedId)
              .filter(Files::exists)
              .map(this::parse)
              .orElse(null);
        }

        public MapModelCriteriaBuilder<String, V, M> createCriteriaBuilder() {
            return new MapModelCriteriaBuilder<>(StringKey.INSTANCE, fieldPredicates);
        }

        @Override
        public Stream<V> read(QueryParameters<M> queryParameters) {
            final List<Path> paths;
            FileCriteriaBuilder cb = queryParameters.getModelCriteriaBuilder().flashToModelCriteriaBuilder(FileCriteriaBuilder.criteria());
            String realmId = (String) cb.getSingleRestrictionArgument(SEARCHABLE_FIELD_REALM_ID_FIELD_NAME);
            setRealmId(realmId);

            final Path dataDirectory = getDataDirectory();
            if (! Files.isDirectory(dataDirectory)) {
                return Stream.empty();
            }

            // We cannot use Files.find since it throws an UncheckedIOException if it lists a file which is removed concurrently
            // before its BasicAttributes can be retrieved for its BiPredicate parameter
            try (Stream<Path> dirStream = Files.walk(dataDirectory, entityClass == MapRealmEntity.class ? 1 : 2)) {
                // The paths list has to be materialized first, otherwise "dirStream" would be closed
                // before the resulting stream would be read and would return empty result
                paths = dirStream.collect(Collectors.toList());
            } catch (IOException ex) {
                LOG.warnf(ex, "Error listing %s", getDataDirectory());
                return Stream.empty();
            }
            Stream<V> res = paths.stream()
              .filter(FileMapStorage::canParseFile)
              .map(this::parse).filter(Objects::nonNull);

            MapModelCriteriaBuilder<String,V,M> mcb = queryParameters.getModelCriteriaBuilder().flashToModelCriteriaBuilder(createCriteriaBuilder());

            Predicate<? super String> keyFilter = mcb.getKeyFilter();
            Predicate<? super V> entityFilter;

            if (isExpirableEntity) {
                entityFilter = mcb.getEntityFilter().and(ExpirationUtils::isNotExpired);
            } else {
                entityFilter = mcb.getEntityFilter();
            }

            res = res.filter(e -> keyFilter.test(e.getId()) && entityFilter.test(e));

            if (! queryParameters.getOrderBy().isEmpty()) {
                res = res.sorted(MapFieldPredicates.getComparator(queryParameters.getOrderBy().stream()));
            }

            return paginatedStream(res, queryParameters.getOffset(), queryParameters.getLimit());
        }

        @Override
        public V update(V value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean delete(String key) {
            Optional<Path> fileName = Optional.ofNullable(sanitizeId(key))
              .map(this::getPathForSanitizedId);
            try {
                return fileName.isPresent() ? Files.deleteIfExists(fileName.get()) : false;
            } catch (IOException ex) {
                LOG.warnf(ex, "Could not delete file: %s", fileName);
                return false;
            }
        }

        @Override
        public long delete(QueryParameters<M> queryParameters) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long getCount(QueryParameters<M> queryParameters) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getRealmId() {
            return defaultRealmId;
        }

        @Override
        public void setRealmId(String realmId) {
            this.defaultRealmId = realmId;
        }

        private Path getDataDirectory() {
            return dataDirectoryFunc.apply(defaultRealmId);
        }

    }

}
