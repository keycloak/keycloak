package org.keycloak.ssf.transmitter.stream.storage.client;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.SsfProfile;
import org.keycloak.ssf.event.SsfEventRegistry;
import org.keycloak.ssf.metadata.DefaultSubjects;
import org.keycloak.ssf.stream.DeliveryMethodFamily;
import org.keycloak.ssf.stream.StreamStatus;
import org.keycloak.ssf.stream.StreamStatusValue;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.stream.SsfEventsConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamDeliveryConfig;
import org.keycloak.ssf.transmitter.stream.StreamVerificationConfig;
import org.keycloak.ssf.transmitter.stream.storage.SsfStreamStore;
import org.keycloak.util.JsonSerialization;
import org.keycloak.vault.VaultStringSecret;

import org.jboss.logging.Logger;

public class ClientStreamStore implements SsfStreamStore {

    protected static final Logger log = Logger.getLogger(ClientStreamStore.class);

    // ----- Receiver-level configuration (survives stream delete) -------------
    public static final String SSF_ENABLED_KEY = "ssf.enabled";
    public static final String SSF_PROFILE_KEY = "ssf.profile";
    public static final String SSF_AUTO_VERIFY_STREAM_KEY = "ssf.autoVerifyStream";
    public static final String SSF_VERIFICATION_DELAY_MILLIS_KEY = "ssf.verificationDelayMillis";
    public static final String SSF_LAST_VERIFIED_AT_KEY = "ssf.lastVerifiedAt";
    public static final String SSF_STREAM_AUDIENCE_KEY = "ssf.streamAudience";
    public static final String SSF_STREAM_SUPPORTED_EVENTS_KEY = "ssf.supportedEvents";

    /**
     * Subset of {@link #SSF_STREAM_SUPPORTED_EVENTS_KEY} that the
     * native event listener will <em>not</em> auto-emit for this
     * receiver. The event types stay in the supported set (so a
     * receiver can still accept them on the wire and an admin can
     * still fire them) but Keycloak's automatic mapping in
     * {@link org.keycloak.ssf.transmitter.event.SsfTransmitterEventListener}
     * skips them — they only flow through the synthetic-emit endpoint.
     *
     * <p>Use case: an SSF receiver advertises {@code CaepSessionRevoked}
     * (Apple School Manager devices need to honour it when fired) but
     * the operator doesn't want every Keycloak-side application logout
     * to translate to a device-level revoke. Adding {@code CaepSessionRevoked}
     * here disables auto-emit while keeping the synthetic-emit path
     * available for the events that should actually trigger a revoke
     * (e.g. an admin-issued logout via a custom integration that calls
     * the emit endpoint).
     *
     * <p>Stored as a comma-separated alias list, same shape as
     * {@link #SSF_STREAM_SUPPORTED_EVENTS_KEY}. Empty/absent =
     * everything supported is auto-emitted (current default behaviour).
     */
    public static final String SSF_EMIT_ONLY_EVENTS_KEY = "ssf.emitOnlyEvents";
    public static final String SSF_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS_KEY = "ssf.pushEndpointConnectTimeoutMillis";
    public static final String SSF_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS_KEY = "ssf.pushEndpointSocketTimeoutMillis";
    public static final String SSF_STREAM_SIGNATURE_ALGORITHM_KEY = "ssf.signatureAlgorithm";
    public static final String SSF_STREAM_USER_SUBJECT_FORMAT_KEY = "ssf.userSubjectFormat";
    public static final String SSF_DEFAULT_SUBJECTS_KEY = "ssf.defaultSubjects";
    public static final String SSF_AUTO_NOTIFY_ON_LOGIN_KEY = "ssf.autoNotifyOnLogin";
    public static final String SSF_REQUIRE_SERVICE_ACCOUNT_KEY = "ssf.requireServiceAccount";
    public static final String SSF_REQUIRED_ROLE_KEY = "ssf.requiredRole";
    public static final String SSF_MIN_VERIFICATION_INTERVAL_KEY = "ssf.minVerificationInterval";
    /**
     * Per-receiver override of the SSF §9.3 grace window. Positive
     * value = receiver-driven {@code subjects/remove} keeps delivering
     * for that many seconds; {@code 0} = take effect immediately;
     * unset = fall back to the transmitter-wide
     * {@code subject-removal-grace-seconds} SPI default.
     */
    public static final String SSF_SUBJECT_REMOVAL_GRACE_SECONDS_KEY = "ssf.subjectRemovalGraceSeconds";
    public static final String SSF_ALLOW_EMIT_EVENTS_KEY = "ssf.allowEmitEvents";
    public static final String SSF_EMIT_EVENTS_ROLE_KEY = "ssf.emitEventsRole";

    /**
     * Per-receiver allow-list of delivery method families this receiver
     * may use at stream-create time. Stored as {@link org.keycloak.models.Constants#CFG_DELIMITER}
     * ({@code ##})-separated list of canonical {@link DeliveryMethodFamily}
     * values ({@code push}, {@code poll}). Empty/absent ⇒ both PUSH and
     * POLL are allowed (transmitter-default behaviour). Set to e.g.
     * {@code poll} to forbid PUSH on this receiver regardless of the
     * receiver's request.
     */
    public static final String SSF_ALLOWED_DELIVERY_METHODS_KEY = "ssf.allowedDeliveryMethods";

