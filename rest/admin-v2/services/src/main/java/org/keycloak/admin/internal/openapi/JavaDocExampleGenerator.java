package org.keycloak.admin.internal.openapi;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

import static java.util.stream.Collectors.joining;

final class JavaDocExampleGenerator {

    private static final String CLASSES_DIR_PROPERTY = "keycloak.classes.dir";
    private static final String EXAMPLES_CHECK_OUTPUT_DIR =
            "generated-test-sources/openapi/org/keycloak/admin/internal/openapi";
    private static final String PARAM_SEPARATOR = ", ";
    private static final DotName PATH_ANNOTATION = DotName.createSimple(jakarta.ws.rs.Path.class);
    private static final DotName ADMIN_API = DotName.createSimple(org.keycloak.admin.api.AdminApi.class);
    private static final Set<DotName> HTTP_METHOD_ANNOTATIONS = Set.of(
            DotName.createSimple(jakarta.ws.rs.GET.class),
            DotName.createSimple(jakarta.ws.rs.POST.class),
            DotName.createSimple(jakarta.ws.rs.PUT.class),
            DotName.createSimple(jakarta.ws.rs.PATCH.class),
            DotName.createSimple(jakarta.ws.rs.DELETE.class));
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

    record DocCategory(String interfaceName, List<DocEndpoint> endpoints) {
        DocCategory(String interfaceName) {
            this(interfaceName, new ArrayList<>());
        }
    }

    record DocEndpoint(String operationId, String httpMethod, String path,
            String summary, String description,
            RequestBodyInfo requestBody,
            List<ParamInfo> parameters,
            Map<String, ResponseInfo> responses,
            String javaExample) {}

    record RequestBodyInfo(String contentType, String typeName,
            String discriminatorProperty, Map<String, String> discriminatorMapping,
            Boolean partial) {}
    record ParamInfo(String name, String in, String description) {}
    record ResponseInfo(String description, String type, String typeRef,
            Map<String, String> discriminatorMapping) {}
    record JavaExample(String interfaceName, String example) {}

    record DocModel(Map<String, DocCategory> categories, Map<String, DocSchema> schemas) {}
    record DocSchema(String parent, List<String> required, List<DocProperty> properties,
            List<String> enumValues) {}
    record DocProperty(String name, String type, String typeRef, String description, Boolean readOnly) {}

    private final IndexView indexView;

    JavaDocExampleGenerator(IndexView indexView) {
        this.indexView = indexView;
    }

    void generate(OpenAPI openAPI) {
        if (openAPI.getPaths() == null || openAPI.getPaths().getPathItems().isEmpty()) {
            throw new IllegalStateException("OpenAPI spec has no paths — cannot generate documentation examples");
        }

        String classesDir = System.getProperty(CLASSES_DIR_PROPERTY);
        if (classesDir == null) {
            throw new IllegalStateException("System property '" + CLASSES_DIR_PROPERTY
                    + "' is not set — configure it in the smallrye-open-api-maven-plugin systemPropertyVariables");
        }

        StringBuilder checkBody = new StringBuilder();
        Map<String, JavaExample> javaExamples = collectJavaExamples(checkBody);
        Map<String, DocCategory> categories = buildDocModel(openAPI, javaExamples);
        Map<String, DocSchema> schemas = collectSchemas(openAPI, categories);
        DocModel doc = new DocModel(categories, schemas);
        Path targetDir = Path.of(classesDir).getParent();

        try {
            MAPPER.writeValue(targetDir.resolve("admin-v2-doc.json").toFile(), doc);
            writeExamplesCompilationCheck(targetDir, checkBody);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write documentation files", e);
        }
    }

    private static Map<String, DocCategory> buildDocModel(OpenAPI openAPI,
            Map<String, JavaExample> javaExamples) {
        Map<String, DocCategory> categories = new LinkedHashMap<>();

        openAPI.getPaths().getPathItems().forEach((path, pathItem) ->
            pathItem.getOperations().forEach((method, operation) -> {
                String operationId = operation.getOperationId();
                if (operationId == null) {
                    throw new IllegalStateException("Operation without operationId at " + method + " " + path);
                }

                JavaExample javaExample = javaExamples.get(operationId);
                if (javaExample == null) {
                    throw new IllegalStateException("No Java example generated for operation: " + operationId);
                }

                String category = extractResourceName(operation, operationId);
                DocCategory categoryData = categories.computeIfAbsent(category, k -> new DocCategory(javaExample.interfaceName()));

                categoryData.endpoints().add(toEndpoint(openAPI, method, path, pathItem, operation, javaExample.example()));
            }));

        return categories;
    }

