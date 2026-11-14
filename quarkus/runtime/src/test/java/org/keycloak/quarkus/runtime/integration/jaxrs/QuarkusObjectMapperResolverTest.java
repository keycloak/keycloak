package org.keycloak.quarkus.runtime.integration.jaxrs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.authorization.authzen.AuthZen;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.authorization.AuthorizationSchema;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.ssf.subject.ComplexSubjectId;
import org.keycloak.ssf.subject.EmailSubjectId;
import org.keycloak.ssf.subject.GenericSubjectId;
import org.keycloak.ssf.subject.IssuerSubjectId;
import org.keycloak.ssf.transmitter.resources.AddSubjectRequest;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class QuarkusObjectMapperResolverTest {

    @Test
    public void preserveIssuerInfoAcrossInternalAndRestMappers() throws Exception {
        var mapper = new QuarkusObjectMapperResolver().getContext(CredentialIssuer.class);
        for (String data : List.of("\"signed-certificate\"", "{\"nested\":[1,true,null]}",
                "[\"value\",2]", "1.25", "true", "null")) {
            String json = "{\"issuer_info\":[{\"format\":\"registration_cert\",\"data\":" + data + "}]}";
            var issuer = JsonSerialization.readValue(json, CredentialIssuer.class);

            assertThat(JsonSerialization.mapper.readTree(mapper.writeValueAsString(issuer)),
                    equalTo(JsonSerialization.mapper.readTree(JsonSerialization.writeValueAsString(issuer))));
            var restored = mapper.readValue(json, CredentialIssuer.class);
            assertThat(JsonSerialization.mapper.readTree(JsonSerialization.writeValueAsString(restored)),
                    equalTo(JsonSerialization.mapper.readTree(JsonSerialization.writeValueAsString(issuer))));
        }
    }

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
    public void preserveWorkflowDeserialization() throws Exception {
        String json = "{\"name\":\"test\",\"on\":\"user-created\",\"steps\":[{\"uses\":\"set-user-attribute\",\"custom\":\"value\"}]}";
        var resolver = new QuarkusObjectMapperResolver();
        var actual = resolver.getContext(WorkflowRepresentation.class).readValue(json, WorkflowRepresentation.class);

        assertThat(actual.getName(), equalTo("test"));
        assertThat(actual.getOn(), equalTo("user-created"));
        assertThat(actual.getSteps().size(), equalTo(1));
        var step = actual.getSteps().get(0);
        assertThat(step.getUses(), equalTo("set-user-attribute"));
        assertThat(step.getConfig().get("custom"), equalTo(List.of("value")));
    }

    @Test
    public void preserveAuthorizationSchemaForms() {
        var mapper = new QuarkusObjectMapperResolver().getContext(AuthorizationSchema.class);
        String resource = "{\"type\":\"document\",\"scopes\":[\"read\",\"write\"]}";
        for (String types : List.of("[" + resource + "]", "{\"document\":" + resource + "}")) {
            var actual = mapper.readValue("{\"resourceTypes\":" + types + "}", AuthorizationSchema.class);

            assertThat(actual.getResourceTypes().keySet(), equalTo(Set.of("document")));
            assertThat(actual.getResourceTypes().get("document").getScopes(), equalTo(Set.of("read", "write")));
        }
    }

    @Test
    public void preserveWorkflowConfiguration() {
        var mapper = new QuarkusObjectMapperResolver().getContext(WorkflowRepresentation.class);
        var workflow = WorkflowRepresentation.withName("test")
                .onEvent("user-created")
                .withSteps(WorkflowStepRepresentation.create().of("set-user-attribute")
                        .withConfig("key", "value").withConfig("values", "first", "second").build())
                .build();
        String json = "{\"name\":\"test\",\"on\":\"user-created\",\"steps\":[{\"uses\":\"set-user-attribute\","
                + "\"with\":{\"key\":\"value\",\"values\":[\"first\",\"second\"]}}]}";
        var actual = mapper.readValue(json, WorkflowRepresentation.class);

        assertThat(actual.getName(), equalTo("test"));
        assertThat(actual.getOn(), equalTo("user-created"));
        assertThat(actual.getSteps().size(), equalTo(1));
        var step = actual.getSteps().get(0);
        assertThat(step.getUses(), equalTo("set-user-attribute"));
        assertThat(step.getConfig(), equalTo(Map.of("key", List.of("value"), "values", List.of("first", "second"))));
        assertThat(mapper.readTree(mapper.writeValueAsString(workflow)), equalTo(mapper.readTree(json)));
    }

    @Test
    public void preserveAuthZenOptions() {
        var mapper = new QuarkusObjectMapperResolver().getContext(AuthZen.Options.class);
        var semantics = Map.of(
                AuthZen.EvaluationsSemantic.EXECUTE_ALL, "execute_all",
                AuthZen.EvaluationsSemantic.DENY_ON_FIRST_DENY, "deny_on_first_deny",
                AuthZen.EvaluationsSemantic.PERMIT_ON_FIRST_PERMIT, "permit_on_first_permit");
        semantics.forEach((semantic, value) -> {
            var options = new AuthZen.Options(semantic);
            String json = "{\"evaluations_semantic\":\"" + value + "\"}";

            assertThat(mapper.readValue(json, AuthZen.Options.class), equalTo(options));
            assertThat(mapper.readTree(mapper.writeValueAsString(options)), equalTo(mapper.readTree(json)));
        });
    }

    @Test
    public void preserveIssuerSubjectIdentifier() {
        var mapper = new QuarkusObjectMapperResolver().getContext(AddSubjectRequest.class);
        String expected = "{\"subject\":{\"format\":\"iss_sub\",\"iss\":\"https://issuer.example\",\"sub\":\"user\"}}";
        for (String discriminator : List.of("\"format\":\"iss_sub\"", "\"subject_type\":\"iss-sub\"")) {
            String json = "{\"subject\":{" + discriminator + ",\"iss\":\"https://issuer.example\",\"sub\":\"user\"}}";
            var actual = mapper.readValue(json, AddSubjectRequest.class);

            assertThat(actual.getSubject().getClass(), equalTo(IssuerSubjectId.class));
            var subject = (IssuerSubjectId) actual.getSubject();
            assertThat(subject.getFormat(), equalTo("iss_sub"));
            assertThat(subject.getIss(), equalTo("https://issuer.example"));
            assertThat(subject.getSub(), equalTo("user"));
            assertThat(mapper.readTree(mapper.writeValueAsString(actual)), equalTo(mapper.readTree(expected)));
        }
    }

    @Test
    public void preserveEmailSubjectIdentifier() {
        var mapper = new QuarkusObjectMapperResolver().getContext(AddSubjectRequest.class);
        String json = "{\"subject\":{\"format\":\"email\",\"email\":\"user@example.com\"}}";
        var actual = mapper.readValue(json, AddSubjectRequest.class);

        assertThat(actual.getSubject().getClass(), equalTo(EmailSubjectId.class));
        assertThat(((EmailSubjectId) actual.getSubject()).getEmail(), equalTo("user@example.com"));
        assertThat(mapper.readTree(mapper.writeValueAsString(actual)), equalTo(mapper.readTree(json)));
    }

    @Test
    public void preserveComplexSubjectIdentifier() {
        var mapper = new QuarkusObjectMapperResolver().getContext(AddSubjectRequest.class);
        for (String user : List.of("{\"format\":\"email\",\"email\":\"user@example.com\"}",
                "{\"format\":\"iss_sub\",\"iss\":\"https://issuer.example\",\"sub\":\"user\"}")) {
            String json = "{\"subject\":{\"format\":\"complex\",\"user\":" + user + "}}";
            var actual = mapper.readValue(json, AddSubjectRequest.class);

            assertThat(actual.getSubject().getClass(), equalTo(ComplexSubjectId.class));
            var subject = (ComplexSubjectId) actual.getSubject();
            assertThat(subject.getUser().getClass(), equalTo(user.contains("email") ? EmailSubjectId.class : IssuerSubjectId.class));
            assertThat(mapper.readTree(mapper.writeValueAsString(actual)), equalTo(mapper.readTree(json)));
        }
    }

    @Test
    public void preserveCustomSubjectIdentifierAttributes() {
        var mapper = new QuarkusObjectMapperResolver().getContext(AddSubjectRequest.class);
        String json = "{\"subject\":{\"format\":\"custom-format\",\"extension\":{\"values\":[1,true,null]}}}";
        var actual = mapper.readValue(json, AddSubjectRequest.class);

        assertThat(actual.getSubject().getClass(), equalTo(GenericSubjectId.class));
        assertThat(actual.getSubject().getFormat(), equalTo("custom-format"));
        assertThat(mapper.valueToTree(actual.getSubject().getAttributes()),
                equalTo(mapper.readTree("{\"extension\":{\"values\":[1,true,null]}}")));
        assertThat(mapper.readTree(mapper.writeValueAsString(actual)),
                equalTo(mapper.readTree("{\"subject\":{\"format\":\"custom-format\"}}")));
    }

    @Test
    public void omitNullSubjectIdentifier() {
        var mapper = new QuarkusObjectMapperResolver().getContext(AddSubjectRequest.class);
        var actual = mapper.readValue("{\"subject\":null}", AddSubjectRequest.class);

        assertNull(actual.getSubject());
        assertThat(mapper.writeValueAsString(actual), equalTo("{}"));
    }

    @Test
    public void rejectInvalidSubjectIdentifierFormats() {
        var resolver = new QuarkusObjectMapperResolver();
        for (String subject : List.of("{}", "{\"format\":null}", "{\"format\":42}")) {
            String json = "{\"subject\":" + subject + "}";
            assertThrows(tools.jackson.databind.exc.MismatchedInputException.class,
                    () -> resolver.getContext(AddSubjectRequest.class).readValue(json, AddSubjectRequest.class));
        }
    }

}