    /**
     * Per-receiver allow-list of valid push endpoint URL patterns used
     * as the SSRF gate for receiver-supplied {@code delivery.endpoint_url}
     * on PUSH stream-create. Stored as {@link org.keycloak.models.Constants#CFG_DELIMITER}
     * ({@code ##})-separated entries. Each entry is matched against the
     * receiver-supplied URL using exact match or a trailing-{@code *}
     * suffix wildcard (e.g. {@code https://recv.example.com/feeds/*}).
     * Bare {@code *} entries are rejected at validation time so the SSRF
     * defence cannot be disabled with a single keystroke.
     *
     * <p>Required when the receiver requests PUSH delivery (or the legacy
     * RISC PUSH variant). Ignored for POLL streams — POLL endpoint URLs
     * are transmitter-owned and never receiver-supplied.
     *
     * <p>The match logic mirrors OIDC redirect-URI matching (see
     * {@code RedirectUtils.matchesRedirects} — the implementation is
     * deliberately copied into the SSF support package so the security
     * surface of OIDC isn't touched by this gate).
     */
    public static final String SSF_VALID_PUSH_URLS_KEY = "ssf.validPushUrls";

    /**
     * Per-receiver outbox event TTL in seconds. Any non-{@code DELIVERED}
     * outbox row for this client whose {@code createdAt} is older than
     * {@code now - ssf.maxEventAgeSeconds} is purged by the drainer's
     * housekeeping pass <em>before</em> the global
     * {@code outbox-dead-letter-retention} window applies. Useful for
     * receivers whose events lose relevance fast (e.g. {@code session-revoked}
     * for a session that's already been re-established). Empty/absent =
     * use only the transmitter-wide retention.
     */
    public static final String SSF_MAX_EVENT_AGE_SECONDS_KEY = "ssf.maxEventAgeSeconds";

    /**
     * Per-receiver SSF inactivity timeout in seconds. Empty/absent
     * disables the check. When set, the transmitter automatically
     * pauses the receiver's stream if no eligible activity (any hit
     * on the stream-management endpoints or a POLL for POLL streams)
     * arrives within the window — see SSF 1.0 §8.1.1
     * {@code inactivity_timeout}.
     */
    public static final String SSF_INACTIVITY_TIMEOUT_SECONDS_KEY = "ssf.inactivityTimeoutSeconds";

    /**
     * Coarse-grained activity-slot marker (epoch seconds) for the
     * receiver. Deliberately named "timeslot" rather than "timestamp"
     * because the value is write-coalesced to a configurable
     * granularity (see {@code SsfActivityTracker.STAMP_GRANULARITY_SECONDS})
     * rather than tracking every request — a busy poll receiver
     * shouldn't trigger a DB UPDATE + cluster-wide Infinispan
     * invalidation per call just to bump a timestamp the inactivity
     * check tolerates minutes of lag on. Consumed by the drainer's
     * inactivity-check pass alongside {@link #SSF_INACTIVITY_TIMEOUT_SECONDS_KEY}.
     */
    public static final String SSF_LAST_ACTIVITY_TIMESLOT_KEY = "ssf.lastActivityTimeslot";

    // ----- Per-stream state (cleared on stream delete) -----------------------
    public static final String SSF_STREAM_ID_KEY = "ssf.streamId";
    public static final String SSF_STATUS_KEY = "ssf.status";
    public static final String SSF_STATUS_REASON_KEY = "ssf.status_reason";

    /**
     * Legacy single-blob storage key. Previously the whole {@link StreamConfig}
     * was JSON-serialized into this attribute, which tripped the
     * {@code CLIENT_ATTRIBUTES.VALUE VARCHAR(2048)} column limit on streams
     * with long endpoint URLs or authorization headers. New saves write the
     * split attributes below; reads fall back to this blob if it still exists
     * so pre-refactor data keeps working. {@link #storeStreamConfig} removes
     * it on every save, so the blob is gone for good the first time the stream
     * is updated after the refactor.
     */
    public static final String SSF_STREAM_CONFIG_KEY = "ssf.streamConfig";

    // ----- Per-stream state split across dedicated attributes ----------------
    // Each receiver-writable / transmitter-supplied field gets its own
    // VARCHAR(2048) client attribute. Event sets are stored comma-joined
    // because event-type URIs cannot contain commas.
    public static final String SSF_STREAM_ISSUER_KEY = "ssf.stream.iss";
    public static final String SSF_STREAM_AUDIENCE_STORED_KEY = "ssf.stream.aud";
    public static final String SSF_STREAM_EVENTS_SUPPORTED_KEY = "ssf.stream.eventsSupported";
    public static final String SSF_STREAM_EVENTS_REQUESTED_KEY = "ssf.stream.eventsRequested";
    public static final String SSF_STREAM_EVENTS_DELIVERED_KEY = "ssf.stream.eventsDelivered";
    public static final String SSF_STREAM_DELIVERY_METHOD_KEY = "ssf.stream.delivery.method";
    public static final String SSF_STREAM_DELIVERY_ENDPOINT_URL_KEY = "ssf.stream.delivery.endpointUrl";
    public static final String SSF_STREAM_DELIVERY_AUTHORIZATION_HEADER_KEY = "ssf.stream.delivery.authorizationHeader";
    public static final String SSF_STREAM_DELIVERY_ADDITIONAL_PARAMETERS_KEY = "ssf.stream.delivery.additionalParameters";
    public static final String SSF_STREAM_DESCRIPTION_KEY = "ssf.stream.description";
    public static final String SSF_STREAM_MIN_VERIFICATION_INTERVAL_KEY = "ssf.stream.minVerificationInterval";
    public static final String SSF_STREAM_INACTIVITY_TIMEOUT_KEY = "ssf.stream.inactivityTimeout";
    public static final String SSF_STREAM_FORMAT_KEY = "ssf.stream.format";
    public static final String SSF_STREAM_DEFAULT_SUBJECTS_KEY = "ssf.stream.defaultSubjects";
    public static final String SSF_STREAM_CREATED_AT_KEY = "ssf.stream.createdAt";
    public static final String SSF_STREAM_UPDATED_AT_KEY = "ssf.stream.updatedAt";