    private static DocEndpoint toEndpoint(OpenAPI openAPI, PathItem.HttpMethod method, String path,
            PathItem pathItem, Operation operation, String javaExample) {
        return new DocEndpoint(
                operation.getOperationId(),
                method.name(),
                path,
                operation.getSummary(),
                operation.getDescription(),
                toRequestBody(openAPI, method, pathItem, operation),
                toParameters(pathItem, operation),
                toResponses(openAPI, operation),
                javaExample);
    }

    private static RequestBodyInfo toRequestBody(OpenAPI openAPI, PathItem.HttpMethod method,
            PathItem pathItem, Operation operation) {
        var requestBody = operation.getRequestBody();
        if (requestBody == null || requestBody.getContent() == null) {
            return null;
        }
        var mediaTypes = requestBody.getContent().getMediaTypes();
        if (mediaTypes == null || mediaTypes.isEmpty()) {
            return null;
        }
        var entry = mediaTypes.entrySet().iterator().next();
        String contentType = entry.getKey();
        var schema = entry.getValue().getSchema();
        if (schema != null && schema.getRef() != null) {
            return buildRequestBodyInfo(openAPI, contentType, schema.getRef());
        }
        if (method == PathItem.HttpMethod.PATCH) {
            RequestBodyInfo fallback = findSiblingRequestBody(openAPI, pathItem);
            if (fallback != null) {
                return new RequestBodyInfo(contentType, fallback.typeName(),
                        fallback.discriminatorProperty(), fallback.discriminatorMapping(), true);
            }
        }
        return new RequestBodyInfo(contentType, null, null, null, null);
    }

    private static RequestBodyInfo findSiblingRequestBody(OpenAPI openAPI, PathItem pathItem) {
        for (PathItem.HttpMethod siblingMethod : List.of(PathItem.HttpMethod.PUT, PathItem.HttpMethod.POST)) {
            Operation sibling = pathItem.getOperations().get(siblingMethod);
            if (sibling == null || sibling.getRequestBody() == null || sibling.getRequestBody().getContent() == null) {
                continue;
            }
            var siblingMediaTypes = sibling.getRequestBody().getContent().getMediaTypes();
            if (siblingMediaTypes == null || siblingMediaTypes.isEmpty()) {
                continue;
            }
            var siblingSchema = siblingMediaTypes.values().iterator().next().getSchema();
            if (siblingSchema != null && siblingSchema.getRef() != null) {
                return buildRequestBodyInfo(openAPI, null, siblingSchema.getRef());
            }
        }
        return null;
    }

    private static RequestBodyInfo buildRequestBodyInfo(OpenAPI openAPI, String contentType, String ref) {
        String typeName = refToSimpleName(ref);
        String discriminatorProperty = null;
        Map<String, String> discriminatorMapping = null;
        var resolved = resolveDiscriminator(openAPI, typeName);
        if (resolved != null) {
            discriminatorProperty = resolved.getKey();
            discriminatorMapping = resolved.getValue();
        }
        return new RequestBodyInfo(contentType, typeName, discriminatorProperty, discriminatorMapping, null);
    }

