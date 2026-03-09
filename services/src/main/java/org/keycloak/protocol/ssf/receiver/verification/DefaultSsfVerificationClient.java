package org.keycloak.protocol.ssf.receiver.verification;

import java.io.IOException;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.registration.SsfReceiverRegistrationProviderConfig;
import org.keycloak.protocol.ssf.receiver.registration.SsfReceiverRegistrationProviderConfig.TransmitterAuthMethod;
import org.keycloak.protocol.ssf.transmitter.metadata.SsfTransmitterMetadata;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.vault.VaultStringSecret;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

public class DefaultSsfVerificationClient implements SsfVerificationClient {

    protected static final Logger LOG = Logger.getLogger(DefaultSsfVerificationClient.class);

    protected final KeycloakSession session;

    public DefaultSsfVerificationClient(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void requestVerification(SsfReceiver receiver, SsfTransmitterMetadata metadata, String state) {

        var verificationRequest = new SsfStreamVerificationRequest();
        verificationRequest.setStreamId(receiver.getConfig().getStreamId());
        verificationRequest.setState(state);

        LOG.debugf("Sending verification request to %s. %s", metadata.getVerificationEndpoint(), verificationRequest);
        var verificationHttpCall = prepareHttpCall(metadata.getVerificationEndpoint(),
                receiver.getConfig(),
                verificationRequest);
        try (var response = verificationHttpCall.asResponse()) {
            LOG.debugf("Received verification response. status=%s", response.getStatus());

            if (response.getStatus() != 204) {
                throw new SsfStreamVerificationException("Expected a 204 response but got: " + response.getStatus());
            }
        } catch (SsfStreamVerificationException e) {
            throw e;
        } catch (Exception e) {
            throw new SsfStreamVerificationException("Could not send verification request", e);
        }
    }

    protected SimpleHttpRequest prepareHttpCall(String verifyUri,
                                                SsfReceiverRegistrationProviderConfig config,
                                                SsfStreamVerificationRequest verificationRequest) {
        SimpleHttpRequest httpRequest = createHttpClient(session).doPost(verifyUri);

        TransmitterAuthMethod authMethod = config.getTransmitterAuthMethod();
        switch (authMethod) {
            case STATIC_TOKEN -> authenticateWithStaticToken(httpRequest, config);
            case CLIENT_CREDENTIALS -> authenticateWithClientCredentials(httpRequest, config);
            default -> throw new SsfStreamVerificationException("Unsupported transmitter auth method: " + authMethod);
        }

        return httpRequest.json(verificationRequest);
    }

    protected void authenticateWithStaticToken(SimpleHttpRequest httpRequest,
                                               SsfReceiverRegistrationProviderConfig config) {
        String token = config.getTransmitterToken();
        switch (config.getTransmitterTokenType()) {
            case ACCESS_TOKEN -> httpRequest.auth(token);
            default ->
                    throw new SsfStreamVerificationException("Unsupported transmitter token type: " + config.getTransmitterTokenType());
        }
    }

    protected void authenticateWithClientCredentials(SimpleHttpRequest httpRequest,
                                                     SsfReceiverRegistrationProviderConfig config) {
        String tokenUrl = config.getTokenUrl();
        String clientId = config.getClientId();
        String clientAuthMethod = config.getClientAuthMethod();

        try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(config.getClientSecret())) {
            String clientSecret = vaultStringSecret.get().orElse(config.getClientSecret());

            SimpleHttpRequest tokenRequest = createHttpClient(session).doPost(tokenUrl)
                    .param("grant_type", "client_credentials");

            String scope = config.getScope();
            if (scope != null && !scope.isBlank()) {
                tokenRequest.param("scope", scope);
            }

            if ("client_secret_basic".equals(clientAuthMethod)) {
                String header = BasicAuthHelper.RFC6749.createHeader(clientId, clientSecret);
                tokenRequest.header(HttpHeaders.AUTHORIZATION, header);
            } else {
                // client_secret_post (default)
                tokenRequest.param("client_id", clientId);
                tokenRequest.param("client_secret", clientSecret);
            }

            try (var tokenResponse = tokenRequest.asResponse()) {
                if (tokenResponse.getStatus() != 200) {
                    throw new SsfStreamVerificationException(
                            "Failed to obtain access token via client_credentials grant. Status: " + tokenResponse.getStatus());
                }

                JsonNode responseJson = tokenResponse.asJson();
                if (!responseJson.has("access_token")) {
                    throw new SsfStreamVerificationException(
                            "Failed to obtain access token via client_credentials grant. Missing 'access_token' field in response.");
                }
                String accessToken = responseJson.get("access_token").asText();
                httpRequest.auth(accessToken);
            }
        } catch (SsfStreamVerificationException e) {
            throw e;
        } catch (IOException e) {
            throw new SsfStreamVerificationException("Failed to obtain access token via client_credentials grant", e);
        }
    }

    protected SimpleHttp createHttpClient(KeycloakSession session) {
        return SimpleHttp.create(session);
    }
}
