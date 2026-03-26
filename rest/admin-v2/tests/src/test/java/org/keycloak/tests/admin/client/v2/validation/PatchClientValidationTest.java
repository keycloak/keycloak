package org.keycloak.tests.admin.client.v2.validation;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.services.PatchTypeNames;
import org.keycloak.services.error.ViolationExceptionResponse;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@KeycloakIntegrationTest(config = AbstractClientValidationTest.AdminV2Config.class)
public class PatchClientValidationTest extends AbstractClientValidationTest {

    @Override
    public String getHttpMethod() {
        return HttpPatch.METHOD_NAME;
    }

    @Override
    public HttpEntityEnclosingRequestBase getRequest(boolean isOidc) {
        var request = new HttpPatch(getClientApiUrl(isOidc ? testOidcClient.getClientId() : testSamlClient.getClientId()));
        request.setHeader(HttpHeaders.CONTENT_TYPE, PatchTypeNames.JSON_MERGE);
        return request;
    }

    @Override
    public String getPayloadClientId(boolean isOidc) {
        return isOidc ? testOidcClient.getClientId() : testSamlClient.getClientId();
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void patchClientWithUuidFailsValidation(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        String existingUuid = isOidc ? testOidcClient.getId() : testSamlClient.getId();

        // Try to patch the client with a UUID (should fail)
        var patchRequest = new HttpPatch(getClientApiUrl(getPayloadClientId(isOidc)));
        setAuthHeader(patchRequest);
        patchRequest.setHeader(HttpHeaders.CONTENT_TYPE, PatchTypeNames.JSON_MERGE);

        patchRequest.setEntity(new StringEntity("""
                {
                    "uuid": "550e8400-e29b-41d4-a716-446655440000",
                    "displayName": "Updated Name"
                }
                """));

        try (var response = client.execute(patchRequest)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("uuid: UUID is server-managed and must not be user-specified"));
        }

        // Patch the client with the same UUID (should succeed)
        patchRequest.setEntity(new StringEntity("""
                {
                    "uuid": "%s",
                    "displayName": "Updated Name"
                }
                """.formatted(existingUuid)));

        try (var response = client.execute(patchRequest)) {
            assertThat(response.getStatusLine().getStatusCode(), is(200));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void patchClientWithNullEntityFails(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        var patchRequest = getRequest(isOidc);
        setAuthHeader(patchRequest);
        patchRequest.setEntity(new StringEntity(""));

        try (var response = client.execute(patchRequest)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            String responseBody = EntityUtils.toString(response.getEntity());
            assertThat(responseBody, containsString("Cannot replace client resource with null"));
        }
    }

    @Test
    @Override
    @Disabled("Only for PUT/POST")
    public void validSAMLClientSucceeds() {
    }

    @Test
    @Override
    @Disabled("Only for PUT/POST")
    public void validOIDCClientSucceeds() {
    }
}