    private static Map<String, DocSchema> collectSchemas(OpenAPI openAPI, Map<String, DocCategory> categories) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return Map.of();
        }
        Map<String, Schema> allSchemas = openAPI.getComponents().getSchemas();
        Set<String> referenced = new LinkedHashSet<>();
        for (DocCategory category : categories.values()) {
            for (DocEndpoint endpoint : category.endpoints()) {
                if (endpoint.requestBody() != null && endpoint.requestBody().typeName() != null) {
                    collectReferencedSchemas(allSchemas, endpoint.requestBody().typeName(), referenced);
                }
                if (endpoint.requestBody() != null && endpoint.requestBody().discriminatorMapping() != null) {
                    for (String subtype : endpoint.requestBody().discriminatorMapping().values()) {
                        collectReferencedSchemas(allSchemas, subtype, referenced);
                    }
                }
            }
        }

        // Build reverse mapping: schema name → (discriminator property, value)
        // e.g. OIDCClientRepresentation → ("protocol", "openid-connect")
        Map<String, Map.Entry<String, String>> discriminatorValues = new LinkedHashMap<>();
        for (String name : referenced) {
            Schema schema = allSchemas.get(name);
            if (schema != null && schema.getDiscriminator() != null
                    && schema.getDiscriminator().getPropertyName() != null
                    && schema.getDiscriminator().getMapping() != null) {
                String propName = schema.getDiscriminator().getPropertyName();
                schema.getDiscriminator().getMapping().forEach((value, ref) ->
                        discriminatorValues.put(refToSimpleName(ref), Map.entry(propName, value)));
            }
        }

        Map<String, DocSchema> result = new LinkedHashMap<>();
        for (String name : referenced) {
            Schema schema = allSchemas.get(name);
            if (schema == null) {
                continue;
            }
            if (schema.getEnumeration() != null && !schema.getEnumeration().isEmpty()) {
                List<String> enumValues = schema.getEnumeration().stream()
                        .map(Object::toString).toList();
                result.put(name, new DocSchema(null, null, List.of(), enumValues));
                continue;
            }

            String parent = null;
            Map<String, Schema> ownProperties = new LinkedHashMap<>();
            if (schema.getAllOf() != null) {
                for (Schema part : schema.getAllOf()) {
                    if (part.getRef() != null) {
                        parent = refToSimpleName(part.getRef());
                    }
                    if (part.getProperties() != null) {
                        ownProperties.putAll(part.getProperties());
                    }
                }
            }
            if (schema.getProperties() != null) {
                ownProperties.putAll(schema.getProperties());
            }

            var discriminatorEntry = discriminatorValues.get(name);
            String ownDiscriminatorProp = schema.getDiscriminator() != null
                    ? schema.getDiscriminator().getPropertyName() : null;
            List<DocProperty> properties = new ArrayList<>();
            for (var entry : ownProperties.entrySet()) {
                Schema prop = entry.getValue();
                PropertyType typeInfo = resolvePropertyType(prop);
                String description = prop.getDescription();
                if (discriminatorEntry != null && entry.getKey().equals(discriminatorEntry.getKey())) {
                    description = "Fixed value: `" + discriminatorEntry.getValue() + "`";
                } else if (entry.getKey().equals(ownDiscriminatorProp)
                        && schema.getDiscriminator().getMapping() != null) {
                    description = "Discriminator. Allowed values: "
                            + schema.getDiscriminator().getMapping().keySet().stream()
                            .map(v -> "`" + v + "`")
                            .collect(joining(", "));
                }
                properties.add(new DocProperty(entry.getKey(), typeInfo.display(), typeInfo.ref(), description,
                        Boolean.TRUE.equals(prop.getReadOnly()) ? true : null));
            }

            result.put(name, new DocSchema(parent, schema.getRequired(), properties, null));
        }
        return result;
    }

    private static void collectReferencedSchemas(Map<String, Schema> allSchemas, String name, Set<String> referenced) {
        if (referenced.contains(name) || !allSchemas.containsKey(name)) {
            return;
        }
        Schema schema = allSchemas.get(name);
        // Add parent first so it appears before children
        if (schema.getAllOf() != null) {
            for (Schema part : schema.getAllOf()) {
                if (part.getRef() != null) {
                    collectReferencedSchemas(allSchemas, refToSimpleName(part.getRef()), referenced);
                }
            }
        }
        referenced.add(name);
        // Collect nested $ref properties
        Map<String, Schema> props = new LinkedHashMap<>();
        if (schema.getProperties() != null) {
            props.putAll(schema.getProperties());
        }
        if (schema.getAllOf() != null) {
            for (Schema part : schema.getAllOf()) {
                if (part.getProperties() != null) {
                    props.putAll(part.getProperties());
                }
            }
        }
        for (Schema prop : props.values()) {
            if (prop.getRef() != null) {
                collectReferencedSchemas(allSchemas, refToSimpleName(prop.getRef()), referenced);
            }
            if (prop.getItems() != null && prop.getItems().getRef() != null) {
                collectReferencedSchemas(allSchemas, refToSimpleName(prop.getItems().getRef()), referenced);
            }
        }
    }

    record PropertyType(String display, String ref) {}

    private static PropertyType resolvePropertyType(Schema prop) {
        if (prop.getRef() != null) {
            String name = refToSimpleName(prop.getRef());
            return new PropertyType(name, name);
        }
        if (prop.getType() != null) {
            String type = schemaTypeName(prop.getType());
            if (prop.getItems() != null) {
                String itemRef = null;
                String itemType;
                if (prop.getItems().getRef() != null) {
                    itemType = refToSimpleName(prop.getItems().getRef());
                    itemRef = itemType;
                } else if (prop.getItems().getType() != null) {
                    itemType = schemaTypeName(prop.getItems().getType());
                } else {
                    itemType = "object";
                }
                return new PropertyType(type + "<" + itemType + ">", itemRef);
            }
            return new PropertyType(type, null);
        }
        return new PropertyType(null, null);
    }

    private static String schemaTypeName(Object schemaType) {
        return schemaType.toString().replaceAll("[\\[\\]]", "");
    }

    private static Map.Entry<String, Map<String, String>> resolveDiscriminator(OpenAPI openAPI, String schemaName) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return null;
        }
        var schema = openAPI.getComponents().getSchemas().get(schemaName);
        if (schema == null || schema.getDiscriminator() == null
                || schema.getDiscriminator().getPropertyName() == null
                || schema.getDiscriminator().getMapping() == null) {
            return null;
        }
        Map<String, String> mapping = new LinkedHashMap<>();
        schema.getDiscriminator().getMapping().forEach((key, ref) ->
                mapping.put(key, refToSimpleName(ref)));
        return Map.entry(schema.getDiscriminator().getPropertyName(), mapping);
    }

    private static List<ParamInfo> toParameters(PathItem pathItem, Operation operation) {
        List<ParamInfo> params = new ArrayList<>();
        addParameters(params, pathItem.getParameters());
        addParameters(params, operation.getParameters());
        return params.isEmpty() ? null : params;
    }

    private static void addParameters(List<ParamInfo> target, List<Parameter> source) {
        if (source != null) {
            source.stream()
                    .map(p -> new ParamInfo(p.getName(), p.getIn() != null ? p.getIn().toString() : null, p.getDescription()))
                    .forEach(target::add);
        }
    }

    private static Map<String, ResponseInfo> toResponses(OpenAPI openAPI, Operation operation) {
        if (operation.getResponses() == null || operation.getResponses().getAPIResponses() == null) {
            return null;
        }
        Map<String, ResponseInfo> responses = new LinkedHashMap<>();
        operation.getResponses().getAPIResponses().forEach((code, response) -> {
            Schema schema = resolveResponseSchema(response);
            if (schema == null) {
                responses.put(code, new ResponseInfo(response.getDescription(), null, null, null));
                return;
            }
            // For arrays, resolve the item type's discriminator
            Schema typeSchema = schema.getItems() != null && schema.getItems().getRef() != null
                    ? schema.getItems() : schema;
            PropertyType typeInfo = resolvePropertyType(schema);
            Map<String, String> discriminatorMapping = null;
            if (typeSchema.getRef() != null) {
                var resolved = resolveDiscriminator(openAPI, refToSimpleName(typeSchema.getRef()));
                if (resolved != null) {
                    discriminatorMapping = resolved.getValue();
                }
            }
            responses.put(code, new ResponseInfo(response.getDescription(),
                    typeInfo.display(), typeInfo.ref(), discriminatorMapping));
        });
        return responses;
    }

    private static Schema resolveResponseSchema(APIResponse response) {
        if (response.getContent() == null) {
            return null;
        }
        var mediaTypes = response.getContent().getMediaTypes();
        if (mediaTypes == null || mediaTypes.isEmpty()) {
            return null;
        }
        return mediaTypes.values().iterator().next().getSchema();
    }

    private Map<String, JavaExample> collectJavaExamples(StringBuilder checkBody) {
        Map<String, JavaExample> examples = new LinkedHashMap<>();
        ClassInfo adminApi = indexView.getClassByName(ADMIN_API);
        if (adminApi == null) {
            throw new IllegalStateException(ADMIN_API + " not found in Jandex index");
        }

        // AdminApi sub-resource locators mirror the wrapper methods on Keycloak admin client.
        // E.g. AdminApi.clientsV2() → Keycloak.clients(realm).v2() both return ClientsApi.
        // Variable name is derived from interface: ClientsApi → clientsApi
        // so the doc template can show: ClientsApi clientsApi = adminClient.clients(realm).v2();
        for (MethodInfo method : adminApi.methods()) {
            ClassInfo subResource = resolveSubResourceInterface(method);
            if (subResource != null) {
                String varName = toVariableName(subResource.name());
                checkBody.append("        ").append(subResource.name()).append(" ").append(varName).append(" = null;\n");
                collectExamples(subResource, varName, varName, examples, checkBody);
            }
        }
        return examples;
    }

    private void collectExamples(ClassInfo iface, String docPrefix, String checkPrefix,
            Map<String, JavaExample> examples, StringBuilder checkBody) {
        for (MethodInfo method : iface.methods()) {
            String docCall = methodCall(docPrefix, method.name(), paramNames(method));
            String checkCall = methodCall(checkPrefix, method.name(), paramNulls(method));

            if (HTTP_METHOD_ANNOTATIONS.stream().anyMatch(method::hasAnnotation)) {
                if (examples.containsKey(method.name())) {
                    throw new IllegalStateException("Duplicate operationId: " + method.name()
                            + " on " + iface.name() + " — operationId must be unique across all interfaces");
                }
                examples.put(method.name(), new JavaExample(iface.name().toString(), docCall + ";"));
                checkBody.append("        ").append(checkCall).append(";\n");
            } else {
                ClassInfo subResource = resolveSubResourceInterface(method);
                if (subResource != null) {
                    collectExamples(subResource, docCall, checkCall, examples, checkBody);
                }
            }
        }
    }

    private ClassInfo resolveSubResourceInterface(MethodInfo method) {
        if (method.annotation(PATH_ANNOTATION) == null) {
            return null;
        }
        Type returnType = method.returnType();
        if (returnType.kind() != Type.Kind.CLASS) {
            return null;
        }
        ClassInfo returnClass = indexView.getClassByName(returnType.asClassType().name());
        if (returnClass == null || !Modifier.isInterface(returnClass.flags())) {
            return null;
        }
        return returnClass;
    }

    private static void writeExamplesCompilationCheck(Path targetDir, StringBuilder checkBody) throws IOException {
        Path checkDir = targetDir.resolve(EXAMPLES_CHECK_OUTPUT_DIR);
        Files.createDirectories(checkDir);
        Files.writeString(checkDir.resolve("AdminApiV2DocExamplesCheck.java"), """
                package org.keycloak.admin.internal.openapi;

                final class AdminApiV2DocExamplesCheck {
                    static void verify() {
                        %s
                    }
                }
                """.formatted(checkBody));
    }

    private static String paramNames(MethodInfo method) {
        return method.parameters().stream().map(JavaDocExampleGenerator::paramName).collect(joining(PARAM_SEPARATOR));
    }

    private static String paramNulls(MethodInfo method) {
        return method.parameterTypes().stream().map(type -> "(" + type.name() + ") null").collect(joining(PARAM_SEPARATOR));
    }

    private static String paramName(MethodParameterInfo param) {
        String name = param.name();
        if (name == null) {
            throw new IllegalStateException("Parameter name not available for " + param.method().declaringClass().name()
                    + "." + param.method().name() + " — compile with -parameters flag");
        }
        return name;
    }

    private static String methodCall(String prefix, String methodName, String args) {
        return prefix + "." + methodName + "(" + args + ")";
    }

    // TODO: replace tag parsing when https://github.com/keycloak/keycloak/issues/47881 is implemented
    private static String extractResourceName(Operation operation, String operationId) {
        if (operation.getTags() == null) {
            throw new IllegalStateException("Operation " + operationId + " has no tags");
        }
        var pattern = Pattern.compile("(.+) \\(v\\d+\\)");
        for (String tag : operation.getTags()) {
            var matcher = pattern.matcher(tag);
            if (matcher.matches()) {
                return matcher.group(1).toLowerCase();
            }
        }
        throw new IllegalStateException(
                "Operation " + operationId + " has no versioned tag matching 'Name (vN)': " + operation.getTags());
    }

    private static String refToSimpleName(String ref) {
        if (!ref.startsWith(OASModelFilter.REF_PREFIX)) {
            throw new IllegalStateException("Unexpected $ref format: " + ref);
        }
        return ref.substring(OASModelFilter.REF_PREFIX.length());
    }

    private static String toVariableName(DotName name) {
        String simpleName = name.withoutPackagePrefix();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
}
