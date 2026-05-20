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
     * @return outcome of the attempt: {@code delivered=true} on 2xx;
     *         {@code httpFailure} carrying status + response body on
     *         non-2xx; {@code transportFailure} carrying the exception
     *         class + message on connection-level failures.
     */
    public PushDeliveryOutcome deliverEvent(StreamConfig stream, SecurityEventToken eventToken, String encodedEvent) {
        if (stream == null || stream.getDelivery() == null) {
            log.warn("Invalid stream configuration for event delivery");
            return PushDeliveryOutcome.invalidConfig("stream or delivery section is null");
        }

        String endpointUrl = stream.getDelivery().getEndpointUrl();
        String authorizationHeader = stream.getDelivery().getAuthorizationHeader();

        if (endpointUrl == null) {
            log.warn("Missing endpoint URL for stream " + stream.getStreamId());
            return PushDeliveryOutcome.invalidConfig("missing endpoint URL");
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
     * @return structured outcome — see {@link PushDeliveryOutcome}.
     */
    protected PushDeliveryOutcome deliverEvent(String endpointUrl, String authorizationHeader, SecurityEventToken eventToken, String encodedEventToken, StreamConfig stream) {
        try {

            if (log.isTraceEnabled()) {
                log.tracef("Delivering event jti=%s to %s. EventToken=%s", eventToken.getJti(), endpointUrl, encodedEventToken);
            } else {
                log.debugf("Delivering event jti=%s to %s.", eventToken.getJti(), endpointUrl);
            }

            try (var response = createSimpleHttp(endpointUrl, authorizationHeader, stream)
                    .header(HttpHeaders.CONTENT_TYPE, Ssf.APPLICATION_SECEVENT_JWT_TYPE)
                    .entity(new StringEntity(encodedEventToken))
                    .asResponse()) {

                // RFC 8935 §2.4: the Event Receiver responds with an HTTP 2xx
                // status code on success — anything in [200, 300) is success;
                // 204 No Content is common (acknowledged but no body).
                int status = response.getStatus();
                boolean success = status >= 200 && status < 300;

                if (!success) {
                    String responseString = response.asString();
                    log.warnf("Failed to deliver event jti=%s to url %s. Got status=%s response='%s'",
                            eventToken.getJti(), endpointUrl, status, responseString);
                    return PushDeliveryOutcome.httpFailure(status, responseString, endpointUrl);
                }

                if (status != Response.Status.OK.getStatusCode()
                        && status != Response.Status.ACCEPTED.getStatusCode()) {
                    // 2xx but not the canonical 200/202 — log at INFO so an
                    // operator sees the receiver's exact status without it
                    // being treated as a failure.
                    log.debugf("Delivery of event jti=%s to url %s accepted with non-canonical 2xx. Got status=%s. events=%s",
                            eventToken.getJti(), endpointUrl, status, eventToken.getEvents());
                } else {
                    log.debugf("Delivery of event jti=%s to url %s successful. Got status=%s. events=%s",
                            eventToken.getJti(), endpointUrl, status, eventToken.getEvents());
                }

                return PushDeliveryOutcome.delivered(status, endpointUrl);
            }
        } catch (Exception e) {
            log.errorf(e, "Error delivering event jti=%s to url %s", eventToken.getJti(), endpointUrl);
            return PushDeliveryOutcome.transportFailure(e, endpointUrl);
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
