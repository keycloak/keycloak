package org.keycloak.quarkus.runtime.oas;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quarkus.smallrye.openapi.OpenApiFilter;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

@OpenApiFilter(OpenApiFilter.RunStage.BUILD)
public class OASModelFilter implements OASFilter {

    private final IndexView index;
    private final Logger log = Logger.getLogger(OASModelFilter.class);
    private final Map<String, ClassInfo> simpleNameToClassInfoMap = new HashMap<>();

    public static final String REF_PREFIX = "#/components/schemas/";

    public OASModelFilter(IndexView indexView) {
        this.index = indexView;
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

        Map<String, Set<Schema>> discriminatorPropertiesToBeAdded = new HashMap<>();

        // Reflect Jackson annotations in OpenAPI spec
        // Follows https://swagger.io/docs/specification/v3_0/data-models/inheritance-and-polymorphism/
        openAPI.getPaths().getPathItems().values().stream()
                .flatMap(p -> p.getOperations().values().stream())
                .forEach(operation -> {
                    // This is not nice but so is the model structure...

                    // Request body
                    Optional.ofNullable(operation.getRequestBody())
                            .map(RequestBody::getContent)
                            .map(Content::getMediaTypes)
                            .map(Map::values)
                            .map(Collection::stream)
                            .ifPresent(mediaTypes -> {
                                mediaTypes.forEach(mediaType -> {
                                    mediaType.setSchema(replaceSchemaWithChildrenIfNeeded(mediaType.getSchema(), openAPI, discriminatorPropertiesToBeAdded));
                                });
                            });

                    // Responses
                    Optional.ofNullable(operation.getResponses())
                            .map(APIResponses::getAPIResponses)
                            .map(Map::values)
                            .map(Collection::stream)
                            .ifPresent(apiResponses -> {
                                apiResponses.forEach(apiResponse -> {
                                    Optional.ofNullable(apiResponse.getContent())
                                            .map(Content::getMediaTypes)
                                            .map(Map::values)
                                            .map(Collection::stream)
                                            .ifPresent(mediaTypes -> {
                                                mediaTypes.forEach(mediaType -> {
                                                    mediaType.setSchema(replaceSchemaWithChildrenIfNeeded(mediaType.getSchema(), openAPI, discriminatorPropertiesToBeAdded));
                                                });
                                            });
                                });
                            });
                });

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

    /**
     * Replaces the given schema with a new schema that uses oneOf to reference all subclasses if the original schema
     * has a $ref and the referenced class has Jackson @JsonTypeInfo and @JsonSubTypes annotations. I.e. adds polymorphism
     * support to OpenAPI generation.
     *
     * @param originalSchema
     * @param openAPI
     * @param discriminatorPropertiesToBeAdded
     * @return the new schema or the original schema if no changes were made
     */
    private Schema replaceSchemaWithChildrenIfNeeded(Schema originalSchema, OpenAPI openAPI, Map<String, Set<Schema>> discriminatorPropertiesToBeAdded) {
        Schema arraySchema = null;
        if (originalSchema.getType() != null && originalSchema.getType().size() == 1 && Schema.SchemaType.ARRAY.equals(originalSchema.getType().get(0))) {
            arraySchema = originalSchema;
            originalSchema = originalSchema.getItems();
        }

        if (originalSchema == null || originalSchema.getRef() == null) {
            return originalSchema;
        }

        String parentSchemaName = originalSchema.getRef().substring(REF_PREFIX.length());

        ClassInfo parentClassInfo = simpleNameToClassInfoMap.get(parentSchemaName);
        if (parentClassInfo == null) {
            throw new IllegalStateException("Could not find class in index for schema: " + parentSchemaName);
        }

        AnnotationInstance typeInfoAnnotation = parentClassInfo.annotation(JsonTypeInfo.class);
        AnnotationInstance subTypesAnnotation = parentClassInfo.annotation(JsonSubTypes.class);
        if (typeInfoAnnotation == null || subTypesAnnotation == null) {
            log.debugf("Class %s does not have JsonTypeInfo or JsonSubTypes annotations, skipping", parentClassInfo.simpleName());
            return originalSchema;
        }

        AnnotationInstance[] typeAnnotations = Optional.of(subTypesAnnotation.value()).map(AnnotationValue::asNestedArray).orElse(new AnnotationInstance[0]);
        if (typeAnnotations.length == 0) {
            log.debugf("Class %s does not have any JsonSubTypes defined, skipping", parentClassInfo.simpleName());
            return originalSchema;
        }

        // Validations

        AnnotationValue useValue = typeInfoAnnotation.value("use");
        if (useValue == null || !JsonTypeInfo.Id.SIMPLE_NAME.name().equals(useValue.asEnum())) {
            throw new IllegalArgumentException(parentClassInfo.simpleName() + ": JsonTypeInfo annotation must have use=SIMPLE_NAME.");
        }

        AnnotationValue includeValue = typeInfoAnnotation.value("include");
        if (includeValue != null && !JsonTypeInfo.As.PROPERTY.name().equals(includeValue.asEnum())) {
            throw new IllegalArgumentException(parentClassInfo.simpleName() + ": JsonTypeInfo annotation must have include=PROPERTY, or include must not be set.");
        }

        String discriminatorPropertyName = Optional.of(typeInfoAnnotation.value("property")).map(AnnotationValue::asString).orElse("");
        if (discriminatorPropertyName.isEmpty()) {
            throw new IllegalArgumentException(parentClassInfo.simpleName() + ": JsonTypeInfo annotation must have property set.");
        }

        Schema newSchema = OASFactory.createSchema();

        // Add discriminator

        Discriminator discriminator = OASFactory.createDiscriminator().propertyName(discriminatorPropertyName);
        newSchema.setDiscriminator(discriminator);

        // Create new schema with oneOf for each subclass

        for (AnnotationInstance typeAnnotation : typeAnnotations) {
            String simpleSubClassName = typeAnnotation.value("value").asClass().name().withoutPackagePrefix();

            // Add schema ref as oneOf to the new schema
            Schema subSchema = openAPI.getComponents().getSchemas().get(simpleSubClassName); // This won't work with inner classes due to '$' in the name
            if (subSchema == null) {
                throw new IllegalStateException(parentClassInfo.simpleName() + ": Could not find schema for subclass: " + simpleSubClassName + ". Make sure the subclass has the @Schema annotation.");
            }
            String ref = REF_PREFIX + simpleSubClassName;
            Schema schemaRef = OASFactory.createSchema().ref(ref);
            newSchema.addOneOf(schemaRef);

            // Add mapping to discriminator
            String typeName = Optional.of(typeAnnotation.value("name")).map(AnnotationValue::asString).orElse("");
            if (!typeName.isEmpty()) {
                discriminator.addMapping(typeName, ref);
            }

            discriminatorPropertiesToBeAdded.computeIfAbsent(discriminatorPropertyName, k -> new HashSet<>()).add(subSchema);
        }

        if (arraySchema != null) {
            arraySchema.setItems(newSchema);
            newSchema = arraySchema;
        }

        return newSchema;
    }
}
