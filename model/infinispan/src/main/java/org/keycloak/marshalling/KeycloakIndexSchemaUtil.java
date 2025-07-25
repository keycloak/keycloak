/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.marshalling;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.api.annotations.indexing.model.Values;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCacheManagerAdmin;
import org.infinispan.commons.internal.InternalCacheNames;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.AnnotationElement;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.impl.AnnotatedDescriptorImpl;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.jboss.logging.Logger;

public class KeycloakIndexSchemaUtil {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    // Basic annotation data
    private static final String BASIC_ANNOTATION = "Basic";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String SEARCHABLE_ATTRIBUTE = "searchable";
    private static final String PROJECTABLE_ATTRIBUTE = "projectable";
    private static final String AGGREGABLE_ATTRIBUTE = "aggregable";
    private static final String SORTABLE_ATTRIBUTE = "sortable";
    private static final String INDEX_NULL_AS_ATTRIBUTE = "indexNullAs";

    // we only use Basic annotation, we may need to add others in the future.
    private static final List<String> INDEX_ANNOTATION = List.of(BASIC_ANNOTATION);

    /**
     * Uploads the {@link GeneratedSchema} to the Infinispan cluster.
     * <p>
     * If indexing is enabled for one or more entities present in the {@link GeneratedSchema}, users may add a list of
     * entities, and the caches where they live. This method will update the indexing schema and perform the reindexing
     * for the new schema. Note that reindexing may be an expensive operation, depending on the amount of data.
     *
     * @param remoteCacheManager The {@link RemoteCacheManager} connected to the Infinispan server.
     * @param schema             The {@link GeneratedSchema} instance to upload.
     * @param indexedEntities    The {@link List} of indexed entities and their caches. Duplicates are allowed if the
     *                           same entity is stored in multiple caches.
     * @throws NullPointerException if {@code remoteCacheManager} or {@code schema} is null.
     */
    public static void uploadAndReindexCaches(RemoteCacheManager remoteCacheManager, GeneratedSchema schema, List<IndexedEntity> indexedEntities) {
        var key = schema.getProtoFileName();
        var current = schema.getProtoFile();

        var protostreamMetadataCache = remoteCacheManager.<String, String>getCache(InternalCacheNames.PROTOBUF_METADATA_CACHE_NAME);
        var stored = protostreamMetadataCache.getWithMetadata(key);
        if (stored == null) {
            if (protostreamMetadataCache.putIfAbsent(key, current) == null) {
                logger.info("Infinispan ProtoStream schema uploaded for the first time.");
            } else {
                logger.info("Failed to update Infinispan ProtoStream schema. Assumed it was updated by other Keycloak server.");
            }
            checkForProtoSchemaErrors(protostreamMetadataCache);
            return;
        }
        if (Objects.equals(stored.getValue(), current)) {
            logger.info("Infinispan ProtoStream schema is up to date!");
            return;
        }
        if (protostreamMetadataCache.replaceWithVersion(key, current, stored.getVersion())) {
            logger.info("Infinispan ProtoStream schema successful updated.");
            reindexCaches(remoteCacheManager, stored.getValue(), current, indexedEntities);
        } else {
            logger.info("Failed to update Infinispan ProtoStream schema. Assumed it was updated by other Keycloak server.");
        }
        checkForProtoSchemaErrors(protostreamMetadataCache);
    }

    private static void checkForProtoSchemaErrors(RemoteCache<String, String> protostreamMetadataCache) {
        var errors = protostreamMetadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
        if (errors == null) {
            return;
        }
        for (String errorFile : errors.split("\n")) {
            logger.errorf("%nThere was an error in proto file: %s%nError message: %s%nCurrent proto schema: %s%n",
                    errorFile,
                    protostreamMetadataCache.get(errorFile + ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX),
                    protostreamMetadataCache.get(errorFile));
        }
    }

    private static void reindexCaches(RemoteCacheManager remoteCacheManager, String oldSchema, String newSchema, List<IndexedEntity> indexedEntities) {
        if (indexedEntities == null || indexedEntities.isEmpty()) {
            return;
        }
        var oldPS = KeycloakModelSchema.parseProtoSchema(oldSchema);
        var newPS = KeycloakModelSchema.parseProtoSchema(newSchema);
        var admin = remoteCacheManager.administration();

        indexedEntities.stream()
                .filter(Objects::nonNull)
                .filter(indexedEntity -> isEntityChanged(oldPS, newPS, indexedEntity.entity()))
                .map(IndexedEntity::cache)
                .distinct()
                .forEach(cacheName -> updateSchemaAndReIndexCache(admin, cacheName));
    }

    private static boolean isEntityChanged(FileDescriptor oldSchema, FileDescriptor newSchema, String entity) {
        var v1 = KeycloakModelSchema.findEntity(oldSchema, entity);
        var v2 = KeycloakModelSchema.findEntity(newSchema, entity);
        return v1.isPresent() && v2.isPresent() && KeycloakIndexSchemaUtil.isIndexSchemaChanged(v1.get(), v2.get());
    }

