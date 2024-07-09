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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.api.annotations.indexing.model.Values;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.AnnotationElement;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.impl.AnnotatedDescriptorImpl;

public class KeycloakIndexSchemaUtil {

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
     * Compares two entities and returns {@code true} if any indexing related annotation were changed, added or removed.
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

}