    private static final String EVENT_SET_DELIMITER = ",";

    /**
     * Attributes that describe a concrete, registered SSF stream for a
     * receiver client. Deleting the stream clears exactly these attributes.
     * Receiver-level configuration survives a stream delete so the receiver
     * can re-register a new stream with the same admin-configured defaults.
     * The legacy {@link #SSF_STREAM_CONFIG_KEY} blob is included here so any
     * pre-refactor leftover is cleaned up on the next delete.
     */
    public static final Set<String> SSF_STREAM_KEYS = Set.of(
            SSF_STREAM_ID_KEY,
            SSF_STATUS_KEY,
            SSF_STATUS_REASON_KEY,
            SSF_STREAM_CONFIG_KEY,
            SSF_STREAM_ISSUER_KEY,
            SSF_STREAM_AUDIENCE_STORED_KEY,
            SSF_STREAM_EVENTS_SUPPORTED_KEY,
            SSF_STREAM_EVENTS_REQUESTED_KEY,
            SSF_STREAM_EVENTS_DELIVERED_KEY,
            SSF_STREAM_DELIVERY_METHOD_KEY,
            SSF_STREAM_DELIVERY_ENDPOINT_URL_KEY,
            SSF_STREAM_DELIVERY_AUTHORIZATION_HEADER_KEY,
            SSF_STREAM_DELIVERY_ADDITIONAL_PARAMETERS_KEY,
            SSF_STREAM_DESCRIPTION_KEY,
            SSF_STREAM_MIN_VERIFICATION_INTERVAL_KEY,
            SSF_STREAM_INACTIVITY_TIMEOUT_KEY,
            SSF_STREAM_FORMAT_KEY,
            SSF_STREAM_DEFAULT_SUBJECTS_KEY,
            SSF_STREAM_CREATED_AT_KEY,
            SSF_STREAM_UPDATED_AT_KEY);

    protected final KeycloakSession session;

