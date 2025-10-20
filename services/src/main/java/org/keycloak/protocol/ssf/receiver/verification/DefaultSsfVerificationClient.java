package org.keycloak.protocol.ssf.receiver.verification;

import org.jboss.logging.Logger;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.receiver.ReceiverModel;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;

public class DefaultSsfVerificationClient implements SsfVerificationClient {

    protected static final Logger log = Logger.getLogger(DefaultSsfVerificationClient.class);

    protected final KeycloakSession session;

    public DefaultSsfVerificationClient(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void requestVerification(ReceiverModel model, SsfTransmitterMetadata metadata, String state) {

        var verificationRequest = new VerificationRequest();
        verificationRequest.setStreamId(model.getStreamId());
        verificationRequest.setState(state);

        log.debugf("Sending verification request to %s. %s", metadata.getVerificationEndpoint(), verificationRequest);
        var verificationHttpCall = prepareHttpCall(metadata.getVerificationEndpoint(), model.getTransmitterAccessToken(), verificationRequest);
        try (var response = verificationHttpCall.asResponse()) {
            log.debugf("Received verification response. status=%s", response.getStatus());

            if (response.getStatus() != 204) {
                throw new SsfStreamVerificationException("Expected a 204 response but got: " + response.getStatus());
            }
        } catch (Exception e) {
            throw new SsfStreamVerificationException("Could not send verification request", e);
        }
    }

    protected SimpleHttpRequest prepareHttpCall(String verifyUri, String token, VerificationRequest verificationRequest) {
        return createHttpClient(session).doPost(verifyUri).auth(token).json(verificationRequest);
    }

    protected SimpleHttp createHttpClient(KeycloakSession session) {
        return SimpleHttp.create(session);
    }
}
