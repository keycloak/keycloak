package org.keycloak.admin.client.jackson3;

import org.keycloak.representations.idm.UserRepresentation;

import org.junit.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class RepresentationAdaptersTest extends org.keycloak.json.jackson3.RepresentationAdaptersTest {

    @Override
    protected JsonMapper mapper() {
        return Jackson3MapperHolder.MAPPER;
    }

    @Test
    public void publicClientMapperRetainsUnknownPropertyLeniency() {
        var mapper = new Jackson3JsonMapper();
        var user = mapper.readValue("{\"username\":\"alice\",\"unknown\":true}", UserRepresentation.class);

        assertThat(user.getUsername(), equalTo("alice"));
    }
}
