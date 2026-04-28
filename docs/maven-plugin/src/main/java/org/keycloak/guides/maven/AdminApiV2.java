package org.keycloak.guides.maven;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads pre-merged Admin API v2 documentation model and example files
 * for FreeMarker templates.
 *
 * <p>Available in FreeMarker templates as {@code ctx.adminApiV2}.
 */
public class AdminApiV2 {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, Object> categories;
    private final Map<String, Object> schemas;
    private final Map<String, Object> cliExamples;
    private final Map<String, Object> jsExamples;

    public AdminApiV2(Path docFile, Path cliExamplesFile, Path jsExamplesFile) {
        Map<String, Object> doc = readFile(docFile, "admin-v2-doc.json");
        this.categories = asMap(doc.get("categories"));
        this.schemas = asMap(doc.get("schemas"));
        this.cliExamples = readFile(cliExamplesFile, "admin-v2-cli-examples.json");
        this.jsExamples = readFile(jsExamplesFile, "admin-v2-js-examples.json");
    }

    public List<String> getCategories() {
        return new ArrayList<>(categories.keySet());
    }

    public List<Map<String, Object>> getEndpoints(String category) {
        if (!categories.containsKey(category)) {
            return List.of();
        }
        return asList(asMap(categories.get(category)).get("endpoints"));
    }

    public boolean hasEndpoints() {
        return !categories.isEmpty();
    }

    public String getJavaInterface(String category) {
        if (!categories.containsKey(category)) {
            return null;
        }
        return (String) asMap(categories.get(category)).get("interfaceName");
    }

    public List<String> getSchemaNames() {
        return new ArrayList<>(schemas.keySet());
    }

    public Map<String, Object> getSchema(String name) {
        return schemas.containsKey(name) ? asMap(schemas.get(name)) : null;
    }

    public String getJavaVariableName(String category) {
        String iface = getJavaInterface(category);
        if (iface == null) {
            return category;
        }
        String simpleName = iface.substring(iface.lastIndexOf('.') + 1);
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    public String getCliExample(String operationId) {
        return getExample(cliExamples, operationId);
    }

    public String getJsExample(String operationId) {
        return getExample(jsExamples, operationId);
    }

    private static String getExample(Map<String, Object> examples, String operationId) {
        if (examples.containsKey(operationId)) {
            return (String) asMap(examples.get(operationId)).get("example");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        throw new IllegalStateException("Expected a JSON object represented as a Map, but got: " + value);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asList(Object value) {
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        throw new IllegalStateException("Expected a JSON array represented as a List, but got: " + value);
    }

    private static Map<String, Object> readFile(Path filePath, String description) {
        if (filePath == null || !Files.isRegularFile(filePath)) {
            throw new IllegalStateException(
                    "Cannot generate Admin API v2 documentation because " + description + " is missing."
                            + " Build documentation with the command: mvn clean install -am -pl docs/guides -DskipTests");
        }
        try (InputStream is = Files.newInputStream(filePath)) {
            return MAPPER.readValue(is, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + filePath, e);
        }
    }

}