    public ClientStreamStore(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void saveStream(StreamConfig streamConfig) {
        ClientModel client = session.getContext().getClient();
        storeStreamConfig(client, streamConfig);
    }

    @Override
    public StreamStatus updateStreamStatus(String streamId, StreamStatus streamStatus) {
        ClientModel client = session.getContext().getClient();
        StreamConfig streamConfig = extractStreamConfig(client);

        if (streamConfig == null || !streamId.equals(streamConfig.getStreamId())) {
            return null;
        }

        streamConfig.setStatus(StreamStatusValue.valueOf(streamStatus.getStatus()));
        streamConfig.setStatusReason(streamStatus.getReason());
        streamConfig.setUpdatedAt(Time.currentTime());

        // Persist the updated stream config back to the client attribute so
        // subsequent extractStreamConfig calls (e.g. from the dispatcher)
        // observe the new status.
        storeStreamConfig(client, streamConfig);

        String statusReason = streamConfig.getStatusReason();
        StreamStatus status = new StreamStatus();
        status.setStreamId(streamId);
        status.setReason(statusReason);
        status.setStatus(streamConfig.getStatus().getStatusCode());
        return status;
    }

    @Override
    public StreamStatus getStreamStatus(String streamId) {
        ClientModel client = session.getContext().getClient();
        StreamConfig streamConfig = extractStreamConfig(client);

        if (streamConfig == null || !streamId.equals(streamConfig.getStreamId())) {
            return null;
        }

        if (streamConfig.getStatus() == null) {
            return null;
        }

        String statusReason = streamConfig.getStatusReason();
        StreamStatus status = new StreamStatus();
        status.setStreamId(streamId);
        status.setReason(statusReason);
        status.setStatus(streamConfig.getStatus().getStatusCode());
        return status;
    }

    @Override
    public StreamConfig getStream(String streamId) {
        if (streamId == null) {
            return null;
        }
        // Look up the owning client via the SSF_STREAM_ID_KEY index so
        // the call works regardless of session.getContext().getClient()
        // — required by the admin-initiated stream delete path which
        // runs with the admin client in session context, not the
        // receiver. The receiver-initiated path (where session.client
        // IS the receiver) finds the same client via the index.
        ClientModel client = findClientByStreamId(streamId, session.getContext().getRealm()).orElse(null);
        if (client == null) {
            return null;
        }
        StreamConfig streamConfig = extractStreamConfig(client);
        if (streamConfig == null || !streamId.equals(streamConfig.getStreamId())) {
            return null;
        }
        if (!Boolean.parseBoolean(client.getAttribute(SSF_ENABLED_KEY))) {
            return null;
        }
        return streamConfig;
    }

    @Override
    public List<StreamConfig> getAvailableStreams(ClientModel receiverClient) {

        if (receiverClient == null) {
            return List.of();
        }

        StreamConfig streamConfig = extractStreamConfig(receiverClient);

        if (streamConfig == null) {
            return List.of();
        }

        if (!Boolean.parseBoolean(receiverClient.getAttribute("ssf.enabled"))) {
            return List.of();
        }

        return List.of(streamConfig);
    }

    @Override
    public List<StreamConfig> findStreamsForSsfReceiverClients() {
        RealmModel realm = session.getContext().getRealm();
        Map<String, String> attributes = Map.of(SSF_ENABLED_KEY, "true");
        return session.clients()
                .searchClientsByAttributes(realm, attributes, null, null)
                .map(this::extractStreamConfig)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public StreamConfig findStreamById(String streamId) {
        RealmModel realm = session.getContext().getRealm();
        Map<String, String> attributes = Map.of(SSF_STREAM_ID_KEY, streamId);
        return session.clients()
                .searchClientsByAttributes(realm, attributes, 0, 1)
                .map(this::extractStreamConfig)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void deleteStream(String streamId) {
        // Look up the owning client from the stream id rather than
        // pulling it from session context — this method is invoked from
        // both receiver-driven flows (where session.getContext().getClient()
        // is the receiver) and admin-driven flows (where it's the admin
        // client). Searching by SSF_STREAM_ID_KEY makes the call
        // self-contained and safe regardless of caller context.
        if (streamId == null) {
            return;
        }
        ClientModel client = findClientByStreamId(streamId, session.getContext().getRealm()).orElse(null);
        if (client == null) {
            return;
        }
        deleteStreamConfig(client, streamId);
    }

    @Override
    public void recordStreamVerification(String streamId) {
        if (streamId == null) {
            return;
        }
        findClientByStreamId(streamId, session.getContext().getRealm()).ifPresent(this::updateLastVerified);
    }

    protected void updateLastVerified(ClientModel client) {
        client.setAttribute(SSF_LAST_VERIFIED_AT_KEY,String.valueOf(Time.currentTime()));
    }

    protected Optional<ClientModel> findClientByStreamId(String streamId, RealmModel realm) {
        // Query with page size 2 so a collision is detectable — the
        // import-time sanitizer in DefaultSsfTransmitterProviderFactory
        // is the primary defence, but we fail closed here for the
        // scenarios it can't catch (out-of-band attribute edits,
        // concurrent imports, etc.) rather than dispatching events to
        // whichever client the store happens to return first.
        List<ClientModel> matches = session.clients()
                .searchClientsByAttributes(realm, Map.of(SSF_STREAM_ID_KEY, streamId), 0, 2)
                .toList();
        if (matches.size() > 1) {
            log.warnf("Refusing to resolve SSF stream — multiple clients hold ssf.streamId=%s in realm=%s. "
                            + "Matching clientIds: %s. Strip the duplicate via a client update/delete "
                            + "before dispatch can resume for this stream.",
                    streamId, realm.getName(),
                    matches.stream().map(ClientModel::getClientId).toList());
            return Optional.empty();
        }
        return matches.stream().findFirst();
    }

    public StreamConfig getStreamForClient(ClientModel client) {
        return extractStreamConfig(client);
    }

    /**
     * Removes the SSF stream registration (and all associated attributes) from
     * the given client. Returns {@code true} if a stream existed and was
     * removed, {@code false} if no stream was registered for the client.
     */
    public boolean deleteStreamForClient(ClientModel client) {
        StreamConfig streamConfig = extractStreamConfig(client);
        if (streamConfig == null) {
            return false;
        }
        SSF_STREAM_KEYS.forEach(client::removeAttribute);
        return true;
    }

    protected StreamConfig extractStreamConfig(ClientModel client) {
        if (client == null) {
            return null;
        }

        String streamId = client.getAttribute(SSF_STREAM_ID_KEY);
        if (streamId == null) {
            return null;
        }

        // Prefer the legacy single-blob attribute if it is still present so
        // upgrades from pre-refactor installs keep working. The next
        // storeStreamConfig() call will write the split attributes and
        // remove the blob, at which point subsequent reads take the split
        // path below.
        String legacyBlob = client.getAttribute(SSF_STREAM_CONFIG_KEY);
        StreamConfig streamConfig = legacyBlob != null
                ? extractFromLegacyBlob(client, streamId, legacyBlob)
                : extractFromSplitAttributes(client, streamId);
        if (streamConfig == null) {
            return null;
        }

        // Stamp the receiver client's Keycloak id on every read so the
        // dispatcher / outbox can identify the owning client without a
        // second lookup. Not persisted, not echoed on the wire — covered
        // by @JsonIgnore on the field.
        streamConfig.setClientId(client.getId());
        streamConfig.setClientClientId(client.getClientId());

        applyReceiverAttributeOverlays(client, streamConfig);
        return streamConfig;
    }

    private StreamConfig extractFromLegacyBlob(ClientModel client, String streamId, String legacyBlob) {
        try {
            return JsonSerialization.readValue(legacyBlob, StreamConfig.class);
        } catch (IOException e) {
            log.errorf(e,
                    "Failed to deserialize legacy stream configuration blob. clientId=%s streamId=%s",
                    client.getClientId(), streamId);
            return null;
        }
    }

    private StreamConfig extractFromSplitAttributes(ClientModel client, String streamId) {
        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setStreamId(streamId);
        streamConfig.setIssuer(client.getAttribute(SSF_STREAM_ISSUER_KEY));
        streamConfig.setAudience(splitSet(client.getAttribute(SSF_STREAM_AUDIENCE_STORED_KEY)));
        streamConfig.setEventsSupported(splitAsEventTypeUris(client.getAttribute(SSF_STREAM_EVENTS_SUPPORTED_KEY)));
        streamConfig.setEventsRequested(splitAsEventTypeUris(client.getAttribute(SSF_STREAM_EVENTS_REQUESTED_KEY)));
        streamConfig.setEventsDelivered(splitAsEventTypeUris(client.getAttribute(SSF_STREAM_EVENTS_DELIVERED_KEY)));
        streamConfig.setDescription(client.getAttribute(SSF_STREAM_DESCRIPTION_KEY));
        streamConfig.setMinVerificationInterval(parseIntAttribute(client, SSF_STREAM_MIN_VERIFICATION_INTERVAL_KEY));
        streamConfig.setInactivityTimeout(parseIntAttribute(client, SSF_STREAM_INACTIVITY_TIMEOUT_KEY));
        streamConfig.setFormat(client.getAttribute(SSF_STREAM_FORMAT_KEY));
        streamConfig.setDefaultSubjects(DefaultSubjects.parseOrDefault(
                client.getAttribute(SSF_STREAM_DEFAULT_SUBJECTS_KEY), null));
        streamConfig.setCreatedAt(parseIntAttribute(client, SSF_STREAM_CREATED_AT_KEY));
        streamConfig.setUpdatedAt(parseIntAttribute(client, SSF_STREAM_UPDATED_AT_KEY));

        String deliveryMethod = client.getAttribute(SSF_STREAM_DELIVERY_METHOD_KEY);
        if (deliveryMethod != null) {
            StreamDeliveryConfig delivery = new StreamDeliveryConfig();
            delivery.setMethod(deliveryMethod);
            delivery.setEndpointUrl(client.getAttribute(SSF_STREAM_DELIVERY_ENDPOINT_URL_KEY));
            // Mirror the IdP clientSecret pattern: the attribute is stored as
            // entered (literal token or ${vault.x} expression) and resolved
            // through VaultTranscriber at read time so operators can keep the
            // raw secret out of the DB.
            String rawAuthorizationHeader = client.getAttribute(SSF_STREAM_DELIVERY_AUTHORIZATION_HEADER_KEY);
            if (rawAuthorizationHeader != null) {
                try (VaultStringSecret vaulted = session.vault().getStringSecret(rawAuthorizationHeader)) {
                    delivery.setAuthorizationHeader(vaulted.get().orElse(rawAuthorizationHeader));
                }
            }
            String additionalRaw = client.getAttribute(SSF_STREAM_DELIVERY_ADDITIONAL_PARAMETERS_KEY);
            if (additionalRaw != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> additionalParameters =
                            JsonSerialization.readValue(additionalRaw, Map.class);
                    delivery.setAdditionalParameters(additionalParameters);
                } catch (IOException e) {
                    log.warnf(e,
                            "Failed to deserialize delivery.additional_parameters; ignoring. clientId=%s streamId=%s",
                            client.getClientId(), streamId);
                }
            }
            streamConfig.setDelivery(delivery);
        }

        String statusValue = client.getAttribute(SSF_STATUS_KEY);
        if (statusValue != null) {
            try {
                streamConfig.setStatus(StreamStatusValue.valueOf(statusValue));
            } catch (IllegalArgumentException e) {
                log.warnf("Unknown stream status '%s' on client %s; leaving unset",
                        statusValue, client.getClientId());
            }
        }
        streamConfig.setStatusReason(client.getAttribute(SSF_STATUS_REASON_KEY));

        return streamConfig;
    }

    private void applyReceiverAttributeOverlays(ClientModel client, StreamConfig streamConfig) {
        // Receiver-level defaults that the admin configures as client
        // attributes. These overlay whatever came out of the per-stream
        // state so the dispatcher sees the effective values at emission time.
        if (client.getAttribute(SSF_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS_KEY) != null) {
            streamConfig.setPushEndpointConnectTimeoutMillis(
                    Integer.parseInt(client.getAttribute(SSF_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS_KEY)));
        }
        if (client.getAttribute(SSF_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS_KEY) != null) {
            streamConfig.setPushEndpointSocketTimeoutMillis(
                    Integer.parseInt(client.getAttribute(SSF_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS_KEY)));
        }
        if (client.getAttribute(SSF_PROFILE_KEY) != null) {
            streamConfig.setProfile(SsfProfile.valueOf(client.getAttribute(SSF_PROFILE_KEY)));
        }
        String signatureAlgorithm = client.getAttribute(SSF_STREAM_SIGNATURE_ALGORITHM_KEY);
        if (signatureAlgorithm != null && !signatureAlgorithm.isBlank()) {
            streamConfig.setSignatureAlgorithm(signatureAlgorithm);
        }
        String userSubjectFormat = client.getAttribute(SSF_STREAM_USER_SUBJECT_FORMAT_KEY);
        if (userSubjectFormat != null && !userSubjectFormat.isBlank()) {
            streamConfig.setUserSubjectFormat(userSubjectFormat);
        }
        if (streamConfig.getDefaultSubjects() == null) {
            DefaultSubjects clientDefault = DefaultSubjects.parseOrDefault(
                    client.getAttribute(SSF_DEFAULT_SUBJECTS_KEY), null);
            if (clientDefault != null) {
                streamConfig.setDefaultSubjects(clientDefault);
            }
        }
        // Client-level minVerificationInterval is an override, not a
        // fallback: the stream creation stamps the transmitter default
        // (e.g. 60s) eagerly, so the stream value is rarely null. The
        // admin explicitly setting a per-client value means "I want this
        // client's rate limit to differ from the transmitter default."
        Integer clientMinVerification = parseIntAttribute(client, SSF_MIN_VERIFICATION_INTERVAL_KEY);
        if (clientMinVerification != null) {
            streamConfig.setMinVerificationInterval(clientMinVerification);
        }
        // SSF 1.0 §8.1.1 inactivity_timeout — receiver-configured per
        // client. Populated on read so the receiver sees the effective
        // value in the stream config response; the drainer's
        // inactivity-check pass reads the same attribute directly.
        Integer clientInactivityTimeout = parseIntAttribute(client, SSF_INACTIVITY_TIMEOUT_SECONDS_KEY);
        if (clientInactivityTimeout != null) {
            streamConfig.setInactivityTimeout(clientInactivityTimeout);
        }
        // SSF §9.3 per-receiver grace override. Stored as the raw
        // integer; the dispatcher's filter prefers this over the
        // transmitter-wide default. Null = inherit transmitter default.
        Integer clientSubjectRemovalGrace = parseIntAttribute(client, SSF_SUBJECT_REMOVAL_GRACE_SECONDS_KEY);
        if (clientSubjectRemovalGrace != null) {
            streamConfig.setSubjectRemovalGraceSeconds(clientSubjectRemovalGrace);
        }

        // Per-receiver auto-emit blocklist. Aliases are resolved to
        // canonical event-type URIs through the registry so the
        // listener-side comparison can match against the URI it gets
        // off the SET. Unknown aliases are silently dropped — same
        // forgiveness pattern as supportedEvents above.
        String emitOnly = client.getAttribute(SSF_EMIT_ONLY_EVENTS_KEY);
        if (emitOnly != null && !emitOnly.isBlank()) {
            SsfEventRegistry registry = Ssf.events().getRegistry();
            Set<String> resolved = new TreeSet<>();
            for (String candidate : SsfEventRegistry.parseEventTypeAliases(emitOnly)) {
                String eventType = registry.resolveEventTypeForAlias(candidate);
                if (eventType == null && registry.getEventClassByType(candidate).isPresent()) {
                    eventType = candidate;
                }
                if (eventType != null) {
                    resolved.add(eventType);
                }
            }
            if (!resolved.isEmpty()) {
                streamConfig.setEmitOnlyEvents(resolved);
            }
        }
    }

    protected void storeStreamConfig(ClientModel client, StreamConfig streamConfig) {

        // Remove any pre-refactor single-blob leftover so extractStreamConfig
        // takes the split-attribute path on subsequent reads instead of the
        // legacy fallback. This is what actually migrates a stream off the
        // old storage shape — it happens on the first update after upgrade.
        client.removeAttribute(SSF_STREAM_CONFIG_KEY);

        client.setAttribute(SSF_STREAM_ID_KEY, streamConfig.getStreamId());

        StreamStatusValue status = streamConfig.getStatus();
        if (status == null) {
            status = StreamStatusValue.enabled;
        }
        client.setAttribute(SSF_STATUS_KEY, status.name());
        setOrRemove(client, SSF_STATUS_REASON_KEY, streamConfig.getStatusReason());

        setOrRemove(client, SSF_STREAM_ISSUER_KEY, streamConfig.getIssuer());
        setOrRemove(client, SSF_STREAM_AUDIENCE_STORED_KEY, joinSet(streamConfig.getAudience()));
        setOrRemove(client, SSF_STREAM_EVENTS_SUPPORTED_KEY, joinAsAliases(streamConfig.getEventsSupported()));
        setOrRemove(client, SSF_STREAM_EVENTS_REQUESTED_KEY, joinAsAliases(streamConfig.getEventsRequested()));
        setOrRemove(client, SSF_STREAM_EVENTS_DELIVERED_KEY, joinAsAliases(streamConfig.getEventsDelivered()));
        setOrRemove(client, SSF_STREAM_DESCRIPTION_KEY, streamConfig.getDescription());
        setIntOrRemove(client, SSF_STREAM_MIN_VERIFICATION_INTERVAL_KEY, streamConfig.getMinVerificationInterval());
        setIntOrRemove(client, SSF_STREAM_INACTIVITY_TIMEOUT_KEY, streamConfig.getInactivityTimeout());
        setOrRemove(client, SSF_STREAM_FORMAT_KEY, streamConfig.getFormat());
        setOrRemove(client, SSF_STREAM_DEFAULT_SUBJECTS_KEY,
                streamConfig.getDefaultSubjects() != null ? streamConfig.getDefaultSubjects().name() : null);
        setIntOrRemove(client, SSF_STREAM_CREATED_AT_KEY, streamConfig.getCreatedAt());
        setIntOrRemove(client, SSF_STREAM_UPDATED_AT_KEY, streamConfig.getUpdatedAt());

        StreamDeliveryConfig delivery = streamConfig.getDelivery();
        if (delivery != null) {
            setOrRemove(client, SSF_STREAM_DELIVERY_METHOD_KEY, delivery.getMethod());
            setOrRemove(client, SSF_STREAM_DELIVERY_ENDPOINT_URL_KEY, delivery.getEndpointUrl());
            // Preserve an admin's vault externalization across a no-op
            // round-trip from the receiver. The receiver sees the resolved
            // value on read (see applyDeliveryConfig); if it echoes that
            // value back unchanged, keep the existing ${vault.x} attribute
            // rather than overwriting it with the resolved plaintext. Only
            // an actual change to the secret rewrites the attribute.
            String incomingAuthorizationHeader = delivery.getAuthorizationHeader();
            String existingRawAuthorizationHeader = client.getAttribute(SSF_STREAM_DELIVERY_AUTHORIZATION_HEADER_KEY);
            if (incomingAuthorizationHeader != null
                    && existingRawAuthorizationHeader != null
                    && !existingRawAuthorizationHeader.equals(incomingAuthorizationHeader)) {
                try (VaultStringSecret vaulted = session.vault().getStringSecret(existingRawAuthorizationHeader)) {
                    if (vaulted.get().filter(incomingAuthorizationHeader::equals).isPresent()) {
                        incomingAuthorizationHeader = existingRawAuthorizationHeader;
                    }
                }
            }
            setOrRemove(client, SSF_STREAM_DELIVERY_AUTHORIZATION_HEADER_KEY, incomingAuthorizationHeader);
            Map<String, Object> additionalParameters = delivery.getAdditionalParameters();
            if (additionalParameters != null && !additionalParameters.isEmpty()) {
                client.setAttribute(SSF_STREAM_DELIVERY_ADDITIONAL_PARAMETERS_KEY,
                        JsonSerialization.valueAsString(additionalParameters));
            } else {
                client.removeAttribute(SSF_STREAM_DELIVERY_ADDITIONAL_PARAMETERS_KEY);
            }
        } else {
            client.removeAttribute(SSF_STREAM_DELIVERY_METHOD_KEY);
            client.removeAttribute(SSF_STREAM_DELIVERY_ENDPOINT_URL_KEY);
            client.removeAttribute(SSF_STREAM_DELIVERY_AUTHORIZATION_HEADER_KEY);
            client.removeAttribute(SSF_STREAM_DELIVERY_ADDITIONAL_PARAMETERS_KEY);
        }
    }

    private static void setOrRemove(ClientModel client, String key, String value) {
        if (value == null) {
            client.removeAttribute(key);
        } else {
            client.setAttribute(key, value);
        }
    }

    private static void setIntOrRemove(ClientModel client, String key, Integer value) {
        if (value == null) {
            client.removeAttribute(key);
        } else {
            client.setAttribute(key, Integer.toString(value));
        }
    }

    private static Integer parseIntAttribute(ClientModel client, String key) {
        String raw = client.getAttribute(key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException e) {
            log.warnf("Invalid numeric client attribute %s='%s' on %s; ignoring",
                    key, raw, client.getClientId());
            return null;
        }
    }

    private static String joinSet(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return null;
        }
        return String.join(EVENT_SET_DELIMITER, set);
    }

    private static Set<String> splitSet(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        // Preserve insertion order so a round-trip through storage keeps
        // the same JSON shape.
        Set<String> result = new LinkedHashSet<>();
        for (String entry : value.split(EVENT_SET_DELIMITER)) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Storage-side compaction for the three per-stream event sets
     * ({@code ssf.stream.eventsSupported}, {@code ssf.stream.eventsRequested},
     * {@code ssf.stream.eventsDelivered}). The in-memory and wire shapes hold
     * canonical event-type URIs (e.g.
     * {@code https://schemas.openid.net/secevent/caep/event-type/credential-change}),
     * but those URIs are ~80 chars each and a stream may carry up to
     * {@code MAX_EVENTS_REQUESTED_COUNT}=32 of them across the three sets.
     * Storing them verbatim in client-attribute columns burns space the
     * total-blob cap ({@link #MAX_STREAM_CONFIG_JSON_BYTES}) would otherwise
     * spend on something useful.
     *
     * <p>This helper converts each URI to its short alias (e.g.
     * {@code CaepCredentialChange}, ~30 chars) when the event registry knows
     * one. Custom event types not registered by any
     * {@link org.keycloak.ssf.event.SsfEventProviderFactory} fall through
     * unchanged. {@code joinAsAliases} on save and {@link #splitAsEventTypeUris}
     * on load are inverse — read-side tolerates both forms so receiver
     * attributes that pre-date this change continue to load correctly,
     * and the next save migrates them to the alias form.
     */
    protected String joinAsAliases(Set<String> eventTypeUris) {
        if (eventTypeUris == null || eventTypeUris.isEmpty()) {
            return null;
        }
        SsfEventRegistry registry = Ssf.events().getRegistry();
        Set<String> compact = new LinkedHashSet<>(eventTypeUris.size());
        for (String entry : eventTypeUris) {
            if (entry == null) {
                continue;
            }
            String alias = registry != null ? registry.resolveAliasForEventType(entry) : null;
            compact.add(alias != null ? alias : entry);
        }
        return joinSet(compact);
    }

    /**
     * Inverse of {@link #joinAsAliases}: splits the stored string and
     * resolves each entry back to its canonical event-type URI when it
     * matches a known alias. Entries that don't match a known alias pass
     * through unchanged — covers two cases at once:
     * <ol>
     *     <li>Legacy URI-form entries written before the alias compaction
     *         landed; they're already in canonical form, no resolution
     *         needed.</li>
     *     <li>Custom event types whose provider factory did not register
     *         an alias; the URI is the canonical name.</li>
     * </ol>
     */
    protected Set<String> splitAsEventTypeUris(String stored) {
        Set<String> entries = splitSet(stored);
        if (entries == null) {
            return null;
        }
        SsfEventRegistry registry = Ssf.events().getRegistry();
        if (registry == null) {
            return entries;
        }
        Set<String> resolved = new LinkedHashSet<>(entries.size());
        for (String entry : entries) {
            String canonical = registry.resolveEventTypeForAlias(entry);
            resolved.add(canonical != null ? canonical : entry);
        }
        return resolved;
    }

    protected void deleteStreamConfig(ClientModel client, String streamId) {
        StreamConfig streamConfig = extractStreamConfig(client);
        if (streamConfig != null && streamConfig.getStreamId().equals(streamId)) {
            SSF_STREAM_KEYS.forEach(client::removeAttribute);
        }
    }

    @Override
    public StreamVerificationConfig getStreamVerificationConfig(String streamId, ClientModel client) {
        return new StreamVerificationConfig(isAutoVerifyStream(client), getVerificationDelayMillis(client));
    }

    protected boolean isAutoVerifyStream(ClientModel client) {
        return Boolean.parseBoolean(client.getAttribute(SSF_AUTO_VERIFY_STREAM_KEY));
    }

    protected int getVerificationDelayMillis(ClientModel client) {
        if (client.getAttribute(SSF_VERIFICATION_DELAY_MILLIS_KEY) != null) {
            return Integer.parseInt(client.getAttribute(SSF_VERIFICATION_DELAY_MILLIS_KEY));
        }
        // Fallback to the transmitter-wide default configured via SPI
        return session.getProvider(SsfTransmitterProvider.class).getConfig().getTransmitterInitiatedVerificationDelayMillis();
    }

    @Override
    public SsfEventsConfig getEventsConfig(ClientModel client, Set<String> eventsRequested) {

        Set<String> supportedEvents = getSupportedEvents(session, client);

        // events_delivered = events_requested ∩ events_supported, but the two
        // sides aren't directly comparable: supportedEvents is canonical URIs
        // (resolved in getSupportedEvents from the stored aliases), while
        // eventsRequested may be either aliases (admin UI sends those) or
        // canonical URIs (what an external receiver sends per spec). Resolve
        // each requested entry through the registry, then keep the receiver's
        // original form in the delivered set so events_requested and
        // events_delivered display consistently in the admin UI.
        SsfEventRegistry registry = Ssf.events().getRegistry();
        Set<String> eventsDelivered = new TreeSet<>();
        if (eventsRequested != null) {
            for (String requested : eventsRequested) {
                if (requested == null) {
                    continue;
                }
                String canonical = registry.resolveEventTypeForAlias(requested);
                if (canonical == null && registry.getEventClassByType(requested).isPresent()) {
                    canonical = requested;
                }
                if (canonical != null && supportedEvents.contains(canonical)) {
                    eventsDelivered.add(requested);
                }
            }
        }

        return new SsfEventsConfig(supportedEvents, eventsRequested, eventsDelivered);
    }

    protected Set<String> getSupportedEvents(KeycloakSession session, ClientModel client) {

        SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
        SsfEventRegistry registry = Ssf.events().getRegistry();

        String supportedEventsAttribute = client.getAttribute(SSF_STREAM_SUPPORTED_EVENTS_KEY);
        if (supportedEventsAttribute == null) {

            return transmitter.getDefaultSupportedEvents();
        }

        // The admin UI stores event aliases (e.g. "CaepCredentialChange") in the
        // client attribute; the SSF stream configuration however MUST carry the
        // full event type URIs as defined by the SSF/CAEP/RISC specs. Resolve
        // each candidate (alias or URI) to its canonical event type URI and
        // drop any candidates the transmitter does not know about.
        Set<String> supportedEvents = new TreeSet<>();
        Set<String> supportedEventCandidates = SsfEventRegistry.parseEventTypeAliases(supportedEventsAttribute);
        for (String supportedEventCandidate : supportedEventCandidates) {

            String eventType = registry.resolveEventTypeForAlias(supportedEventCandidate);
            if (eventType == null && registry.getEventClassByType(supportedEventCandidate).isPresent()) {
                eventType = supportedEventCandidate;
            }
            if (eventType == null) {
                continue;
            }
            supportedEvents.add(eventType);
        }
        return supportedEvents;
    }
}
