package org.keycloak.quarkus.runtime.integration.jaxrs;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.authorization.authzen.AuthZen;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.authorization.AuthorizationSchema;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.ssf.transmitter.resources.AddSubjectRequest;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class QuarkusObjectMapperResolverTest {

    @Test
    public void omitNullTokenResponseFields() throws Exception {
        var response = new AccessTokenResponse();
        response.setToken("access-token");
        var resolver = new QuarkusObjectMapperResolver();
        var mapper = resolver.getContext(AccessTokenResponse.class);

        var json = mapper.readTree(mapper.writeValueAsString(response));

        assertThat(json.get("access_token").asString(), equalTo("access-token"));
        assertThat(json.has("authorization_details"), is(false));
        assertThat(json.has("refresh_token"), is(false));

        var legacyMapper = resolver.getObjectMapper();
        assertThat(legacyMapper.readTree(resolver.getContext(AccessTokenResponse.class).writeValueAsString(response)),
                equalTo(legacyMapper.readTree(legacyMapper.writeValueAsString(response))));
    }

    @Test
    public void serializeStreamsAsArrays() {
        var mapper = new QuarkusObjectMapperResolver().getContext(Stream.class);

        assertThat(mapper.writeValueAsString(Stream.of("first", "second")), equalTo("[\"first\",\"second\"]"));
    }

    @Test
    public void omitNullMapValues() {
        var values = new LinkedHashMap<String, Object>();
        values.put("visible", "value");
        values.put("hidden", null);
        var mapper = new QuarkusObjectMapperResolver().getContext(values.getClass());

        assertThat(mapper.writeValueAsString(values), equalTo("{\"visible\":\"value\"}"));
    }

    @Test
    public void honorPrettyPrinting() {
        String previous = System.getProperty("keycloak.jsonPrettyPrint");
        try {
            System.setProperty("keycloak.jsonPrettyPrint", "true");
            var mapper = new QuarkusObjectMapperResolver().getContext(Object.class);
            var values = new LinkedHashMap<String, String>();
            values.put("key", "value");

            assertThat(mapper.writeValueAsString(values), containsString("\n"));
        } finally {
            if (previous == null) {
                System.clearProperty("keycloak.jsonPrettyPrint");
            } else {
                System.setProperty("keycloak.jsonPrettyPrint", previous);
            }
        }
    }

    @Test
    public void preserveLegacyJsonNodes() throws Exception {
        var resolver = new QuarkusObjectMapperResolver();
        var legacyMapper = resolver.getObjectMapper();
        var mapper = resolver.getContext(Object.class);

        for (String json : List.of("\"signed-data\"", "{\"nested\":[1,true,null]}", "[\"value\",2]", "1.25", "true", "null")) {
            var value = new LinkedHashMap<String, Object>();
            value.put("data", legacyMapper.readTree(json));
            assertThat(json, legacyMapper.readTree(mapper.writeValueAsString(value)),
                    equalTo(legacyMapper.readTree(legacyMapper.writeValueAsString(value))));
        }
    }

    @Test
    public void preserveWorkflowDeserialization() throws Exception {
        String json = "{\"name\":\"test\",\"on\":\"user-created\",\"steps\":[{\"uses\":\"set-user-attribute\",\"custom\":\"value\"}]}";
        var resolver = new QuarkusObjectMapperResolver();
        var legacyMapper = resolver.getObjectMapper();
        var expected = legacyMapper.readValue(json, WorkflowRepresentation.class);
        var actual = resolver.getContext(WorkflowRepresentation.class).readValue(json, WorkflowRepresentation.class);

        assertThat(legacyMapper.readTree(legacyMapper.writeValueAsString(actual)),
                equalTo(legacyMapper.readTree(legacyMapper.writeValueAsString(expected))));
    }

    @Test
    public void preserveAuthorizationSchemaForms() throws Exception {
        var resolver = new QuarkusObjectMapperResolver();
        String resource = "{\"type\":\"document\",\"scopes\":[\"read\",\"write\"]}";
        for (String types : List.of("[" + resource + "]", "{\"document\":" + resource + "}")) {
            String json = "{\"resourceTypes\":" + types + "}";
            var expected = resolver.getObjectMapper().readValue(json, AuthorizationSchema.class);
            var actual = resolver.getContext(AuthorizationSchema.class).readValue(json, AuthorizationSchema.class);
            assertThat(actual.getResourceTypes().keySet(), equalTo(expected.getResourceTypes().keySet()));
            assertThat(actual.getResourceTypes().get("document").getScopes(),
                    equalTo(expected.getResourceTypes().get("document").getScopes()));
        }
    }

    @Test
    public void preserveWorkflowConfiguration() throws Exception {
        var resolver = new QuarkusObjectMapperResolver();
        var legacyMapper = resolver.getObjectMapper();
        var workflow = WorkflowRepresentation.withName("test")
                .onEvent("user-created")
                .withSteps(WorkflowStepRepresentation.create().of("set-user-attribute")
                        .withConfig("key", "value").withConfig("values", "first", "second").build())
                .build();
        String json = legacyMapper.writeValueAsString(workflow);
        var actual = resolver.getContext(WorkflowRepresentation.class).readValue(json, WorkflowRepresentation.class);

        assertThat(legacyMapper.readTree(legacyMapper.writeValueAsString(actual)), equalTo(legacyMapper.readTree(json)));
        assertThat(legacyMapper.readTree(resolver.getContext(WorkflowRepresentation.class).writeValueAsString(workflow)),
                equalTo(legacyMapper.readTree(json)));
    }

    @Test
    public void preserveAuthZenOptions() throws Exception {
        var resolver = new QuarkusObjectMapperResolver();
        var legacyMapper = resolver.getObjectMapper();
        for (var semantic : AuthZen.EvaluationsSemantic.values()) {
            var options = new AuthZen.Options(semantic);
            String json = legacyMapper.writeValueAsString(options);
            assertThat(resolver.getContext(AuthZen.Options.class).readValue(json, AuthZen.Options.class), equalTo(options));
            assertThat(legacyMapper.readTree(resolver.getContext(AuthZen.Options.class).writeValueAsString(options)),
                    equalTo(legacyMapper.readTree(json)));
        }
    }

    @Test
    public void preserveLegacyJsonNodeDeserialization() throws Exception {
        var resolver = new QuarkusObjectMapperResolver();
        var legacyMapper = resolver.getObjectMapper();
        var mapper = resolver.getContext(LegacyTreeHolder.class);

        for (String value : List.of("\"value\"", "{\"nested\":[1,true,null]}", "[1,2]", "1.25", "true", "null")) {
            String json = "{\"data\":" + value + "}";
            var expected = legacyMapper.readValue(json, LegacyTreeHolder.class);
            var actual = mapper.readValue(json, LegacyTreeHolder.class);
            assertThat(json, actual.data, equalTo(expected.data));
        }
    }

    @Test
    public void preserveLegacyTreeNumbers() throws Exception {
        var resolver = new QuarkusObjectMapperResolver();
        var legacy = resolver.getObjectMapper();
        var mapper = resolver.getContext(LegacyTreeHolder.class);
        for (String number : List.of("1", "2147483648", "123456789012345678901234567890", "1.2500", "-0.0", "1e400", "-1e400")) {
            String json = "{\"data\":" + number + "}";
            var expected = legacy.readValue(json, LegacyTreeHolder.class).data;
            var actual = mapper.readValue(json, LegacyTreeHolder.class).data;
            assertThat(number, actual.isNumber(), is(true));
            assertThat(number, actual.numberType(), equalTo(expected.numberType()));
            assertThat(number, actual.numberValue(), equalTo(expected.numberValue()));
            assertThat(number, mapper.writeValueAsString(actual), equalTo(legacy.writeValueAsString(expected)));
        }
    }

    @Test
    public void preserveStructuredTreeConversions() throws Exception {
        var resolver = new QuarkusObjectMapperResolver();
        var legacy = resolver.getObjectMapper();
        var mapper = resolver.getContext(LegacyTreeHolder.class);
        var holder = new LegacyTreeHolder();
        holder.data = legacy.readTree("{\"nested\":[1,true,null],\"explicitNull\":null}");

        var tree = mapper.valueToTree(holder);
        assertThat(tree.get("data").isObject(), is(true));
        assertThat(tree.get("data").get("nested").isArray(), is(true));
        assertThat(tree.get("data").get("explicitNull").isNull(), is(true));
        assertThat(tree.get("data").has("absent"), is(false));
        assertThat(mapper.convertValue(holder, Map.class), equalTo(legacy.convertValue(holder, Map.class)));
        assertThat(mapper.treeToValue(tree, LegacyTreeHolder.class).data, equalTo(holder.data));
        assertThat(mapper.convertValue(Map.of("data", holder.data), LegacyTreeHolder.class).data, equalTo(holder.data));

        assertThat(mapper.readValue("{}", LegacyTreeHolder.class).data, nullValue());
        assertThat(mapper.readValue("{\"data\":null}", LegacyTreeHolder.class).data.isNull(), is(true));
    }

    @Test
    public void preserveInMemoryNumericTypesAndNonFinitePolicy() throws Exception {
        var resolver = new QuarkusObjectMapperResolver();
        var legacy = resolver.getObjectMapper();
        var mapper = resolver.getContext(LegacyTreeHolder.class);
        var factory = legacy.getNodeFactory();
        for (var number : List.of(factory.numberNode(1), factory.numberNode(1L),
                factory.numberNode(new BigInteger("123456789012345678901234567890")),
                factory.numberNode(1.25f), factory.numberNode(1.25d),
                factory.numberNode(new BigDecimal("1.2345678901234567890123456789")),
                factory.numberNode(Double.POSITIVE_INFINITY), factory.numberNode(Double.NEGATIVE_INFINITY),
                factory.numberNode(Double.NaN), factory.numberNode(Float.NaN))) {
            var holder = new LegacyTreeHolder();
            holder.data = number;
            var tree = mapper.valueToTree(holder);
            assertThat(tree.get("data").isNumber(), is(true));
            assertThat(tree.get("data").numberValue(), equalTo(number.numberValue()));
            var converted = mapper.convertValue(Map.of("data", number), LegacyTreeHolder.class).data;
            assertThat(converted.numberType(), equalTo(number.numberType()));
            assertThat(converted.numberValue(), equalTo(number.numberValue()));
            assertThat(mapper.writeValueAsString(holder), equalTo(legacy.writeValueAsString(holder)));
        }
        // Like the legacy mapper, bare non-finite literals are not valid JSON input.
        assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
                () -> legacy.readValue("{\"data\":NaN}", LegacyTreeHolder.class));
        assertThrows(tools.jackson.core.JacksonException.class,
                () -> mapper.readValue("{\"data\":NaN}", LegacyTreeHolder.class));
    }

    @Test
    public void preserveEmbeddedTreeValues() throws Exception {
        var resolver = new QuarkusObjectMapperResolver();
        var legacy = resolver.getObjectMapper();
        var mapper = resolver.getContext(LegacyTreeHolder.class);
        var holder = new LegacyTreeHolder();
        holder.data = legacy.getNodeFactory().binaryNode(new byte[] { 1, 2, 3 });
        assertThat(mapper.writeValueAsString(holder), equalTo(legacy.writeValueAsString(holder)));
        assertThat(mapper.valueToTree(holder).get("data").binaryValue(), equalTo(holder.data.binaryValue()));
        holder.data = legacy.getNodeFactory().pojoNode(Map.of("nested", List.of(1, 2)));
        assertThat(mapper.valueToTree(holder).get("data").isObject(), is(true));
        assertThat(mapper.convertValue(holder, Map.class), equalTo(legacy.convertValue(holder, Map.class)));
    }

    @Test
    public void preserveSubjectIdentifierFormatsAndExtensions() throws Exception {
        var resolver = new QuarkusObjectMapperResolver();
        var legacy = resolver.getObjectMapper();
        var mapper = resolver.getContext(AddSubjectRequest.class);
        for (String subject : List.of("null",
                "{\"format\":\"iss_sub\",\"iss\":\"https://issuer.example\",\"sub\":\"user\"}",
                "{\"subject_type\":\"iss-sub\",\"iss\":\"https://issuer.example\",\"sub\":\"user\"}",
                "{\"format\":\"email\",\"email\":\"user@example.com\"}",
                "{\"format\":\"complex\",\"user\":{\"format\":\"email\",\"email\":\"user@example.com\"}}",
                "{\"format\":\"complex\",\"user\":{\"format\":\"iss_sub\",\"iss\":\"https://issuer.example\",\"sub\":\"user\"}}",
                "{\"format\":\"custom-format\",\"extension\":{\"values\":[1,true,null]}}")) {
            String json = "{\"subject\":" + subject + "}";
            var expected = legacy.readValue(json, AddSubjectRequest.class);
            var actual = mapper.readValue(json, AddSubjectRequest.class);
            assertThat(subject, legacy.valueToTree(actual), equalTo(legacy.valueToTree(expected)));
            assertThat(subject, legacy.readTree(mapper.writeValueAsString(actual)), equalTo(legacy.valueToTree(expected)));
        }
    }

    @Test
    public void rejectInvalidSubjectIdentifierFormats() {
        var resolver = new QuarkusObjectMapperResolver();
        for (String subject : List.of("{}", "{\"format\":null}", "{\"format\":42}")) {
            String json = "{\"subject\":" + subject + "}";
            assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
                    () -> resolver.getObjectMapper().readValue(json, AddSubjectRequest.class));
            assertThrows(tools.jackson.databind.exc.MismatchedInputException.class,
                    () -> resolver.getContext(AddSubjectRequest.class).readValue(json, AddSubjectRequest.class));
        }
    }

    public static class LegacyTreeHolder {
        public com.fasterxml.jackson.databind.JsonNode data;
    }
}
