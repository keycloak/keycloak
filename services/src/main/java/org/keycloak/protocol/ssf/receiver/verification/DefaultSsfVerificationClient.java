package org.keycloak.protocol.ssf.receiver.verification;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.registration.SsfReceiverRegistrationProviderConfig.TransmitterTokenType;
import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterMetadata;

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
                receiver.getConfig().getTransmitterToken(),
                receiver.getConfig().getTransmitterTokenType(),
                verificationRequest);
        try (var response = verificationHttpCall.asResponse()) {
            LOG.debugf("Received verification response. status=%s", response.getStatus());

            if (response.getStatus() != 204) {
                throw new SsfStreamVerificationException("Expected a 204 response but got: " + response.getStatus());
            }
        } catch (Exception e) {
            throw new SsfStreamVerificationException("Could not send verification request", e);
        }
    }

    protected SimpleHttpRequest prepareHttpCall(String verifyUri, String token,
                                                TransmitterTokenType transmitterTokenType,
                                                SsfStreamVerificationRequest verificationRequest) {
        SimpleHttpRequest httpRequest = createHttpClient(session).doPost(verifyUri);

        // TODO add support for refresh token type
        switch (transmitterTokenType) {
            case ACCESS_TOKEN -> httpRequest.auth(token);
            default -> throw new SsfStreamVerificationException("Unsupported transmitter token type: " + transmitterTokenType);
        }
        return httpRequest.json(verificationRequest);
    }

    protected SimpleHttp createHttpClient(KeycloakSession session) {
        return SimpleHttp.create(session);
    }
}
