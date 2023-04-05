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
import org.keycloak.common.util.StackUtil;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.ExpirationUtils;
import org.keycloak.models.map.common.HasRealmId;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.CrudOperations;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder;
import org.keycloak.models.map.storage.file.common.MapEntityContext;
import org.keycloak.models.map.storage.file.yaml.PathWriter;
import org.keycloak.models.map.storage.file.yaml.YamlParser;
import org.keycloak.models.map.storage.file.yaml.YamlWritingMechanism;
import org.keycloak.storage.SearchableModelField;
import org.snakeyaml.engine.v2.emitter.Emitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashMap;
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

import org.snakeyaml.engine.v2.api.DumpSettings;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

public abstract class FileCrudOperations<V extends AbstractEntity & UpdatableEntity, M> implements CrudOperations<V, M>, HasRealmId {

    private static final Logger LOG = Logger.getLogger(FileCrudOperations.class);
    private String defaultRealmId;
    private final Class<V> entityClass;
    private final Function<String, Path> dataDirectoryFunc;
    private final Function<V, String[]> suggestedPath;
    private final boolean isExpirableEntity;
    private final Map<SearchableModelField<? super M>, MapModelCriteriaBuilder.UpdatePredicatesFunc<String, V, M>> fieldPredicates;

    private static final Map<Class<?>, Map<SearchableModelField<?>, MapModelCriteriaBuilder.UpdatePredicatesFunc<?, ?, ?>>> ENTITY_FIELD_PREDICATES = new HashMap<>();

    public static final String SEARCHABLE_FIELD_REALM_ID_FIELD_NAME = ClientModel.SearchableFields.REALM_ID.getName();
    public static final String FILE_SUFFIX = ".yaml";
    public static final DumpSettings DUMP_SETTINGS = DumpSettings.builder()
            .setIndent(4)
            .setIndicatorIndent(2)
            .setIndentWithIndicator(false)
            .build();

    public FileCrudOperations(Class<V> entityClass,
                              Function<String, Path> dataDirectoryFunc,
                              Function<V, String[]> suggestedPath,
                              boolean isExpirableEntity) {
        this.entityClass = entityClass;
        this.dataDirectoryFunc = dataDirectoryFunc;
        this.suggestedPath = suggestedPath;
        this.isExpirableEntity = isExpirableEntity;
        this.fieldPredicates = new IdentityHashMap<>(getPredicates(entityClass));
        this.fieldPredicates.keySet().stream()   // Ignore realmId since this is treated in reading differently
                .filter(f -> Objects.equals(SEARCHABLE_FIELD_REALM_ID_FIELD_NAME, f.getName()))
                .findAny()
                .ifPresent(key -> this.fieldPredicates.replace(key, (builder, op, params) -> builder));
    }

    @SuppressWarnings("unchecked")
    public static <V extends AbstractEntity & UpdatableEntity, M> Map<SearchableModelField<? super M>, MapModelCriteriaBuilder.UpdatePredicatesFunc<String, V, M>> getPredicates(Class<V> entityClass) {
        return (Map) ENTITY_FIELD_PREDICATES.computeIfAbsent(entityClass, n -> {
            Map<SearchableModelField<? super M>, MapModelCriteriaBuilder.UpdatePredicatesFunc<String, V, M>> fieldPredicates = new IdentityHashMap<>(MapFieldPredicates.getPredicates(ModelEntityUtil.getModelType(entityClass)));
            fieldPredicates.keySet().stream()   // Ignore realmId since this is treated in reading differently
                    .filter(f -> Objects.equals(SEARCHABLE_FIELD_REALM_ID_FIELD_NAME, f.getName()))
                    .findAny()
                    .ifPresent(key -> fieldPredicates.replace(key, (builder, op, params) -> builder));

            return (Map) fieldPredicates;
        });
    }

