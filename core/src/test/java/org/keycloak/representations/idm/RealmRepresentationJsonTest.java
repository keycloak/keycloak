package org.keycloak.representations.idm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RealmRepresentationJsonTest {

    @Test
    public void deviceSettingsRetainJackson2WireNames() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        RealmRepresentation realm = mapper.readValue("{\"oauth2DeviceCodeLifespan\":600,\"oauth2DevicePollingInterval\":5}",
                RealmRepresentation.class);

        assertThat(realm.getOAuth2DeviceCodeLifespan(), equalTo(Integer.valueOf(600)));
        assertThat(realm.getOAuth2DevicePollingInterval(), equalTo(Integer.valueOf(5)));

        JsonNode json = mapper.readTree(mapper.writeValueAsString(realm));
        assertThat(json.path("oauth2DeviceCodeLifespan").asInt(), equalTo(600));
        assertThat(json.path("oauth2DevicePollingInterval").asInt(), equalTo(5));
        assertThat(json.has("OAuth2DeviceCodeLifespan"), is(false));
        assertThat(json.has("OAuth2DevicePollingInterval"), is(false));
    }
}
