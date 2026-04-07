package org.keycloak.tests.admin.client.v2.validation;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

@KeycloakIntegrationTest(config = AbstractClientValidationTest.AdminV2Config.class)
public class PostClientValidationTest extends AbstractClientValidationTest {

    @Override
    public String getHttpMethod() {
        return HttpPost.METHOD_NAME;
    }

    @Override
    public HttpEntityEnclosingRequestBase getRequest(boolean isOidc) {
        var request = new HttpPost(getClientsApiUrl());
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        return request;
    }

    @Override
    public String getPayloadClientId(boolean isOidc) {
        return isOidc ? "something-random-oidc" : "something-random-saml";
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void createClientWithUuidPassesValidation(String protocol) throws Exception {
        HttpPost request = new HttpPost(getClientsApiUrl());
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        final String uuid = "550e8400-e29b-41d4-a716-446655440000";
        request.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "test-with-uuid-%s",
                    "uuid": "%s",
                    "enabled": true
                }
                """.formatted(protocol, protocol, uuid)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(201));

            var rep = mapper.createParser(response.getEntity().getContent()).readValueAs(BaseClientRepresentation.class);
            assertThat(rep.getUuid(), not(is(uuid)));
        }
    }

    @Test
    @Override
    @Disabled("Only for PUT/PATCH")
    public void clientIdMismatchBetweenPathAndPayloadFails(String protocol){
    }

    @Test
    @Override
    @Disabled("Only for PUT/PATCH")
    public void clientWithTypeMismatchFails(String protocol){
    }
}
