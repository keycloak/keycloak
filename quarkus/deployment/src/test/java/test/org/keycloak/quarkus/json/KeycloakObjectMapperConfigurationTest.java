package test.org.keycloak.quarkus.json;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.keycloak.representations.AccessTokenResponse;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
@TestProfile(KeycloakObjectMapperConfigurationTest.LenientTrailingTokensProfile.class)
public class KeycloakObjectMapperConfigurationTest {

    @Inject
    JsonMapper mapper;

    @Test
    public void honorQuarkusTrailingTokensConfiguration() {
        var response = mapper.readValue("{\"access_token\":\"access-token\"} {}", AccessTokenResponse.class);

        assertThat(response.getToken(), equalTo("access-token"));
    }

    public static class LenientTrailingTokensProfile extends KeycloakObjectMapperTest.MapperProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            var config = new HashMap<>(super.getConfigOverrides());
            config.put("quarkus.jackson.fail-on-trailing-tokens", "false");
            return config;
        }
    }
}