    protected Path getPathForEscapedId(String[] escapedIdPathArray) {
        Path parentDirectory = getDataDirectory();
        Path targetPath = parentDirectory;
        for (String path : escapedIdPathArray) {
            targetPath = targetPath.resolve(path).normalize();
            if (!targetPath.getParent().equals(parentDirectory)) {
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
                .map(FileCrudOperations::escapeId)
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

    public static boolean canParseFile(Path p) {
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

    protected V parse(Path fileName) {
        getLastModifiedTime(fileName);
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
        } else if (!escapedId.endsWith(idFromFilename)) {
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
     *
     * @param value     Object
     * @param forCreate Whether this is for create operation ({@code true}) or
     * @return
     */
    @Override
    public String determineKeyFromValue(V value, boolean forCreate) {
        final boolean randomId;
        String[] proposedId = suggestedPath.apply(value);

        if (!forCreate) {
            String[] escapedProposedId = escapeId(proposedId);
            final String res = proposedId == null ? null : String.join(ID_COMPONENT_SEPARATOR, escapedProposedId);
            if (LOG.isDebugEnabled()) {
                LOG.debugf("determineKeyFromValue: got %s (%s) for %s", res, res == null ? null : String.join(" [/] ", proposedId), value);
            }
            return res;
        }

        if (proposedId == null || proposedId.length == 0) {
            randomId = value.getId() == null;
            proposedId = new String[]{value.getId() == null ? StringKeyConverter.StringKey.INSTANCE.yieldNewUniqueKey() : value.getId()};
        } else {
            randomId = false;
        }

        String[] escapedProposedId = escapeId(proposedId);
        Path sp = getPathForEscapedId(escapedProposedId);   // sp will never be null

        final Path parentDir = sp.getParent();
        if (!Files.isDirectory(parentDir)) {
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
                if (!randomId) {
                    throw new ModelDuplicateException("File " + sp + " already exists!");
                }
                final String lastComponent = StringKeyConverter.StringKey.INSTANCE.yieldNewUniqueKey();
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
        return new MapModelCriteriaBuilder<>(StringKeyConverter.StringKey.INSTANCE, fieldPredicates);
    }

    @Override
    public Stream<V> read(QueryParameters<M> queryParameters) {
        final List<Path> paths;
        FileCriteriaBuilder cb = queryParameters.getModelCriteriaBuilder().flashToModelCriteriaBuilder(FileCriteriaBuilder.criteria());
        String realmId = (String) cb.getSingleRestrictionArgument(SEARCHABLE_FIELD_REALM_ID_FIELD_NAME);
        setRealmId(realmId);

        final Path dataDirectory = getDataDirectory();
        if (!Files.isDirectory(dataDirectory)) {
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
                .filter(FileCrudOperations::canParseFile)
                .map(this::parse)
                .filter(Objects::nonNull);

        MapModelCriteriaBuilder<String, V, M> mcb = queryParameters.getModelCriteriaBuilder().flashToModelCriteriaBuilder(createCriteriaBuilder());

        Predicate<? super String> keyFilter = mcb.getKeyFilter();
        Predicate<? super V> entityFilter;

        if (isExpirableEntity) {
            entityFilter = mcb.getEntityFilter().and(ExpirationUtils::isNotExpired);
        } else {
            entityFilter = mcb.getEntityFilter();
        }

        res = res.filter(e -> keyFilter.test(e.getId()) && entityFilter.test(e));

        if (!queryParameters.getOrderBy().isEmpty()) {
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

        checkIsSafeToModify(sp);

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

    /**
     * Hook to obtain the last modified time of the file identified by the supplied {@link Path}.
     *
     * @param path the {@link Path} to the file whose last modified time it to be obtained.
     * @return the {@link FileTime} corresponding to the file's last modified time.
     */
    protected abstract FileTime getLastModifiedTime(final Path path);

    /**
     * Hook to validate that it is safe to modify the file identified by the supplied {@link Path}. The primary
     * goal is to identify if other transactions have modified the file after it was read by the current transaction,
     * preventing updates to a stale entity.
     *
     * @param path the {@link Path} to the file that is to be modified.
     */
    protected abstract void checkIsSafeToModify(final Path path);
}
