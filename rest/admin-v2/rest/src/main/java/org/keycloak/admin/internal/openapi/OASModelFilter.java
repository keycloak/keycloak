package org.keycloak.admin.internal.openapi;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import static org.keycloak.services.PatchTypeNames.JSON_MERGE;

public class OASModelFilter implements OASFilter {

    private final Logger log = Logger.getLogger(OASModelFilter.class);
    private final Map<String, ClassInfo> simpleNameToClassInfoMap = new HashMap<>();

    public static final String REF_PREFIX = "#/components/schemas/";
    private static final DotName JSON_NODE = DotName.createSimple(JsonNode.class);

    public OASModelFilter(IndexView indexView) {
        log.debug("Index size: " + indexView.getKnownClasses().size());

        indexView.getKnownClasses().forEach(classInfo -> {
            simpleNameToClassInfoMap.put(classInfo.simpleName(), classInfo);
        });
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        // Sort Paths
        Map<String, PathItem> newPaths = openAPI.getPaths().getPathItems().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> sortOperationsByMethod(entry.getValue())
                ));

        // Replace ALL Paths with sorted Paths
        var paths = OASFactory.createPaths();
        newPaths.forEach(paths::addPathItem);
        openAPI.setPaths(paths);

        removeSchemaAndRefs(openAPI, "BaseRepresentation");

        fixJsonMergePatchRequestObject(openAPI);

        Map<String, Set<Schema>> discriminatorPropertiesToBeAdded = new HashMap<>();

        // Follows https://swagger.io/docs/specification/v3_0/data-models/inheritance-and-polymorphism/
        addDiscriminatorsToParentSchemas(openAPI, discriminatorPropertiesToBeAdded);

        // Add missing discriminator properties to subclass schemas
        // Normally, this is handled by Jackson
        discriminatorPropertiesToBeAdded.forEach((propertyName, schemas) -> {
            schemas.forEach(schema -> {
                if (schema.getProperties() == null || !schema.getProperties().containsKey(propertyName)) {
                    Schema discriminatorPropertySchema = OASFactory.createSchema().addType(Schema.SchemaType.STRING);
                    schema.addProperty(propertyName, discriminatorPropertySchema);
                }
            });
        });
    }

    /**
     * Removes a schema from components and cleans up allOf/oneOf/anyOf references to it
     * from all remaining schemas.
     */
    private void removeSchemaAndRefs(OpenAPI openAPI, String schemaName) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return;
        }

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();

        if (schemas.containsKey(schemaName)) {
            Map<String, Schema> remainingSchemas = new HashMap<>(schemas);
            remainingSchemas.remove(schemaName);
            openAPI.getComponents().setSchemas(remainingSchemas);
            log.debugf("Removed schema '%s'", schemaName);
        }

        String ref = REF_PREFIX + schemaName;
        for (Schema schema : openAPI.getComponents().getSchemas().values()) {
            filterRef(schema::getAllOf, schema::setAllOf, ref);
            filterRef(schema::getOneOf, schema::setOneOf, ref);
            filterRef(schema::getAnyOf, schema::setAnyOf, ref);
        }
    }

    private void filterRef(Supplier<List<Schema>> getter, Consumer<List<Schema>> setter, String refToRemove) {
        List<Schema> schemas = getter.get();
        if (schemas == null) {
            return;
        }
        List<Schema> filtered = schemas.stream()
                .filter(s -> !refToRemove.equals(s.getRef()))
                .collect(Collectors.toList());
        setter.accept(filtered.isEmpty() ? null : filtered);
    }

    /**
     * Currently, if endpoint consumes 'application/merge-patch+json' and the request object is 'JsonNode',
     * SmallRye OpenAPI generates array type schema for the endpoint.
     * See <a href="https://github.com/smallrye/smallrye-open-api/issues/2494">issue 2494</a> for more context.
     * What we need is either no schema, or an object with additional properties, so that the generated client
     * doesn't have empty body. This method removes schema.
     */
    private void fixJsonMergePatchRequestObject(OpenAPI openAPI) {
        if (openAPI.getPaths() == null) {
            return;
        }

        openAPI.getPaths().getPathItems().forEach((path, pathItem) -> {
            if (pathItem.getPATCH() != null && pathItem.getPATCH().getRequestBody() != null) {
                var patchOp = pathItem.getPATCH();
                var requestBody = patchOp.getRequestBody();

                if (requestBody.getContent() != null && requestBody.getContent().getMediaType(JSON_MERGE) != null
                        && hasJsonNodeParameter(patchOp.getOperationId())) {
                    var mediaTypeObject = requestBody.getContent().getMediaType(JSON_MERGE);
                    mediaTypeObject.setSchema(null);
                    log.debugf("Removed request body schema from PATCH path '%s' operation '%s' using content type '%s'", path, patchOp.getOperationId(), JSON_MERGE);
                }
            }
        });
    }

    /**
     * Detects REST interface method which name matches the operation name.
     */
    private boolean hasJsonNodeParameter(String operationId) {
        if (operationId == null) {
            return false;
        }

        for (ClassInfo classInfo : simpleNameToClassInfoMap.values()) {
            for (MethodInfo method : classInfo.methods()) {
                if (method.name().equals(operationId)) {
                    for (Type paramType : method.parameterTypes()) {
                        if (JSON_NODE.equals(paramType.name())) {
                            log.debugf("Method '%s#%s' has parameter with type '%s'", classInfo.name(), method.name());
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Adds discriminator and oneOf references to parent schemas that have Jackson @JsonTypeInfo
     * and @JsonSubTypes annotations. This enables OpenAPI generators to create proper class
     * hierarchies with inheritance.
     */
    private void addDiscriminatorsToParentSchemas(OpenAPI openAPI, Map<String, Set<Schema>> discriminatorPropertiesToBeAdded) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return;
        }

        // Create a copy of schema names to avoid ConcurrentModificationException
        Set<String> schemaNames = new HashSet<>(openAPI.getComponents().getSchemas().keySet());

        for (String schemaName : schemaNames) {
            ClassInfo classInfo = simpleNameToClassInfoMap.get(schemaName);
            if (classInfo == null) {
                continue;
            }

            AnnotationInstance typeInfoAnnotation = classInfo.annotation(JsonTypeInfo.class);
            AnnotationInstance subTypesAnnotation = classInfo.annotation(JsonSubTypes.class);
            if (typeInfoAnnotation == null || subTypesAnnotation == null) {
                continue;
            }

            AnnotationInstance[] typeAnnotations = Optional.of(subTypesAnnotation.value())
                    .map(AnnotationValue::asNestedArray)
                    .orElse(new AnnotationInstance[0]);
            if (typeAnnotations.length == 0) {
                continue;
            }

            // Validate annotations
            AnnotationValue useValue = typeInfoAnnotation.value("use");
            if (useValue == null || (!JsonTypeInfo.Id.NAME.name().equals(useValue.asEnum())
                    && !JsonTypeInfo.Id.SIMPLE_NAME.name().equals(useValue.asEnum()))) {
                throw new IllegalStateException(
                        String.format("@JsonTypeInfo on '%s' must use Id.NAME or Id.SIMPLE_NAME, but found: %s",
                                schemaName, useValue == null ? "null" : useValue.asEnum()));
            }

            AnnotationValue includeValue = typeInfoAnnotation.value("include");
            if (includeValue != null && !JsonTypeInfo.As.PROPERTY.name().equals(includeValue.asEnum())
                    && !JsonTypeInfo.As.EXISTING_PROPERTY.name().equals(includeValue.asEnum())) {
                throw new IllegalStateException(
                        String.format("@JsonTypeInfo on '%s' must use As.PROPERTY or As.EXISTING_PROPERTY, but found: %s",
                                schemaName, includeValue.asEnum()));
            }

            String discriminatorPropertyName = Optional.of(typeInfoAnnotation.value("property"))
                    .map(AnnotationValue::asString)
                    .orElse("");
            if (discriminatorPropertyName.isEmpty()) {
                throw new IllegalStateException(
                        String.format("@JsonTypeInfo on '%s' must specify a non-empty 'property' value", schemaName));
            }

            Schema parentSchema = openAPI.getComponents().getSchemas().get(schemaName);
            if (parentSchema == null) {
                continue;
            }

            // Create discriminator with mappings only (no oneOf on parent schema)
            // OpenAPI generators use the discriminator + allOf on subtypes to establish inheritance
            // Adding oneOf to the parent schema causes generators to merge sibling properties incorrectly
            Discriminator discriminator = OASFactory.createDiscriminator().propertyName(discriminatorPropertyName);

            for (AnnotationInstance typeAnnotation : typeAnnotations) {
                String simpleSubClassName = typeAnnotation.value("value").asClass().name().withoutPackagePrefix();
                String ref = REF_PREFIX + simpleSubClassName;

                // Add mapping to discriminator
                String typeName = Optional.of(typeAnnotation.value("name"))
                        .map(AnnotationValue::asString)
                        .orElse("");
                if (!typeName.isEmpty()) {
                    discriminator.addMapping(typeName, ref);
                }

                // Track subschemas that need discriminator property added
                Schema subSchema = openAPI.getComponents().getSchemas().get(simpleSubClassName);
                if (subSchema != null) {
                    discriminatorPropertiesToBeAdded
                            .computeIfAbsent(discriminatorPropertyName, k -> new HashSet<>())
                            .add(subSchema);
                }
            }

            parentSchema.setDiscriminator(discriminator);
            log.debugf("Added discriminator '%s' to schema '%s' with %d subtypes",
                    discriminatorPropertyName, schemaName, typeAnnotations.length);
        }
    }

    private PathItem sortOperationsByMethod(PathItem pathItem) {
        PathItem sortedPathItem = OASFactory.createPathItem();

        // Add operations order: GET -> POST -> PUT -> PATCH -> DELETE -> HEAD -> OPTIONS -> TRACE
        if (pathItem.getGET() != null) {
            sortedPathItem.setGET(pathItem.getGET());
        }
        if (pathItem.getPOST() != null) {
            sortedPathItem.setPOST(pathItem.getPOST());
        }
        if (pathItem.getPUT() != null) {
            sortedPathItem.setPUT(pathItem.getPUT());
        }
        if (pathItem.getPATCH() != null) {
            sortedPathItem.setPATCH(pathItem.getPATCH());
        }
        if (pathItem.getDELETE() != null) {
            sortedPathItem.setDELETE(pathItem.getDELETE());
        }
        if (pathItem.getHEAD() != null) {
            sortedPathItem.setHEAD(pathItem.getHEAD());
        }
        if (pathItem.getOPTIONS() != null) {
            sortedPathItem.setOPTIONS(pathItem.getOPTIONS());
        }
        if (pathItem.getTRACE() != null) {
            sortedPathItem.setTRACE(pathItem.getTRACE());
        }

        sortedPathItem.setSummary(pathItem.getSummary());
        sortedPathItem.setDescription(pathItem.getDescription());
        sortedPathItem.setServers(pathItem.getServers());
        sortedPathItem.setParameters(pathItem.getParameters());

        return sortedPathItem;
    }
}
