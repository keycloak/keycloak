package org.keycloak.tests.admin.client.v2.validation;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.services.error.ViolationExceptionResponse;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

@KeycloakIntegrationTest(config = AbstractClientValidationTest.AdminV2Config.class)
public class PutClientValidationTest extends AbstractClientValidationTest {

    @Override
    public String getHttpMethod() {
        return HttpPut.METHOD_NAME;
    }

    @Override
    public HttpEntityEnclosingRequestBase getRequest(boolean isOidc) {
        var request = new HttpPut(getClientApiUrl(isOidc ? testOidcClient.getClientId() : testSamlClient.getClientId()));
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        return request;
    }

    @Override
    public String getPayloadClientId(boolean isOidc) {
        return isOidc ? testOidcClient.getClientId() : testSamlClient.getClientId();
    }

    @ParameterizedTest
    @ValueSource(strings = {ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID})
    public void putClientWithSecretAuthButMissingSecretFails(String authMethod) throws Exception {
        var clientId = "test-client-secret-validation";
        HttpPut request = new HttpPut(getClientApiUrl(clientId));
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        // PUT client with client-secret-based auth method but no secret
        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "enabled": true,
                    "auth": {
                        "method": "%s"
                    }
                }
                """.formatted(clientId, authMethod)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("auth.secret: must not be blank when authentication method requires a secret"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID})
    public void putClientWithSecretAuthAndBlankSecretFails(String authMethod) throws Exception {
        var clientId = "test-client-blank-secret";
        HttpPut request = new HttpPut(getClientApiUrl(clientId));
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        // PUT client with client-secret-based auth method but blank secret
        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
                    "clientId": "%s",
                    "enabled": true,
                    "auth": {
                        "method": "%s",
                        "secret": "   "
                    }
                }
                """.formatted(clientId, authMethod)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("auth.secret: must not be blank when authentication method requires a secret"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void createClientViaPutFailsUuidValidation(String protocol) throws Exception {
        boolean isOidc = protocol.equals(OIDCClientRepresentation.PROTOCOL);
        String existingUuid = isOidc ? testOidcClient.getId() : testSamlClient.getId();

        // Try to create client with a used UUID (should fail)
        var differentClientId = "test-client-for-create-via-put-%s".formatted(protocol);
        HttpPut updateRequest = new HttpPut(getClientApiUrl(differentClientId));
        setAuthHeader(updateRequest);
        updateRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        updateRequest.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "uuid": "%s",
                    "enabled": true
                }
                """.formatted(protocol, differentClientId, existingUuid)));

        try (var response = client.execute(updateRequest)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("uuid: UUID is server-managed and must not be user-specified"));
        }

        // Try to create client with a different UUID (should pass)
        updateRequest = new HttpPut(getClientApiUrl(differentClientId));
        setAuthHeader(updateRequest);
        updateRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        updateRequest.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "uuid": "550e8400-e29b-41d4-a716-446655440000",
                    "enabled": true
                }
                """.formatted(protocol, differentClientId)));

        try (var response = client.execute(updateRequest)) {
            assertThat(response.getStatusLine().getStatusCode(), is(201));
            var rep = mapper.createParser(response.getEntity().getContent()).readValueAs(BaseClientRepresentation.class);
            assertThat(rep.getUuid(), not(is(existingUuid)));
            assertThat(rep.getUuid(), not(is("550e8400-e29b-41d4-a716-446655440000")));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL})
    public void updateClientWithUuidFailsValidation(String protocol) throws Exception {
        var clientId = "test-client-for-update-%s".formatted(protocol);

        // First create a client without UUID
        HttpPost createRequest = new HttpPost(getClientsApiUrl());
        setAuthHeader(createRequest);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        createRequest.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "enabled": true
                }
                """.formatted(protocol, clientId)));

        String actualUuid;
        try (var createResponse = client.execute(createRequest)) {
            assertThat(createResponse.getStatusLine().getStatusCode(), is(201));
            BaseClientRepresentation rep = mapper.createParser(createResponse.getEntity().getContent()).readValueAs(BaseClientRepresentation.class);
            actualUuid = rep.getUuid();
        }

        // Try to update the client with a UUID (should fail)
        HttpPut updateRequest = new HttpPut(getClientApiUrl(clientId));
        setAuthHeader(updateRequest);
        updateRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        updateRequest.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "uuid": "550e8400-e29b-41d4-a716-446655440000",
                    "enabled": true
                }
                """.formatted(protocol, clientId)));

        try (var response = client.execute(updateRequest)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            assertThat(body.violations(), hasItem("uuid: UUID is server-managed and must not be user-specified"));
        }

        // Update the client with the same UUID (should succeed)
        updateRequest = new HttpPut(getClientApiUrl(clientId));
        setAuthHeader(updateRequest);
        updateRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        updateRequest.setEntity(new StringEntity("""
                {
                    "protocol": "%s",
                    "clientId": "%s",
                    "uuid": "%s",
                    "enabled": true
                }
                """.formatted(protocol, clientId, actualUuid)));

        try (var response = client.execute(updateRequest)) {
            assertThat(response.getStatusLine().getStatusCode(), is(200));
        }
    }
}
