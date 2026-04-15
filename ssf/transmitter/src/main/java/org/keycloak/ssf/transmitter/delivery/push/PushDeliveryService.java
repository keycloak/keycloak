package org.keycloak.ssf.transmitter.delivery.push;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.token.SecurityEventToken;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfig;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.StringEntity;
import org.jboss.logging.Logger;

/**
 * Service for delivering events using the PUSH delivery method.
 */
public class PushDeliveryService {

    protected static final Logger log = Logger.getLogger(PushDeliveryService.class);

    private final HttpClient httpClient;

    private final SsfTransmitterConfig transmitterConfig;

    public PushDeliveryService(KeycloakSession session, SsfTransmitterConfig transmitterConfig) {
        // Resolve the shared HttpClient up front so push delivery can later
        // run on an async executor thread that has no live KeycloakSession
        // bound to it any more (the /verify request's session is closed by
        // then). Likewise, transmitterConfig is captured once so delivery
        // doesn't depend on the thread-local session via Ssf.transmitter().
        this.httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        this.transmitterConfig = transmitterConfig;
    }

    /**
     * Delivers an event to a receiver endpoint using the PUSH delivery method.
     *
     * @param stream       The stream configuration
     * @param encodedEvent The event to deliver
     * @return true if the event was delivered successfully, false otherwise
     */
    public boolean deliverEvent(StreamConfig stream, SecurityEventToken eventToken, String encodedEvent) {
        if (stream == null || stream.getDelivery() == null) {
            log.warn("Invalid stream configuration for event delivery");
            return false;
        }

        String endpointUrl = stream.getDelivery().getEndpointUrl();
        String authorizationHeader = stream.getDelivery().getAuthorizationHeader();

        if (endpointUrl == null) {
            log.warn("Missing endpoint URL for stream " + stream.getStreamId());
            return false;
        }

        return deliverEvent(endpointUrl, authorizationHeader, eventToken, encodedEvent, stream);
    }

    /**
     * Delivers an event to a receiver endpoint using the PUSH delivery method.
     *
     * @param endpointUrl         The endpoint URL to deliver the event to
     * @param authorizationHeader The authorization header to use
     * @param eventToken          The event token
     * @param encodedEventToken   The encoded event to deliver
     * @param stream
     * @return true if the event was delivered successfully, false otherwise
     */
    protected boolean deliverEvent(String endpointUrl, String authorizationHeader, SecurityEventToken eventToken, String encodedEventToken, StreamConfig stream) {
        try {

            log.debugf("Delivering event %s to %s. EventToken=%s", eventToken.getJti(), endpointUrl, encodedEventToken);

            try (var response = createSimpleHttp(endpointUrl, authorizationHeader, stream)
                    .header(HttpHeaders.CONTENT_TYPE, Ssf.APPLICATION_SECEVENT_JWT_TYPE)
                    .entity(new StringEntity(encodedEventToken))
                    .asResponse()) {

                boolean success = response.getStatus() == Response.Status.OK.getStatusCode() ||
                                  response.getStatus() == Response.Status.ACCEPTED.getStatusCode();

                if (!success) {
                    String responseString = response.asString();
                    log.warnf("Failed to deliver event %s to url %s. Got status=%s response='%s'",
                            eventToken.getJti(), endpointUrl, response.getStatus(), responseString);
                } else {
                    log.debugf("Delivery of event %s to url %s successful. Got status=%s",
                            eventToken.getJti(), endpointUrl, response.getStatus());
                }

                return success;
            }
        } catch (Exception e) {
            log.errorf(e, "Error delivering event %s to url %s", eventToken.getJti(), endpointUrl);
            return false;
        }
    }

    protected SimpleHttpRequest createSimpleHttp(String endpointUrl, String authorizationHeader, StreamConfig stream) {

        Integer connectRequestTimeout = stream.getPushEndpointConnectTimeoutMillis();
        if (connectRequestTimeout == null) {
            connectRequestTimeout = transmitterConfig.getPushEndpointConnectTimeoutMillis();
        }
        Integer socketTimeout = stream.getPushEndpointSocketTimeoutMillis();
        if (socketTimeout == null) {
            socketTimeout = transmitterConfig.getPushEndpointSocketTimeoutMillis();
        }

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(connectRequestTimeout)
                .setSocketTimeout(socketTimeout)
                .build();
        var httpRequest = SimpleHttp.create(httpClient)
                .withRequestConfig(requestConfig)
                .doPost(endpointUrl);
        if (authorizationHeader != null) {
            // we use the push authorization header as is
            httpRequest.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        return httpRequest;
    }

}