    private static void updateSchemaAndReIndexCache(RemoteCacheManagerAdmin admin, String cacheName) {
        admin.updateIndexSchema(cacheName);
        admin.reindexCache(cacheName);
    }

    /**
     * Adds the annotations to the ProtoStream parser.
     */
    public static void configureAnnotationProcessor(Configuration.Builder builder) {
        //TODO remove in the future?
        builder.annotationsConfig()
                .annotation(BASIC_ANNOTATION, AnnotationElement.AnnotationTarget.FIELD)
                .attribute(NAME_ATTRIBUTE)
                .type(AnnotationElement.AttributeType.STRING)
                .defaultValue("")
                .attribute(SEARCHABLE_ATTRIBUTE)
                .type(AnnotationElement.AttributeType.BOOLEAN)
                .defaultValue(true)
                .attribute(PROJECTABLE_ATTRIBUTE)
                .type(AnnotationElement.AttributeType.BOOLEAN)
                .defaultValue(false)
                .attribute(AGGREGABLE_ATTRIBUTE)
                .type(AnnotationElement.AttributeType.BOOLEAN)
                .defaultValue(false)
                .attribute(SORTABLE_ATTRIBUTE)
                .type(AnnotationElement.AttributeType.BOOLEAN)
                .defaultValue(false)
                .attribute(INDEX_NULL_AS_ATTRIBUTE)
                .type(AnnotationElement.AttributeType.STRING)
                .defaultValue(Values.DO_NOT_INDEX_NULL);
    }

    /**
     * Compares two entities and returns {@code true} if any indexing related annotation were changed, added or
     * removed.
     */
    public static boolean isIndexSchemaChanged(Descriptor oldDescriptor, Descriptor newDescriptor) {
        var allFields = Stream.concat(
                oldDescriptor.getFields().stream().map(AnnotatedDescriptorImpl::getName),
                newDescriptor.getFields().stream().map(AnnotatedDescriptorImpl::getName)
        ).collect(Collectors.toSet());
        for (var fieldName : allFields) {
            var oldField = oldDescriptor.findFieldByName(fieldName);
            var newField = newDescriptor.findFieldByName(fieldName);
            if (isNewFieldAdded(oldField, newField)) {
                if (isFieldIndexed(newField)) {
                    // a new field is added and is indexed
                    return true;
                }
                continue;
            }
            if (isNewFieldRemoved(oldField, newField)) {
                if (isFieldIndexed(oldField)) {
                    // an old field is indexed and has been removed
                    return true;
                }
                continue;
            }
            if (isFieldIndexed(oldField) != isFieldIndexed(newField)) {
                // some annotation added or removed
                return true;
            }
            if (!isFieldIndexed(oldField) && !isFieldIndexed(newField)) {
                // nothing changes
                continue;
            }
            if (isAnnotationChanged(oldField, newField)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNewFieldAdded(FieldDescriptor oldField, FieldDescriptor newField) {
        return oldField == null && newField != null;
    }

    private static boolean isNewFieldRemoved(FieldDescriptor oldField, FieldDescriptor newField) {
        return oldField != null && newField == null;
    }

    private static boolean isFieldIndexed(FieldDescriptor descriptor) {
        var annotations = descriptor.getAnnotations();
        return INDEX_ANNOTATION.stream().anyMatch(annotations::containsKey);
    }

    private static boolean isAnnotationChanged(FieldDescriptor oldField, FieldDescriptor newField) {
        return INDEX_ANNOTATION.stream().anyMatch(s -> {
            var oldAnnot = oldField.getAnnotations().get(s);
            var newAnnot = newField.getAnnotations().get(s);
            return isAnnotatedDifferent(oldAnnot, newAnnot);
        });
    }

    private static boolean isAnnotatedDifferent(AnnotationElement.Annotation oldAnnot, AnnotationElement.Annotation newAnnot) {
        if (oldAnnot == null && newAnnot == null) {
            // annotation not present in both field
            return false;
        }
        if (oldAnnot != null && newAnnot == null) {
            // annotation present *only* in old field
            return true;
        }
        if (oldAnnot == null) {
            // annotation present *only* in new field
            return true;
        }
        // check if the attributes didn't change
        return !Objects.equals(getAnnotationValues(oldAnnot), getAnnotationValues(newAnnot));

    }

    private static Map<String, Object> getAnnotationValues(AnnotationElement.Annotation annotation) {
        return annotation.getAttributes()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue().getValue()));
    }

    public record IndexedEntity(String entity, String cache) {
        public IndexedEntity {
            Objects.requireNonNull(entity);
            Objects.requireNonNull(cache);
        }
    }
}
