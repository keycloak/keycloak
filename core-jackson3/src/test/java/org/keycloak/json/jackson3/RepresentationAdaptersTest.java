package org.keycloak.json.jackson3;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.json.MultivaluedHashMapValue;
import org.keycloak.json.ResourceTypeMap;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ResourceType;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

/**
 * Client and server run the same representation contracts with their own mapper policies.
 */
public class RepresentationAdaptersTest {

    protected JsonMapper mapper() {
        return JsonMapper.builder()
                .configureForJackson2()
                .annotationIntrospector(new KeycloakAnnotationIntrospector3())
                .changeDefaultPropertyInclusion(value -> JsonInclude.Value.construct(
                        JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
                .build();
    }

    private final ObjectMapper legacyMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    public void stringListMapAcceptsScalarArrayAndNullAttributes() throws Exception {
        String json = """
                {"username":"alice","attributes":{
                  "scalar":"value","array":["first","second"],"empty":[],
                  "null":null,"mixed":[null,"text",true,42],"boolean":false,"number":12}}
                """;
        var expected = legacyMapper.readValue(json, UserRepresentation.class);
        var actual = mapper().readValue(json, UserRepresentation.class);

        assertThat(actual.getAttributes(), equalTo(expected.getAttributes()));
        assertThat(actual.getAttributes().get("scalar"), equalTo(List.of("value")));
        assertThat(actual.getAttributes().get("mixed"), equalTo(Arrays.asList(null, "text", "true", "42")));
        assertWireParity(actual);
    }

    @Test
    public void audienceRetainsScalarAndArrayWireForms() throws Exception {
        for (String audience : List.of("\"one\"", "[\"one\"]", "[\"one\",\"two\"]", "[]", "null")) {
            String json = "{\"aud\":" + audience + "}";
            var expected = legacyMapper.readValue(json, JsonWebToken.class);
            var actual = mapper().readValue(json, JsonWebToken.class);

            assertThat(actual.getAudience(), equalTo(expected.getAudience()));
            assertWireParity(actual);
        }
        var token = new JsonWebToken().audience("one");
        assertThat(mapper().readTree(mapper().writeValueAsString(token)).get("aud").stringValue(), equalTo("one"));
    }

    @Test
    public void audiencePreservesLegacyNonTextValues() throws Exception {
        for (String audience : List.of("42", "true", "{}", "[null,42,true,{},[],\"one\"]")) {
            String json = "{\"aud\":" + audience + "}";
            var expected = legacyMapper.readValue(json, JsonWebToken.class);
            var actual = mapper().readValue(json, JsonWebToken.class);

            assertThat(actual.getAudience(), equalTo(expected.getAudience()));
            assertWireParity(actual);
        }
    }

    @Test
    public void resourceTypesAcceptArrayAndObjectForms() throws Exception {
        String resource = "{\"type\":\"document\",\"scopes\":[\"read\",\"write\"]}";
        for (String types : List.of("[" + resource + "]", "{\"document\":" + resource + "}")) {
            String json = "{\"resourceTypes\":" + types + "}";
            var expected = legacyMapper.readValue(json, ResourceTypesValue.class);
            var actual = mapper().readValue(json, ResourceTypesValue.class);

            assertThat(actual.resourceTypes.keySet(), equalTo(Set.of("document")));
            assertThat(actual.resourceTypes.get("document").getScopes(), equalTo(expected.resourceTypes.get("document").getScopes()));
            assertWireParity(actual);
        }
    }

    @Test
    public void resourceTypesRejectScalarInput() {
        assertThrows(tools.jackson.databind.exc.MismatchedInputException.class,
                () -> mapper().readValue("{\"resourceTypes\":42}", ResourceTypesValue.class));
    }

    @Test
    public void multivaluedWorkflowConfigurationSerializationPreservesWireShape() throws Exception {
        var workflow = WorkflowRepresentation.withName("test")
                .onEvent("user-created")
                .withSteps(WorkflowStepRepresentation.create().of("set-user-attribute")
                        .withConfig("key", "value")
                        .withConfig("enabled", "true")
                        .withConfig("values", "first", "second").build())
                .build();
        assertWireParity(workflow);
    }

    @Test
    public void multivaluedConfigurationPreservesValuesAndFiltersReservedProperties() throws Exception {
        String json = """
                {"config":{"reserved":"hidden","scalar":"value","flag":true,"values":["first","second"]}}
                """;
        var expected = legacyMapper.readValue(json, MultivaluedValue.class);
        var actual = mapper().readValue(json, MultivaluedValue.class);

        assertThat(actual.config, equalTo(expected.config));
        assertThat(actual.config.get("flag"), equalTo(List.of("true")));
        assertThat(mapper().readTree(mapper().writeValueAsString(actual)).get("config").has("reserved"), is(false));
        assertWireParity(actual);
    }

    @Test
    public void nativeJacksonNamingWorksInBothDirections() {
        var value = mapper().readValue("{\"some-value\":\"text\"}", NativeNamingValue.class);

        assertThat(value.someValue, equalTo("text"));
        assertThat(mapper().writeValueAsString(value), equalTo("{\"some-value\":\"text\"}"));
    }

    private void assertWireParity(Object value) throws Exception {
        assertThat(legacyMapper.readTree(mapper().writeValueAsString(value)),
                equalTo(legacyMapper.readTree(legacyMapper.writeValueAsString(value))));
    }

    @tools.jackson.databind.annotation.JsonNaming(tools.jackson.databind.PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class NativeNamingValue {
        public String someValue;
    }

    public static class ResourceTypesValue {
        @ResourceTypeMap
        public Map<String, ResourceType> resourceTypes;
    }

    public static class MultivaluedValue {
        @MultivaluedHashMapValue
        public MultivaluedHashMap<String, String> config;

        public String getReserved() {
            return "visible";
        }
    }
}
