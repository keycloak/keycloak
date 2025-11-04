package org.keycloak.protocol.ssf.receiver.streamclient;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.stream.CreateStreamRequest;
import org.keycloak.protocol.ssf.stream.SsfStreamRepresentation;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Map;

public class DefaultSsfStreamClient implements SsfStreamClient {

    protected static final Logger log = Logger.getLogger(DefaultSsfStreamClient.class);

    protected final KeycloakSession session;

    public DefaultSsfStreamClient(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public SsfStreamRepresentation createStream(
            SsfTransmitterMetadata transmitterMetadata,
            String transmitterAccessToken,
            CreateStreamRequest createStreamRequest) {

        try {
            log.debugf("Sending stream creation request. %s", JsonSerialization.writeValueAsPrettyString(createStreamRequest));
        } catch (IOException ioe) {
            throw new SsfStreamException("Could not serialize stream creation request", ioe, Response.Status.INTERNAL_SERVER_ERROR);
        }
        String uri = transmitterMetadata.getConfigurationEndpoint();
        var httpCall = createHttpClient(session).doPost(uri).auth(transmitterAccessToken).json(createStreamRequest);
        try (var response = httpCall.asResponse()) {
            log.debugf("Stream creation response. status=%s", response.getStatus());

            if (response.getStatus() != 201) {
                log.errorf("Stream creation failed. %s", response.asJson(Map.class));
                throw new SsfStreamException("Expected a 201 response but got: " + response.getStatus(), Response.Status.fromStatusCode(response.getStatus()));
            }

            return response.asJson(SsfStreamRepresentation.class);
        } catch (IOException ioe) {
            throw new SsfStreamException("I/O error during stream creation", ioe, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteStream(SsfTransmitterMetadata transmitterMetadata, String transmitterAccessToken, String streamId) {

        RealmModel realm = session.getContext().getRealm();
        log.debugf("Sending stream deletion request. realm=%s stream_id=%s", realm.getName(), streamId);

        String uri = transmitterMetadata.getConfigurationEndpoint() + "?stream_id=" + streamId;
        var httpCall = createHttpClient(session).doDelete(uri).auth(transmitterAccessToken);
        try (var response = httpCall.asResponse()) {
            log.debugf("Stream deletion response. status=%s", response.getStatus());

            if (response.getStatus() != 204) {
                log.errorf("Stream deletion failed. realm=%s stream_id=%s error='%s'", realm.getName(), streamId, response.asJson(Map.class));
                throw new SsfStreamException("Expected a 204 response but got: " + response.getStatus(), Response.Status.fromStatusCode(response.getStatus()));
            }
        } catch (Exception e) {
            throw new SsfStreamException("Could not send stream deletion request", e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public SsfStreamRepresentation getStream(SsfTransmitterMetadata transmitterMetadata, String transmitterAccessToken, String streamId) {

        RealmModel realm = session.getContext().getRealm();
        log.debugf("Sending stream read request. realm=%s stream_id=%s", realm.getName(), streamId);

        String uri = transmitterMetadata.getConfigurationEndpoint() + "?stream_id=" + streamId;
        var httpCall = createHttpClient(session).doGet(uri).auth(transmitterAccessToken);
        try (var response = httpCall.asResponse()) {
            log.debugf("Stream read response. status=%s", response.getStatus());

            if (response.getStatus() != 200) {
                log.errorf("Stream read request failed. realm=%s stream_id=%s error='%s'", realm.getName(), streamId, response.asJson(Map.class));
                throw new SsfStreamException("Expected a 200 response but got: " + response.getStatus(), Response.Status.fromStatusCode(response.getStatus()));
            }

            return response.asJson(SsfStreamRepresentation.class);
        } catch (Exception e) {
            throw new SsfStreamException("Could not send stream read request", e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    protected SimpleHttp createHttpClient(KeycloakSession session) {
        return SimpleHttp.create(session);
    }

}
