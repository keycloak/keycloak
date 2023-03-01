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

import org.keycloak.common.util.StackUtil;
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
import org.keycloak.models.map.storage.file.yaml.YamlParser;
import org.keycloak.models.map.storage.file.common.MapEntityContext;
import org.keycloak.models.map.storage.file.yaml.PathWriter;
import org.keycloak.models.map.storage.file.yaml.YamlWritingMechanism;
import org.keycloak.storage.SearchableModelField;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    private final static DumpSettings DUMP_SETTINGS = DumpSettings.builder()
      .setIndent(4)
      .setIndicatorIndent(2)
      .setIndentWithIndicator(false)
      .build();

    private final Class<V> entityClass;
    private final Function<String, Path> dataDirectoryFunc;
    private final Function<V, String[]> suggestedPath;
    private final boolean isExpirableEntity;
    private final Map<SearchableModelField<? super M>, UpdatePredicatesFunc<String, V, M>> fieldPredicates;

    // TODO: Add auxiliary directory for indices, locks etc.
    // private final String auxiliaryFilesDirectory;

    public FileMapStorage(Class<V> entityClass, Function<V, String[]> uniqueHumanReadableField, Function<String, Path> dataDirectoryFunc) {
        this.entityClass = entityClass;
        this.fieldPredicates = new IdentityHashMap<>(MapFieldPredicates.getPredicates(ModelEntityUtil.getModelType(entityClass)));
        this.fieldPredicates.keySet().stream()   // Ignore realmId since this is treated in reading differently
          .filter(f -> Objects.equals(SEARCHABLE_FIELD_REALM_ID_FIELD_NAME, f.getName()))
          .findAny()
          .ifPresent(key -> this.fieldPredicates.replace(key, (builder, op, params) -> builder));
        this.dataDirectoryFunc = dataDirectoryFunc;
        this.suggestedPath = uniqueHumanReadableField == null ? v -> v.getId() == null ? null : new String[] { v.getId() } : uniqueHumanReadableField;
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

    public FileMapKeycloakTransaction<V, M> createTransactionInternal(KeycloakSession session) {
        return FileMapKeycloakTransaction.newInstance(entityClass, dataDirectoryFunc, suggestedPath, isExpirableEntity, fieldPredicates);
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

    public static abstract class Crud<V extends AbstractEntity & UpdatableEntity, M> implements ConcurrentHashMapCrudOperations<V, M>, HasRealmId {

        private String defaultRealmId;
        private final Class<V> entityClass;
        private final Function<String, Path> dataDirectoryFunc;
        private final Function<V, String[]> suggestedPath;
        private final boolean isExpirableEntity;
        private final Map<SearchableModelField<? super M>, UpdatePredicatesFunc<String, V, M>> fieldPredicates;

        public Crud(Class<V> entityClass, Function<String, Path> dataDirectoryFunc, Function<V, String[]> suggestedPath, boolean isExpirableEntity, Map<SearchableModelField<? super M>, UpdatePredicatesFunc<String, V, M>> fieldPredicates) {
            this.entityClass = entityClass;
            this.dataDirectoryFunc = dataDirectoryFunc;
            this.suggestedPath = suggestedPath;
            this.isExpirableEntity = isExpirableEntity;

            this.fieldPredicates = new IdentityHashMap<>(fieldPredicates);
            this.fieldPredicates.keySet().stream()   // Ignore realmId since this is treated in reading differently
              .filter(f -> Objects.equals(SEARCHABLE_FIELD_REALM_ID_FIELD_NAME, f.getName()))
              .findAny()
              .ifPresent(key -> this.fieldPredicates.replace(key, (builder, op, params) -> builder));
        }

        protected Path getPathForEscapedId(String[] escapedIdPathArray) {
            Path parentDirectory = getDataDirectory();
            Path targetPath = parentDirectory;
            for (String path : escapedIdPathArray) {
                targetPath = targetPath.resolve(path).normalize();
                if (! targetPath.getParent().equals(parentDirectory)) {
                    LOG.warnf("Path traversal detected: %s", Arrays.toString(escapedIdPathArray));
                    return null;
                }
                parentDirectory = targetPath;
            }

            return targetPath.resolveSibling(targetPath.getFileName() + FILE_SUFFIX);
        }

        protected Path getPathForEscapedId(String escapedId) {
            if (escapedId == null) {
                throw new IllegalStateException("Invalid ID to escape");
            }

            String[] escapedIdArray = ID_COMPONENT_SEPARATOR_PATTERN.split(escapedId);
            return getPathForEscapedId(escapedIdArray);
        }

        // Percent sign + Unix (/) and https://learn.microsoft.com/en-us/windows/win32/fileio/naming-a-file reserved characters
        private static final Pattern RESERVED_CHARACTERS = Pattern.compile("[%<:>\"/\\\\|?*=]");
        private static final String ID_COMPONENT_SEPARATOR = ":";
        private static final String ESCAPING_CHARACTER = "=";
        private static final Pattern ID_COMPONENT_SEPARATOR_PATTERN = Pattern.compile(Pattern.quote(ID_COMPONENT_SEPARATOR) + "+");

        private static String[] escapeId(String[] idArray) {
            if (idArray == null || idArray.length == 0 || idArray.length == 1 && idArray[0] == null) {
                return null;
            }
            return Stream.of(idArray)
              .map(Crud::escapeId)
              .toArray(String[]::new);
        }

        private static String escapeId(String id) {
            Objects.requireNonNull(id, "ID must be non-null");

            StringBuilder idEscaped = new StringBuilder();
            Matcher m = RESERVED_CHARACTERS.matcher(id);
            while (m.find()) {
                m.appendReplacement(idEscaped, String.format(ESCAPING_CHARACTER + "%02x", (int) m.group().charAt(0)));
            }
            m.appendTail(idEscaped);
            final Path pId = Path.of(idEscaped.toString());

            return pId.toString();
        }

        protected V parse(Path fileName) {
            final V parsedObject = YamlParser.parse(fileName, new MapEntityContext<>(entityClass));
            if (parsedObject == null) {
                LOG.debugf("Could not parse %s%s", fileName, StackUtil.getShortStackTrace());
                return null;
            }

            String escapedId = determineKeyFromValue(parsedObject, false);
            final String fileNameStr = fileName.getFileName().toString();
            final String idFromFilename = fileNameStr.substring(0, fileNameStr.length() - FILE_SUFFIX.length());
            if (escapedId == null) {
                LOG.debugf("Determined ID from filename: %s%s", idFromFilename);
                escapedId = idFromFilename;
            } else if (! escapedId.endsWith(idFromFilename)) {
                LOG.warnf("Id \"%s\" does not conform with filename \"%s\", expected: %s", escapedId, fileNameStr, escapeId(escapedId));
            }

            parsedObject.setId(escapedId);
            parsedObject.clearUpdatedFlag();

            return parsedObject;
        }

        @Override
        public V create(V value) {
            // TODO: Lock realm directory for changes (e.g. on realm deletion)
            String escapedId = value.getId();

            writeYamlContents(getPathForEscapedId(escapedId), value);

            return value;
        }

        /**
         * Returns escaped ID - relative file name in the file system with path separator {@link #ID_COMPONENT_SEPARATOR}.
         * @param value Object
         * @param forCreate Whether this is for create operation ({@code true}) or
         * @return
         */
        @Override
        public String determineKeyFromValue(V value, boolean forCreate) {
            final boolean randomId;
            String[] proposedId = suggestedPath.apply(value);

            if (! forCreate) {
                String[] escapedProposedId = escapeId(proposedId);
                final String res = proposedId == null ? null : String.join(ID_COMPONENT_SEPARATOR, escapedProposedId);
                if (LOG.isDebugEnabled()) {
                    LOG.debugf("determineKeyFromValue: got %s (%s) for %s", res, res == null ? null : String.join(" [/] ", proposedId), value);
                }
                return res;
            }

            if (proposedId == null || proposedId.length == 0) {
                randomId = value.getId() == null;
                proposedId = new String[] { value.getId() == null ? StringKey.INSTANCE.yieldNewUniqueKey() : value.getId() };
            } else {
                randomId = false;
            }

            String[] escapedProposedId = escapeId(proposedId);
            Path sp = getPathForEscapedId(escapedProposedId);   // sp will never be null

            final Path parentDir = sp.getParent();
            if (! Files.isDirectory(parentDir)) {
                try {
                    Files.createDirectories(parentDir);
                } catch (IOException ex) {
                    throw new IllegalStateException("Directory does not exist and cannot be created: " + parentDir, ex);
                }
            }

            for (int counter = 0; counter < 100; counter++) {
                LOG.tracef("Attempting to create file %s", sp, StackUtil.getShortStackTrace());
                try {
                    touch(sp);
                    final String res = String.join(ID_COMPONENT_SEPARATOR, escapedProposedId);
                    LOG.debugf("determineKeyFromValue: got %s for created %s", res, value);
                    return res;
                } catch (FileAlreadyExistsException ex) {
                    if (! randomId) {
                        throw new ModelDuplicateException("File " + sp + " already exists!");
                    }
                    final String lastComponent = StringKey.INSTANCE.yieldNewUniqueKey();
                    escapedProposedId[escapedProposedId.length - 1] = lastComponent;
                    sp = getPathForEscapedId(escapedProposedId);
                } catch (IOException ex) {
                    throw new IllegalStateException("Could not create file " + sp, ex);
                }
            }

            return null;
        }

        @Override
        public V read(String key) {
            return Optional.ofNullable(key)
              .map(this::getPathForEscapedId)
              .filter(Files::isReadable)
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
            } catch (IOException | UncheckedIOException ex) {
                LOG.warnf(ex, "Error listing %s", dataDirectory);
                return Stream.empty();
            }
            Stream<V> res = paths.stream()
              .filter(FileMapStorage::canParseFile)
              .map(this::parse)
              .filter(Objects::nonNull);

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
            String escapedId = value.getId();

            Path sp = getPathForEscapedId(escapedId);
            if (sp == null) {
                throw new IllegalArgumentException("Invalid path: " + escapedId);
            }

            // TODO: improve locking
            synchronized (FileMapStorageProviderFactory.class) {
                writeYamlContents(sp, value);
            }

            return value;
        }

        @Override
        public boolean delete(String key) {
            return Optional.ofNullable(key)
              .map(this::getPathForEscapedId)
              .map(this::removeIfExists)
              .orElse(false);
        }

        @Override
        public long delete(QueryParameters<M> queryParameters) {
            return read(queryParameters).map(AbstractEntity::getId).map(this::delete).filter(a -> a).count();
        }

        @Override
        public long getCount(QueryParameters<M> queryParameters) {
            return read(queryParameters).count();
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
            return dataDirectoryFunc.apply(defaultRealmId == null ? null : escapeId(defaultRealmId));
        }

        private void writeYamlContents(Path sp, V value) {
            Path tempSp = sp.resolveSibling("." + getTxId() + "-" + sp.getFileName());
            try (PathWriter w = new PathWriter(tempSp)) {
                final Emitter emitter = new Emitter(DUMP_SETTINGS, w);
                try (YamlWritingMechanism mech = new YamlWritingMechanism(emitter::emit)) {
                    new MapEntityContext<>(entityClass).writeValue(value, mech);
                }
                registerRenameOnCommit(tempSp, sp);
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot write " + sp, ex);
            }
        }

        protected abstract void touch(Path sp) throws IOException;

        protected abstract boolean removeIfExists(Path sp);

        protected abstract void registerRenameOnCommit(Path tempSp, Path sp);

        protected abstract String getTxId();

    }

}
