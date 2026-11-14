package org.keycloak.quarkus.runtime.integration.jaxrs;

import org.keycloak.representations.idm.UserRepresentation;

import org.junit.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class RepresentationAdaptersTest extends org.keycloak.json.jackson3.RepresentationAdaptersTest {

    @Override
    protected JsonMapper mapper() {
        return (JsonMapper) new QuarkusObjectMapperResolver().getContext(Object.class);
    }

    @Test
    public void serverRejectsUnknownRepresentationProperties() {
        assertThrows(UnrecognizedPropertyException.class,
                () -> mapper().readValue("{\"username\":\"alice\",\"unknown\":true}", UserRepresentation.class));
    }

    @Test
    public void serverRejectsScalarForArrayProperties() {
        assertThrows(MismatchedInputException.class,
                () -> mapper().readValue("{\"requiredActions\":\"UPDATE_PASSWORD\"}", UserRepresentation.class));
    }

    @Test
    public void snakeCaseNamingWorksInBothDirections() {
        var value = mapper().readValue("{\"some_value\":\"text\"}", SnakeCaseValue.class);

        assertThat(value.someValue, equalTo("text"));
        assertThat(mapper().writeValueAsString(value), equalTo("{\"some_value\":\"text\"}"));
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class SnakeCaseValue {
        public String someValue;
    }
}
