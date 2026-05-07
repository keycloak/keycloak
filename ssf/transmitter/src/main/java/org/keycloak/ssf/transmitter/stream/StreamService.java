package org.keycloak.ssf.transmitter.stream;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.keycloak.common.util.Time;
import org.keycloak.events.outbox.OutboxStore;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.SsfException;
import org.keycloak.ssf.SsfProfile;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.metadata.TransmitterMetadata;
import org.keycloak.ssf.stream.DeliveryMethod;
import org.keycloak.ssf.stream.DeliveryMethodFamily;
import org.keycloak.ssf.stream.StreamStatus;
import org.keycloak.ssf.stream.StreamStatusValue;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.event.SsfSignatureAlgorithms;
import org.keycloak.ssf.transmitter.event.SsfUserSubjectFormats;
import org.keycloak.ssf.transmitter.metadata.TransmitterMetadataService;
import org.keycloak.ssf.transmitter.metrics.SsfMetricsBinder;
import org.keycloak.ssf.transmitter.outbox.SsfOutboxKinds;
import org.keycloak.ssf.transmitter.stream.storage.SsfStreamStore;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.support.SsfPushUrlValidator;
import org.keycloak.ssf.transmitter.support.SsfPushUrlValidator.SsfPushUrlValidationException;
import org.keycloak.ssf.transmitter.support.SsfTransmitterUrls;
import org.keycloak.utils.KeycloakSessionUtil;

import org.jboss.logging.Logger;

/**
 * Service for managing SSF streams.
 */
public class StreamService {

    protected static final Logger log = Logger.getLogger(StreamService.class);

    /**
     * Length caps for receiver-supplied fields on {@code /streams}
     * POST/PATCH/PUT. Chosen so that even a maximally-populated stream fits
     * inside the client attribute value column with a safety margin — see
     * {@link ClientStreamStore#MAX_STREAM_CONFIG_JSON_BYTES} for the overall
     * total-size guard. Violations produce {@link SsfException} → HTTP 400.
     */
    protected static final int MAX_DESCRIPTION_LENGTH = 255;
    protected static final int MAX_EVENTS_REQUESTED_COUNT = 32;
    protected static final int MAX_EVENT_TYPE_LENGTH = 256;
    protected static final int MAX_DELIVERY_ENDPOINT_URL_LENGTH = 512;
    protected static final int MAX_DELIVERY_AUTHORIZATION_HEADER_LENGTH = 1024;
    protected static final int MAX_DELIVERY_ADDITIONAL_PARAMETERS_COUNT = 8;
    protected static final int MAX_DELIVERY_ADDITIONAL_PARAMETER_KEY_LENGTH = 64;
    protected static final int MAX_DELIVERY_ADDITIONAL_PARAMETER_VALUE_LENGTH = 256;

    protected final KeycloakSession session;

    protected final SsfTransmitterProvider transmitterProvider;

    protected final SsfStreamStore streamStore;

    protected final TransmitterMetadataService transmitterService;

    protected final StreamVerificationService streamVerificationService;

    protected final Function<KeycloakSession, OutboxStore> outboxStoreFactory;

    public StreamService(KeycloakSession session,
                         SsfTransmitterProvider transmitterProvider,
                         SsfStreamStore streamStore,
                         TransmitterMetadataService transmitterService,
                         StreamVerificationService streamVerificationService,
                         Function<KeycloakSession, OutboxStore> outboxStoreFactory) {
        this.session = session;
        this.transmitterProvider = transmitterProvider;
        this.streamStore = streamStore;
        this.transmitterService = transmitterService;
        this.streamVerificationService = streamVerificationService;
        this.outboxStoreFactory = outboxStoreFactory;
    }

    /**
     * Creates a new stream from a receiver-supplied {@link StreamConfigInputRepresentation}
     * request body (SSF spec §8.1.1.2).
     *
     * <p>Transmitter-controlled fields ({@code stream_id}, {@code iss},
     * {@code aud}, {@code events_supported}, {@code events_delivered}, the
     * {@code kc_*} extensions, …) are computed here and are intentionally
     * absent from {@link StreamConfigUpdateRepresentation}/{@link StreamConfigInputRepresentation} so a
     * receiver cannot supply them over the wire: Jackson rejects unknown
     * fields with 400 at bind time.
     *
     * @param input The receiver-supplied create request.
     * @return The created stream configuration.
     */
    public StreamConfig createStream(StreamConfigInputRepresentation input, ClientModel receiverClient) {
        return createStream(receiverClient, input, false);
    }

    /**
     * Admin-initiated create path: same flow as {@link #createStream(StreamConfigInputRepresentation)}
     * but explicitly associated with a receiver client that the admin
     * selected from the admin UI, and trusted to set transmitter-supplied
     * fields ({@code iss}/{@code aud}/{@code format}) regardless of the
     * receiver client's SSF profile.
     *
     * <p>Temporarily rewrites {@code session.getContext().getClient()} to
     * the target receiver so the downstream code (and
     * {@link ClientStreamStore}, which pulls the client from session
     * context) sees the correct client for attribute reads and the
     * duplicate-stream guard. Restored in a finally so the admin's own
     * session context isn't left pointing at the receiver.
     */
    public StreamConfig createStreamAsAdmin(StreamConfigInputRepresentation input, ClientModel receiverClient) {
        ClientModel previousClient = session.getContext().getClient();
        session.getContext().setClient(receiverClient);
        try {
            return createStream(receiverClient, input, true);
        } finally {
            session.getContext().setClient(previousClient);
        }
    }

    protected StreamConfig createStream(ClientModel receiverClient, StreamConfigInputRepresentation input, boolean adminInitiated) {

        SsfProfile profile = resolveReceiverProfile(receiverClient);

        // iss / aud / format are transmitter-supplied under SSF 1.0 §8.1.1 and
        // MUST NOT be set by a receiver. The legacy SSE CAEP profile (Apple
        // Business Manager / Apple School Manager) does allow receivers to
        // include them in the create body, so we only reject when the
        // receiver client is not on that profile. Admin-initiated creates
        // skip this check — the admin is trusted to set any field on any
        // profile from the admin UI's create-stream form.
        if (!adminInitiated) {
            validateLegacyFieldsForProfile(input, profile);
        }

        StreamConfig streamConfig = new StreamConfig();
        // Receiver-writable fields first so validate() sees the delivery
        // configuration the receiver actually supplied.
        replaceReceiverFields(input, streamConfig);

        // Cheap, side-effect-free input/URL validation up front so a receiver
        // with a misconfigured push URL gets the actionable 400 even if it
        // also has an existing stream that would fail the duplicate guard
        // below — the receiver fixes the URL before fighting the duplicate.
        // Also keeps "rejection means no client-state mutation" a guarantee:
        // applyLegacyFields below (SSE_CAEP profile only) writes a client
        // attribute, and we don't want a rejected request to leave that
        // attribute behind.
        validate(streamConfig, receiverClient);

        // Stateful checks AFTER input is known good.
        checkClient(receiverClient);
        applyLegacyFields(input, streamConfig, receiverClient, profile);

        // Transmitter-supplied identity fields.
        streamConfig.setStreamId(createStreamId(session, streamConfig));
        streamConfig.setStatus(StreamStatusValue.enabled);
        // iss is always the transmitter's own issuer — any receiver-supplied
        // value was already rejected for SSF 1.0 above and is intentionally
        // ignored for SSE_CAEP (Apple echoes it from discovery, so overwriting
        // is a no-op in practice).
        streamConfig.setIssuer(transmitterService.getTransmitterMetadata().getIssuer());

        // For poll delivery, the endpoint_url is transmitter-owned per
        // SSF §6.1.2. Now that stream_id is assigned, build the URL
        // and overwrite whatever the receiver supplied (or didn't
        // supply) on input.
        finalizePollEndpointUrlIfApplicable(streamConfig, receiverClient);

        streamConfig.setAudience(createAudience(input, streamConfig, receiverClient));

        Set<String> eventsRequested = streamConfig.getEventsRequested();

        // Compute delivered events based on requested events
        SsfEventsConfig eventsConfig = streamStore.getEventsConfig(receiverClient, eventsRequested);
        streamConfig.setEventsDelivered(eventsConfig.eventsDelivered());
        // Return supported events
        streamConfig.setEventsSupported(eventsConfig.eventsSupported());

        // Set timestamps
        int now = Time.currentTime();
        streamConfig.setCreatedAt(now);
        streamConfig.setUpdatedAt(now);

        streamConfig.setMinVerificationInterval(transmitterProvider.getConfig().getMinVerificationIntervalSeconds());

        applySignatureAlgorithmFromClient(streamConfig, receiverClient);
        applyUserSubjectFormatFromClient(streamConfig, receiverClient);

        streamConfig.setStatus(StreamStatusValue.enabled);

        // Store the stream configuration
        streamStore.saveStream(streamConfig);

        log.debugf("Stream created. realm=%s client=%s streamId=%s",
                session.getContext().getRealm().getName(), receiverClient.getClientId(), streamConfig.getStreamId());

        StreamVerificationConfig streamVerificationConfig = streamStore.getStreamVerificationConfig(streamConfig.getStreamId(), receiverClient);
        if (streamVerificationConfig.autoVerifyStream()) {
            scheduleTransmitterInitiatedAsyncStreamVerification(streamConfig, streamVerificationConfig, session);
        }

        return resolveStreamForResponse(streamConfig);
    }

    /**
     * Reads the stream back through the store so the response carries
     * the overlays that {@code applyReceiverAttributeOverlays} layers
     * on top of the persisted per-stream state — e.g. the client-level
     * {@code ssf.minVerificationInterval} override, {@code ssf.inactivityTimeoutSeconds},
     * and the signature-algorithm / user-subject-format attributes.
     * The in-memory draft we just saved does not carry those; without
     * this re-read the receiver's POST/PATCH/PUT response would show
     * the transmitter defaults even when the admin has pinned different
     * values on the receiver client.
     */
    protected StreamConfig resolveStreamForResponse(StreamConfig savedDraft) {
        StreamConfig resolved = streamStore.getStream(savedDraft.getStreamId());
        return resolved != null ? resolved : savedDraft;
    }

    /**
     * Computes the {@code aud} for a freshly created stream using a three-tier
     * priority:
     * <ol>
     *   <li>Admin-configured {@link ClientStreamStore#SSF_STREAM_AUDIENCE_KEY
     *       ssf.streamAudience} client attribute — always wins so the admin
     *       can pin a realm-specific audience regardless of receiver input.</li>
     *   <li>Receiver-supplied {@code aud} from the create body — only honored
     *       when it arrives, intended for legacy SSE CAEP receivers (Apple
     *       Business Manager) that carry their feed URL in the request. The
     *       profile gate on {@link #validateLegacyFieldsForProfile} already
     *       rejects this tier for SSF 1.0 receivers.</li>
     *   <li>Transmitter-generated default {@code clientId/streamId}.</li>
     * </ol>
     */
    protected Set<String> createAudience(StreamConfigInputRepresentation input,
                                         StreamConfig streamConfig,
                                         ClientModel receiverClient) {
        String ssfClientAudience = receiverClient.getAttribute(ClientStreamStore.SSF_STREAM_AUDIENCE_KEY);
        if (ssfClientAudience != null && !ssfClientAudience.isBlank()) {
            Set<String> audience = new HashSet<>();
            audience.add(ssfClientAudience);
            return audience;
        }
        if (input.getAudience() != null && !input.getAudience().isEmpty()) {
            return new HashSet<>(input.getAudience());
        }
        Set<String> audience = new HashSet<>();
        audience.add(receiverClient.getClientId() + "/" + streamConfig.getStreamId());
        return audience;
    }

    /**
     * Reads the receiver client's {@link ClientStreamStore#SSF_PROFILE_KEY
     * ssf.profile} attribute and returns the corresponding {@link SsfProfile}.
     * Defaults to {@link SsfProfile#SSF_1_0 SSF_1_0} when unset or invalid so
     * the strict legacy-field gate applies by default.
     */
    protected SsfProfile resolveReceiverProfile(ClientModel receiverClient) {
        String profileValue = receiverClient.getAttribute(ClientStreamStore.SSF_PROFILE_KEY);
        if (profileValue == null || profileValue.isBlank()) {
            return SsfProfile.SSF_1_0;
        }
        try {
            return SsfProfile.valueOf(profileValue);
        } catch (IllegalArgumentException e) {
            log.warnf("Unknown ssf.profile '%s' defaulting to SSF_1_0. realm=%s client=%s",
                    profileValue,
                    session.getContext().getRealm().getName(),
                    receiverClient.getClientId());
            return SsfProfile.SSF_1_0;
        }
    }

    /**
     * Rejects a create/update/replace body that carries {@code iss},
     * {@code aud}, or {@code format} when the receiver client is not on the
     * legacy SSE CAEP profile. Per SSF 1.0 §8.1.1 those fields are
     * transmitter-supplied and MUST NOT be set by the receiver; the SSE CAEP
     * profile relaxes this specifically so Apple Business Manager's
     * round-tripping-the-metadata create flow keeps working.
     */
    protected void validateLegacyFieldsForProfile(StreamConfigInputRepresentation input, SsfProfile profile) {
        if (profile == SsfProfile.SSE_CAEP) {
            return;
        }
        List<String> offending = new ArrayList<>(3);
        if (input.getIssuer() != null) {
            offending.add("iss");
        }
        if (input.getAudience() != null) {
            offending.add("aud");
        }
        if (input.getFormat() != null) {
            offending.add("format");
        }
        if (!offending.isEmpty()) {
            throw new SsfException("Invalid stream configuration: " + offending
                    + " are transmitter-supplied under SSF 1.0 §8.1.1 and may only be set by"
                    + " receivers configured with the SSE_CAEP profile");
        }
    }

    /**
     * Applies the receiver-supplied legacy fields onto {@code target} for
     * receivers on the SSE CAEP profile. {@code iss} and {@code aud} are
     * still owned by the transmitter — iss is ignored here and aud is
     * wired through {@link #createAudience} — so this method carries
     * {@code format} onto the stored stream so CAEP receivers can
     * round-trip it in their GET response <em>and</em> mirrors it onto
     * the receiver client's
     * {@link ClientStreamStore#SSF_STREAM_USER_SUBJECT_FORMAT_KEY
     * ssf.userSubjectFormat} attribute.
     *
     * <p>The mirror is the important part: SSE CAEP's {@code format} and
     * Keycloak's {@code ssf.userSubjectFormat} use identical code strings
     * ({@code iss_sub}, {@code email}, …), so persisting the receiver's
     * choice onto the client attribute means
     * {@link #applyUserSubjectFormatFromClient} (which runs later in the
     * same create/update flow) picks it up and the dispatcher uses the
     * receiver-requested subject format at SET emission time. Without the
     * mirror, Apple would ask for {@code iss_sub} and we'd still emit
     * events using whatever subject format the admin had preconfigured
     * (or the transmitter-wide default) — a silent mismatch.
     */
    protected void applyLegacyFields(StreamConfigInputRepresentation input,
                                     StreamConfig target,
                                     ClientModel receiverClient,
                                     SsfProfile profile) {
        if (profile != SsfProfile.SSE_CAEP) {
            return;
        }
        String format = input.getFormat();
        if (format == null) {
            return;
        }
        // Validate against the allow-list so an unsupported format surfaces
        // as a clean 400 at stream create/update time rather than a silent
        // drop later at SET emission.
        if (!SsfUserSubjectFormats.isAllowed(format)) {
            throw new SsfException("Invalid stream configuration: format '" + format
                    + "' is not in the transmitter allow-list " + SsfUserSubjectFormats.ALLOWED);
        }
        target.setFormat(format);
        receiverClient.setAttribute(ClientStreamStore.SSF_STREAM_USER_SUBJECT_FORMAT_KEY, format);
    }

    protected void checkClient(ClientModel receiverClient) {
        List<StreamConfig> availableStreams = streamStore.getAvailableStreams(receiverClient);
        if (availableStreams != null && !availableStreams.isEmpty()) {
            throw new DuplicateStreamConfigException("Only one stream per receiver is allowed");
        }
    }

    protected String createStreamId(KeycloakSession session, StreamConfig streamConfig) {
        return UUID.randomUUID().toString();
    }

    protected void scheduleTransmitterInitiatedAsyncStreamVerification(StreamConfig streamConfig, StreamVerificationConfig streamVerificationConfig, KeycloakSession session) {
        StreamVerificationRequest verificationRequest = new StreamVerificationRequest();
        verificationRequest.setStreamId(streamConfig.getStreamId());
        // If the Verification Event is initiated by the Transmitter then this parameter MUST not be set.
        // https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-8.1.4.2-5
        // verificationRequest.setState(UUID.randomUUID().toString());

        TransmitterMetadata transmitterMetadata = transmitterProvider.metadataService().getTransmitterMetadata();

        String realmId = session.getContext().getRealm().getId();
        String realmName = session.getContext().getRealm().getName();
        String clientId = session.getContext().getClient().getClientId();

        log.debugf("Scheduling Verification request after stream creation. realm=%s client=%s streamId=%s",
                realmName, clientId, streamConfig.getStreamId());

        HttpRequest httpRequest = session.getContext().getHttpRequest();
        KeycloakSessionFactory keycloakSessionFactory = session.getKeycloakSessionFactory();

        // Hand the verification dispatch to Keycloak's managed executor pool (ExecutorsProvider)
        // The delay exists so the receiver's POST /streams response is delivered
        // before the verification SET arrives on its push endpoint.
        // CompletableFuture.delayedExecutor handles the wait via the JDK's
        // shared timer without holding a worker thread, then forwards the
        // task to the managed pool when it's due.
        ExecutorService verificationPool = session.getProvider(ExecutorsProvider.class)
                .getExecutor("ssf-stream-verification");
        int delay = streamVerificationConfig.verificationDelayMillis();
        CompletableFuture.runAsync(() -> {
            try (KeycloakSession subSession = keycloakSessionFactory.create()) {
                subSession.getTransactionManager().begin();
                subSession.setAttribute("ssfTransmitterMetadata", transmitterMetadata);

                RealmModel realm = subSession.realms().getRealm(realmId);
                ClientModel clientById = realm.getClientByClientId(clientId);
                subSession.getContext().setRealm(realm);
                subSession.getContext().setClient(clientById);
                subSession.getContext().setHttpRequest(httpRequest);

                KeycloakSessionUtil.setKeycloakSession(subSession);
                try {
                    log.debugf("Sending transmitter initiated Verification request after stream creation. realm=%s client=%s streamId=%s",
                            realmName, clientId, streamConfig.getStreamId());
                    boolean verificationSent = triggerTransmitterInitiatedStreamVerification(streamConfig, subSession, verificationRequest);
                    if (verificationSent) {
                        log.debugf("Verification transmitter initiated request sent after stream creation. realm=%s client=%s streamId=%s",
                                realmName, clientId, streamConfig.getStreamId());
                    }
                    subSession.getTransactionManager().commit();
                } finally {
                    KeycloakSessionUtil.setKeycloakSession(null);
                }

            } catch (Exception e) {
                log.errorf(e, "Failed to send transmitter initiated verification request after stream creation. realm=%s client=%s streamId=%s",
                        realmName, clientId, streamConfig.getStreamId());
            }
        }, CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS, verificationPool));
    }

    protected boolean triggerTransmitterInitiatedStreamVerification(StreamConfig streamConfig, KeycloakSession subSession, StreamVerificationRequest verificationRequest) {
        var ssfProvider = subSession.getProvider(SsfTransmitterProvider.class);
        return ssfProvider.verificationService().triggerVerification(verificationRequest,
                SsfMetricsBinder.VerificationInitiator.TRANSMITTER);
    }

    /**
     * Applies the receiver-writable fields of {@code input} to
     * {@code target} using §8.1.1.3 merge semantics: only non-null fields
     * are copied; null fields leave the corresponding value on
     * {@code target} untouched.
     *
     * <p>Java beans collapse "field absent from the body" and "field present
     * and explicitly null" into the same state, so we define a null in a
     * PATCH body as "don't change this". To explicitly clear a
     * receiver-writable field a receiver must use PUT (full replace) instead.
     */
    protected void mergeReceiverFields(StreamConfigInputRepresentation input, StreamConfig target) {
        if (input.getDescription() != null) {
            target.setDescription(input.getDescription());
        }
        if (input.getEventsRequested() != null) {
            target.setEventsRequested(input.getEventsRequested());
        }
        if (input.getDelivery() != null) {
            target.setDelivery(input.getDelivery());
        }
    }

    /**
     * Applies the receiver-writable fields of {@code input} to
     * {@code target} using replace semantics: all receiver-writable fields
     * are unconditionally copied, so absent fields on {@code input} reset
     * the corresponding value on {@code target} to {@code null}.
     *
     * <p>Transmitter-controlled fields on {@code target} ({@code stream_id},
     * {@code iss}, {@code aud}, {@code events_supported}, the {@code kc_*}
     * extensions, …) are left untouched — this method only replaces the
     * receiver-writable subset. Used for both {@code POST /streams} (apply
     * onto a fresh {@link StreamConfig}) and {@code PUT /streams} (apply
     * onto the stored {@link StreamConfig}).
     */
    protected void replaceReceiverFields(StreamConfigInputRepresentation input, StreamConfig target) {
        target.setDescription(input.getDescription());
        target.setEventsRequested(input.getEventsRequested());
        target.setDelivery(input.getDelivery());
    }

    protected void validate(StreamConfig streamConfig, ClientModel receiverClient) {

        StreamDeliveryConfig delivery = streamConfig.getDelivery();
        if (delivery == null) {
            throw new SsfException("Invalid stream configuration: missing delivery configuration");
        }

        if (delivery.getMethod() == null) {
            throw new SsfException("Invalid stream configuration: missing delivery method");
        }

        // Reject the legacy RISC variants up front when SSE CAEP support
        // is disabled — keeps the accepted surface aligned with what's
        // advertised in delivery_methods_supported.
        boolean sseCaepEnabled = transmitterProvider.getConfig().isSseCaepEnabled();
        if (!sseCaepEnabled
                && (Ssf.DELIVERY_METHOD_RISC_PUSH_URI.equals(delivery.getMethod())
                    || Ssf.DELIVERY_METHOD_RISC_POLL_URI.equals(delivery.getMethod()))) {
            throw new SsfException("Invalid stream configuration: SSE CAEP delivery methods are disabled on this transmitter");
        }

        validateAllowedDeliveryMethod(delivery, receiverClient);
        validateDeliveryMethod(streamConfig, delivery, receiverClient);

        validateFieldConstraints(streamConfig);
    }

    /**
     * Per-receiver allow-list gate on the delivery family. Reads
     * {@link ClientStreamStore#SSF_ALLOWED_DELIVERY_METHODS_KEY} off the
     * receiver client; when set, the receiver-requested method's family
     * ({@link DeliveryMethodFamily#PUSH}/{@link DeliveryMethodFamily#POLL}) MUST be
     * present in the allow-list. Empty/absent attribute means "both
     * allowed" (transmitter-default behaviour preserved).
     */
    protected void validateAllowedDeliveryMethod(StreamDeliveryConfig delivery, ClientModel receiverClient) {
        if (receiverClient == null) {
            return;
        }
        String raw = receiverClient.getAttribute(ClientStreamStore.SSF_ALLOWED_DELIVERY_METHODS_KEY);
        if (raw == null || raw.isBlank()) {
            return;
        }
        Set<DeliveryMethodFamily> allowed = new HashSet<>();
        for (String token : Constants.CFG_DELIMITER_PATTERN.split(raw)) {
            DeliveryMethodFamily family = DeliveryMethodFamily.ofMethodValue(token);
            if (family != null) {
                allowed.add(family);
            }
        }
        if (allowed.isEmpty()) {
            // The attribute is set but parses to no recognised family.
            // Treat as misconfiguration rather than silently falling
            // through to the default allow-both — admins who set the
            // attribute meant to restrict, not to allow more.
            throw new SsfException("Invalid stream configuration: ssf.allowedDeliveryMethods is configured but contains"
                    + " no recognised delivery family (expected push and/or poll)");
        }
        DeliveryMethod method;
        try {
            method = DeliveryMethod.valueOfUri(delivery.getMethod());
        } catch (IllegalArgumentException e) {
            // Unknown method URI is rejected later by validateDeliveryMethod
            // with its own clear message; nothing to add here.
            return;
        }
        DeliveryMethodFamily requested = method.family();
        if (!allowed.contains(requested)) {
            throw new SsfException("Invalid stream configuration: delivery method '"
                                   + requested.getValue() + "' is not allowed for this receiver"
                                   + " (ssf.allowedDeliveryMethods = " + allowed.stream().map(DeliveryMethodFamily::getValue).sorted().toList() + ")");
        }
    }

    protected void validateDeliveryMethod(StreamConfig streamConfig, StreamDeliveryConfig delivery, ClientModel receiverClient) {
        switch (delivery.getMethod()) {
            case Ssf.DELIVERY_METHOD_PUSH_URI, Ssf.DELIVERY_METHOD_RISC_PUSH_URI -> {
                if (delivery.getEndpointUrl() == null) {
                    throw new SsfException("Invalid stream configuration: missing delivery endpoint push URL");
                }
                validatePushEndpointUrl(delivery.getEndpointUrl());
                validatePushEndpointAgainstReceiverAllowList(delivery.getEndpointUrl(), receiverClient);
            }
            case Ssf.DELIVERY_METHOD_POLL_URI, Ssf.DELIVERY_METHOD_RISC_POLL_URI -> {
                // Poll endpoint_url is owned by the transmitter per SSF
                // §6.1.2 ("This is specified by the Transmitter") — any
                // receiver-supplied value is ignored on input and
                // overwritten with the transmitter-generated URL after
                // stream_id is assigned (see finalizePollEndpointUrl in
                // the create path). We accept the field on input rather
                // than rejecting it so receivers that echo back a
                // previously-saved stream config don't get a 400.
            }
            default -> throw new SsfException("Invalid stream configuration: unsupported delivery method");
        }
    }

    /**
     * SSRF gate for receiver-supplied push URLs: matches the URL against
     * the receiver client's {@code ssf.validPushUrls} allow-list and runs
     * the post-match scheme/host check (see {@link SsfPushUrlValidator}).
     * Rejection produces a {@link SsfException} → HTTP 400 with a stable
     * message identifying the failing rule.
     *
     * <p>The error returned to the receiver is deliberately generic — it
     * does not echo the rejected URL or suggest an allow-list value, to
     * avoid leaking diagnostic detail to a potentially malicious caller.
     * Operators get the full picture in the server log: the URL the
     * receiver tried to register and a suggested {@code ssf.validPushUrls}
     * entry derived from it. With that information an operator can update
     * the client attribute in one step and the receiver can retry.
     */
    protected void validatePushEndpointAgainstReceiverAllowList(String endpointUrl, ClientModel receiverClient) {
        if (receiverClient == null) {
            // No client context (admin-initiated path that didn't thread
            // it through, defensive) — fall back to the structural URL
            // check only. Should not happen on the receiver-facing path.
            return;
        }
        Set<String> validPushUrls = readValidPushUrls(
                receiverClient.getAttribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY));
        try {
            transmitterProvider.pushUrlValidator().validate(endpointUrl, validPushUrls);
        } catch (SsfPushUrlValidationException e) {
            logPushUrlRejection(endpointUrl, receiverClient, e);
            throw e;
        }
    }

    /**
     * Emits a WARN with the rejected URL and a suggested allow-list entry
     * so operators can fix the {@code ssf.validPushUrls} client attribute
     * without having to ask the receiver what URL it tried. The receiver-
     * facing 400 stays generic (see
     * {@link #validatePushEndpointAgainstReceiverAllowList}); this log line
     * is the operator's side of the same failure.
     */
    protected void logPushUrlRejection(String endpointUrl,
                                       ClientModel receiverClient,
                                       SsfPushUrlValidationException cause) {
        String suggestion = suggestValidPushUrlEntry(endpointUrl);
        RealmModel realm = session.getContext().getRealm();
        log.warnf("SSF push URL rejected (%s). realm=%s client=%s rejectedUrl=%s suggestedAllowListEntry=%s",
                cause.getReason(),
                realm != null ? realm.getName() : "(unknown)",
                receiverClient.getClientId(),
                endpointUrl,
                suggestion);
    }

    /**
     * Derives a conservative {@code ssf.validPushUrls} entry that would
     * have allowed the rejected URL. Uses {@code scheme://host[:port]/*}
     * — pins the origin and lets the receiver vary the path. Operators
     * who need a tighter pin (per-tenant prefix, exact URL) can adjust
     * after seeing the suggestion. Returns the input verbatim if it
     * doesn't parse as a URI.
     */
    protected String suggestValidPushUrlEntry(String endpointUrl) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            return "(none — URL was missing)";
        }
        URI uri;
        try {
            uri = new URI(endpointUrl);
        } catch (URISyntaxException e) {
            return endpointUrl;
        }
        if (uri.getScheme() == null || uri.getHost() == null) {
            return endpointUrl;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(uri.getScheme()).append("://");
        sb.append(uri.getHost());
        if (uri.getPort() != -1) {
            sb.append(':').append(uri.getPort());
        }
        sb.append("/*");
        return sb.toString();
    }

    /**
     * Splits a raw {@code ssf.validPushUrls}-shaped client-attribute value
     * on the {@link Constants#CFG_DELIMITER} delimiter, in the same shape
     * as {@code SecureClientUrisExecutor#getAttributeMultivalued}. Trims
     * whitespace, drops empty entries, preserves insertion order; {@code null}
     * or blank input yields an empty set.
     */
    protected Set<String> readValidPushUrls(String rawAttributeValue) {
        if (rawAttributeValue == null || rawAttributeValue.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> entries = new LinkedHashSet<>();
        for (String entry : Constants.CFG_DELIMITER_PATTERN.split(rawAttributeValue)) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                entries.add(trimmed);
            }
        }
        return entries;
    }

    /**
     * Validates that a receiver-supplied push endpoint URL is well-formed
     * and uses an http(s) scheme. Without this guard, the field would
     * accept arbitrary strings like {@code "not-a-url"} or
     * {@code "javascript:alert(1)"} — they'd persist, only to fail on
     * first push attempt and dead-letter every queued event for that
     * receiver. Reject up front so receivers / admins get a 400 with a
     * clear reason instead of mysterious wire-time failures later.
     */
    protected void validatePushEndpointUrl(String endpointUrl) {
        URI uri;
        try {
            uri = new URI(endpointUrl);
        } catch (URISyntaxException e) {
            throw new SsfException("Invalid stream configuration: delivery.endpoint_url is not a valid URI");
        }
        if (!uri.isAbsolute()) {
            throw new SsfException("Invalid stream configuration: delivery.endpoint_url must be absolute");
        }
        String scheme = uri.getScheme();
        if (scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new SsfException("Invalid stream configuration: delivery.endpoint_url must use http or https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new SsfException("Invalid stream configuration: delivery.endpoint_url must include a host");
        }
    }

    /**
     * Enforces structural constraints on receiver-supplied fields so invalid
     * inputs are rejected with a clean {@link SsfException} → HTTP 400 rather
     * than leaking through. Covers per-field length caps (so oversized values
     * don't reach DB column overflow) and content rules (e.g. rejecting
     * vault expressions in {@code authorization_header}, which would otherwise
     * grant a rogue receiver indirect read of arbitrary vault entries via
     * the resolved-on-read path in {@link ClientStreamStore}). The overall
     * persisted-blob size is additionally capped in
     * {@link ClientStreamStore#storeStreamConfig}.
     */
    protected void validateFieldConstraints(StreamConfig streamConfig) {

        String description = streamConfig.getDescription();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new SsfException("Invalid stream configuration: description exceeds "
                    + MAX_DESCRIPTION_LENGTH + " characters");
        }

        Set<String> eventsRequested = streamConfig.getEventsRequested();
        if (eventsRequested != null) {
            if (eventsRequested.size() > MAX_EVENTS_REQUESTED_COUNT) {
                throw new SsfException("Invalid stream configuration: events_requested exceeds "
                        + MAX_EVENTS_REQUESTED_COUNT + " entries");
            }
            for (String eventType : eventsRequested) {
                if (eventType != null && eventType.length() > MAX_EVENT_TYPE_LENGTH) {
                    throw new SsfException("Invalid stream configuration: events_requested entry exceeds "
                            + MAX_EVENT_TYPE_LENGTH + " characters");
                }
            }
        }

        StreamDeliveryConfig delivery = streamConfig.getDelivery();
        if (delivery != null) {
            String endpointUrl = delivery.getEndpointUrl();
            if (endpointUrl != null && endpointUrl.length() > MAX_DELIVERY_ENDPOINT_URL_LENGTH) {
                throw new SsfException("Invalid stream configuration: delivery.endpoint_url exceeds "
                        + MAX_DELIVERY_ENDPOINT_URL_LENGTH + " characters");
            }

            String authorizationHeader = delivery.getAuthorizationHeader();
            if (authorizationHeader != null && authorizationHeader.length() > MAX_DELIVERY_AUTHORIZATION_HEADER_LENGTH) {
                throw new SsfException("Invalid stream configuration: delivery.authorization_header exceeds "
                        + MAX_DELIVERY_AUTHORIZATION_HEADER_LENGTH + " characters");
            }
            // Vault expressions (${vault.x}) are an admin-only ceremony.
            // Accepting them via the receiver-facing API would let a rogue
            // receiver point its own authorization_header at any vault key
            // the operator has registered; the next GET would then resolve
            // and hand back the vault contents (see ClientStreamStore
            // applyDeliveryConfig). Reject the syntax outright — admins
            // configure vault placeholders directly on the client attribute.
            //
            // Use contains rather than startsWith: the default transcriber
            // only resolves anchored ^${vault.x}$ today, but a custom vault
            // provider or a future pattern loosening would re-open the
            // door if we accepted embedded forms like "Bearer ${vault.x}".
            if (authorizationHeader != null && authorizationHeader.contains("${vault.")) {
                throw new SsfException("Invalid stream configuration: delivery.authorization_header must not contain a vault expression");
            }

            Map<String, Object> additionalParameters = delivery.getAdditionalParameters();
            if (additionalParameters != null) {
                if (additionalParameters.size() > MAX_DELIVERY_ADDITIONAL_PARAMETERS_COUNT) {
                    throw new SsfException("Invalid stream configuration: delivery.additional_parameters exceeds "
                            + MAX_DELIVERY_ADDITIONAL_PARAMETERS_COUNT + " entries");
                }
                for (Map.Entry<String, Object> entry : additionalParameters.entrySet()) {
                    if (entry.getKey() != null && entry.getKey().length() > MAX_DELIVERY_ADDITIONAL_PARAMETER_KEY_LENGTH) {
                        throw new SsfException("Invalid stream configuration: delivery.additional_parameters key exceeds "
                                + MAX_DELIVERY_ADDITIONAL_PARAMETER_KEY_LENGTH + " characters");
                    }
                    Object value = entry.getValue();
                    if (value != null && value.toString().length() > MAX_DELIVERY_ADDITIONAL_PARAMETER_VALUE_LENGTH) {
                        throw new SsfException("Invalid stream configuration: delivery.additional_parameters value for key '"
                                + entry.getKey() + "' exceeds "
                                + MAX_DELIVERY_ADDITIONAL_PARAMETER_VALUE_LENGTH + " characters");
                    }
                }
            }
        }
    }

    /**
     * Gets a stream by ID.
     *
     * @param streamId The stream ID
     * @return The stream configuration, or null if not found
     */
    public StreamConfig getStream(String streamId) {
        return streamStore.getStream(streamId);
    }

    /**
     * Gets all streams for the current client context.
     *
     * @return A list of all stream configurations
     */
    public List<StreamConfig> getStreamsByClient(ClientModel receiverClient) {
        return streamStore.getAvailableStreams(receiverClient);
    }

    /**
     * Returns every stream configuration attached to a client whose SSF
     * receiver capability is enabled. Does not filter by per-stream status
     * — the dispatcher applies {@code StreamStatusValue} gating before
     * actually delivering events. See
     * {@link SsfStreamStore#findStreamsForSsfReceiverClients()}
     * for details.
     */
    public List<StreamConfig> findStreamsForSsfReceiverClients() {
        return streamStore.findStreamsForSsfReceiverClients();
    }

    /**
     * Finds a stream by ID across all clients in the realm.
     *
     * @param streamId The stream ID
     * @return The stream configuration, or null if not found
     */
    public StreamConfig findStreamById(String streamId) {
        return streamStore.findStreamById(streamId);
    }

    /**
     * Updates a stream using SSF spec §8.1.1.3 merge semantics.
     *
     * <p>Only non-null fields on {@code update} are applied to the stored
     * stream; null/absent fields retain their current value. Transmitter-
     * controlled fields are not on {@link StreamConfigUpdateRepresentation} at all, so
     * Jackson rejects any such field in the request body with 400 at bind
     * time — the receiver cannot clobber transmitter state such as
     * {@code iss}, {@code aud}, or {@code kc_created_at} by round-tripping
     * a previously fetched representation.
     *
     * <p>Note on null vs. absent: Java beans collapse the two into the same
     * state, so we define null-in-a-PATCH-body as "don't change this". To
     * explicitly clear a receiver-writable field, use PUT (full replace).
     *
     * @return The updated stream configuration, or {@code null} if the stream
     *         identified by {@code update.streamId} does not exist.
     */
    public StreamConfig updateStream(StreamConfigUpdateRepresentation streamUpdate) {

        if (streamUpdate == null || streamUpdate.getStreamId() == null) {
            throw new SsfException("Invalid stream update: stream_id is required");
        }

        StreamConfig existingStream = streamStore.getStream(streamUpdate.getStreamId());
        if (existingStream == null) {
            return null;
        }

        ClientModel client = session.getContext().getClient();
        ClientModel receiverClient = client;
        SsfProfile profile = resolveReceiverProfile(receiverClient);
        validateLegacyFieldsForProfile(streamUpdate, profile);

        boolean eventsRequestedChanged = streamUpdate.getEventsRequested() != null;
        String previousDeliveryMethodUri = currentDeliveryMethodUri(existingStream);

        // Work on an isolated draft: if validation or any post-merge
        // step throws, the stored config remains untouched. The draft
        // is only handed off to the store once every step has succeeded.
        StreamConfig draft = new StreamConfig(existingStream);
        mergeReceiverFields(streamUpdate, draft);
        applyLegacyFields(streamUpdate, draft, receiverClient, profile);

        // Re-run full validation against the merged result; delivery method /
        // push endpoint constraints are enforced on the combined state.
        validate(draft, receiverClient);

        // If the merge flipped the delivery channel (e.g. PUSH → POLL),
        // regenerate the poll endpoint URL if now needed and retarget
        // any already-queued non-terminal outbox rows so the new
        // channel picks them up. See also replaceStream.
        handleDeliveryMethodChange(draft, receiverClient, previousDeliveryMethodUri);

        // Recompute events_delivered only when events_requested actually changed —
        // a pure description or delivery update should not touch the delivered set.
        if (eventsRequestedChanged) {
            SsfEventsConfig eventsConfig = streamStore.getEventsConfig(receiverClient, draft.getEventsRequested());
            draft.setEventsDelivered(eventsConfig.eventsDelivered());
            // Already-queued non-terminal outbox rows whose event type
            // is no longer in events_delivered would otherwise be shipped
            // by the drainer / poll endpoint (neither rechecks the live
            // events_requested). Drop them so a narrowing PATCH actually
            // takes effect on the wire.
            evictPendingEventsOutsideDeliveredSet(receiverClient, draft.getEventsDelivered(), draft.getStreamId());
        }

        applySignatureAlgorithmFromClient(draft, receiverClient);
        applyUserSubjectFormatFromClient(draft, receiverClient);

        draft.setUpdatedAt(Time.currentTime());

        streamStore.saveStream(draft);

        RealmModel realm = session.getContext().getRealm();
        log.debugf("Stream updated. realm=%s client=%s streamId=%s",
                realm.getName(), client.getClientId(), streamUpdate.getStreamId());

        return resolveStreamForResponse(draft);
    }

    /**
     * Replaces a stream using SSF spec §8.1.1.4 semantics.
     *
     * <p>The receiver sends the entire receiver-writable stream configuration.
     * Receiver-updatable fields on the stored stream are replaced with the
     * values from the request (including being reset to {@code null} when
     * absent). Transmitter-controlled fields ({@code iss}, {@code aud},
     * {@code events_supported}, the {@code kc_*} extensions, …) are preserved
     * from storage: they are not on {@link StreamConfigUpdateRepresentation}, so Jackson
     * rejects them in the request body with 400 at bind time.
     *
     * @return The replaced stream configuration, or {@code null} if the stream
     *         does not exist.
     */
    public StreamConfig replaceStream(StreamConfigUpdateRepresentation streamUpdate) {

        if (streamUpdate == null || streamUpdate.getStreamId() == null) {
            throw new SsfException("Invalid stream replace: stream_id is required");
        }

        StreamConfig existingStream = streamStore.getStream(streamUpdate.getStreamId());
        if (existingStream == null) {
            return null;
        }

        ClientModel client = session.getContext().getClient();
        ClientModel receiverClient = client;
        SsfProfile profile = resolveReceiverProfile(receiverClient);
        validateLegacyFieldsForProfile(streamUpdate, profile);

        if (streamUpdate.getDelivery() == null) {
            throw new SsfException("Invalid stream replace: delivery is required");
        }

        String previousDeliveryMethodUri = currentDeliveryMethodUri(existingStream);

        // Work on an isolated draft: validation or any post-replace step
        // throwing leaves the stored config untouched. Omitted receiver
        // fields are reset on the draft — that is the whole point of
        // PUT vs PATCH.
        StreamConfig draft = new StreamConfig(existingStream);
        replaceReceiverFields(streamUpdate, draft);
        applyLegacyFields(streamUpdate, draft, receiverClient, profile);

        validate(draft, receiverClient);

        // Delivery method may have flipped (PUSH ↔ POLL) — re-derive
        // the poll endpoint URL if the new method is POLL and retarget
        // any queued non-terminal outbox rows so they land on the new
        // channel instead of being orphaned.
        handleDeliveryMethodChange(draft, receiverClient, previousDeliveryMethodUri);

        SsfEventsConfig eventsConfig = streamStore.getEventsConfig(receiverClient, draft.getEventsRequested());
        draft.setEventsDelivered(eventsConfig.eventsDelivered());
        // PUT replaces the entire receiver-writable subset, so always
        // evict pending rows for event types the receiver no longer wants.
        evictPendingEventsOutsideDeliveredSet(receiverClient, draft.getEventsDelivered(), draft.getStreamId());

        applySignatureAlgorithmFromClient(draft, receiverClient);
        applyUserSubjectFormatFromClient(draft, receiverClient);

        draft.setUpdatedAt(Time.currentTime());

        streamStore.saveStream(draft);

        RealmModel realm = session.getContext().getRealm();
        log.debugf("Stream replaced. realm=%s client=%s streamId=%s",
                realm.getName(), client.getClientId(), streamUpdate.getStreamId());

        return resolveStreamForResponse(draft);
    }

    /**
     * Reads the receiver client's {@code ssf.signatureAlgorithm} attribute,
     * validates it against {@link SsfSignatureAlgorithms#ALLOWED}, and
     * copies it onto the given {@link StreamConfig} so the dispatcher can
     * pick it up at delivery time. Rejects the stream create/update with
     * {@link SsfException} when the attribute is set to a value the
     * transmitter does not support, giving the receiver a clean 400
     * instead of a silent drop later during SET signing.
     *
     * <p>A {@code null} or blank attribute is intentionally allowed — it
     * means "use the transmitter-wide default", which the dispatcher
     * resolves via {@link SsfSignatureAlgorithms#resolveForStream}.
     */
    /**
     * For POLL streams, derives the transmitter-owned poll endpoint URL
     * from the realm issuer, the receiver's OAuth {@code clientId} and
     * the freshly-assigned {@code stream_id}, and writes it into
     * {@link StreamDeliveryConfig#setEndpointUrl}. Per SSF §6.1.2 the
     * poll endpoint URL is "specified by the Transmitter" — anything the
     * receiver provided on input is overwritten here. No-op for PUSH
     * streams where the receiver-supplied URL is the actual delivery
     * target.
     */
    /**
     * Returns the delivery-method URI currently stored on the given
     * stream, or {@code null} if unset. Exposed as a helper because
     * {@code updateStream} and {@code replaceStream} both need to
     * capture it before the merge / replace runs.
     */
    protected String currentDeliveryMethodUri(StreamConfig streamConfig) {
        if (streamConfig == null || streamConfig.getDelivery() == null) {
            return null;
        }
        return streamConfig.getDelivery().getMethod();
    }

    /**
     * Deals with the fallout of a receiver flipping the stream's
     * delivery method on update / replace:
     *
     * <ul>
     *     <li>When the new method is POLL, re-derive the
     *         transmitter-owned poll endpoint URL so any URL the
     *         receiver may have carried over from the previous PUSH
     *         config is overwritten with the correct one.</li>
     *     <li>Migrate queued non-terminal outbox rows
     *         (PENDING + HELD) to the new {@code DELIVERY_METHOD}
     *         column so the already-signed SETs land on the new
     *         channel instead of being orphaned. PUSH rows retargeted
     *         to POLL become available to the receiver's next poll;
     *         POLL rows retargeted to PUSH are picked up by the
     *         drainer on its next tick.</li>
     * </ul>
     *
     * <p>No-op when the URI hasn't changed.
     */
    protected void handleDeliveryMethodChange(StreamConfig streamConfig,
                                              ClientModel receiverClient,
                                              String previousDeliveryMethodUri) {
        String newMethodUri = currentDeliveryMethodUri(streamConfig);
        if (newMethodUri == null || newMethodUri.equals(previousDeliveryMethodUri)) {
            return;
        }

        // Re-derive the poll URL when switching TO a poll flavor so
        // the stored endpoint_url matches the transmitter-owned one.
        finalizePollEndpointUrlIfApplicable(streamConfig, receiverClient);

        // Retarget queued non-terminal outbox rows. The encoded SET
        // bytes are channel-agnostic; only the routing column needs
        // to change.
        DeliveryMethod newMethod = DeliveryMethod.valueOfUri(newMethodUri);
        if (newMethod == null) {
            return;
        }
        // In the generic outbox the delivery method is encoded by the
        // entryKind (ssf-push vs ssf-poll) rather than a column on the
        // row. Migrating the queued backlog therefore translates to
        // re-tagging rows from the old kind to the new one.
        String newKind = switch (newMethod) {
            case PUSH, RISC_PUSH -> SsfOutboxKinds.PUSH;
            case POLL, RISC_POLL -> SsfOutboxKinds.POLL;
        };
        String currentKind = SsfOutboxKinds.PUSH.equals(newKind) ? SsfOutboxKinds.POLL : SsfOutboxKinds.PUSH;
        OutboxStore outboxStore = outboxStoreFactory.apply(session);
        int migrated = outboxStore.migrateEntryKindForOwner(currentKind, newKind, streamConfig.getClientId());
        if (migrated > 0) {
            RealmModel realm = session.getContext().getRealm();
            log.debugf("Retargeted %d queued outbox row(s) on delivery method change %s → %s. realm=%s client=%s streamId=%s",
                    migrated, previousDeliveryMethodUri, newMethodUri,
                    realm.getName(), receiverClient.getClientId(), streamConfig.getStreamId());
        }
    }

    /**
     * Parks queued non-terminal outbox rows for the receiver whose
     * event type is not in the supplied {@code allowedEventTypes}
     * (the freshly recomputed {@code events_delivered} set) as
     * {@code DEAD_LETTER}. Called after a stream PATCH/PUT narrows
     * {@code events_requested}, so already-signed SETs of dropped
     * event types stop being delivered to the receiver later —
     * neither the push drainer nor the poll endpoint re-checks the
     * live {@code events_requested} per row. We dead-letter rather
     * than delete so the rows remain available for audit; the
     * standard dead-letter retention purge will eventually evict them.
     *
     * <p>An empty {@code allowedEventTypes} means the receiver no
     * longer accepts any events; every queued row is dead-lettered
     * for the same reason.
     */
    protected void evictPendingEventsOutsideDeliveredSet(ClientModel receiverClient,
                                                         Set<String> allowedEventTypes,
                                                         String streamId) {
        if (receiverClient == null) {
            return;
        }
        OutboxStore outboxStore = outboxStoreFactory.apply(session);
        Set<String> allowed = allowedEventTypes != null ? allowedEventTypes : Set.of();
        int parked = outboxStore.deadLetterQueuedForOwnerNotMatchingTypes(
                SsfOutboxKinds.PUSH, receiverClient.getId(), allowed,
                DEAD_LETTER_REASON_EVENT_TYPE_NO_LONGER_REQUESTED);
        parked += outboxStore.deadLetterQueuedForOwnerNotMatchingTypes(
                SsfOutboxKinds.POLL, receiverClient.getId(), allowed,
                DEAD_LETTER_REASON_EVENT_TYPE_NO_LONGER_REQUESTED);
        if (parked > 0) {
            RealmModel realm = session.getContext().getRealm();
            log.debugf("Dead-lettered %d queued outbox row(s) on events_requested narrow. realm=%s client=%s streamId=%s",
                    parked, realm.getName(), receiverClient.getClientId(), streamId);
        }
    }

    /**
     * Reason recorded on outbox rows dead-lettered when the receiver
     * narrows {@code events_requested} (the row's {@code entry_type}
     * is no longer in the allow-list). Stable string so admin dashboards
     * filtering on {@code last_error} match.
     */
    public static final String DEAD_LETTER_REASON_EVENT_TYPE_NO_LONGER_REQUESTED =
            "event_type_no_longer_requested";

    protected void finalizePollEndpointUrlIfApplicable(StreamConfig streamConfig, ClientModel receiverClient) {
        StreamDeliveryConfig delivery = streamConfig.getDelivery();
        if (delivery == null || delivery.getMethod() == null) {
            return;
        }
        String method = delivery.getMethod();
        if (!Ssf.DELIVERY_METHOD_POLL_URI.equals(method)
                && !Ssf.DELIVERY_METHOD_RISC_POLL_URI.equals(method)) {
            return;
        }
        String pollUrl = SsfTransmitterUrls.getPollEndpointUrl(
                transmitterService.getTransmitterMetadata().getIssuer(),
                receiverClient.getClientId(),
                streamConfig.getStreamId());
        delivery.setEndpointUrl(pollUrl);
    }

    protected void applySignatureAlgorithmFromClient(StreamConfig streamConfig, ClientModel receiverClient) {
        String signatureAlgorithm = receiverClient.getAttribute(ClientStreamStore.SSF_STREAM_SIGNATURE_ALGORITHM_KEY);
        if (signatureAlgorithm == null || signatureAlgorithm.isBlank()) {
            streamConfig.setSignatureAlgorithm(null);
            return;
        }
        if (!SsfSignatureAlgorithms.isAllowed(signatureAlgorithm)) {
            throw new SsfException("Invalid stream configuration: signature algorithm " + signatureAlgorithm
                    + " is not in the transmitter allow-list " + SsfSignatureAlgorithms.ALLOWED);
        }
        streamConfig.setSignatureAlgorithm(signatureAlgorithm);
    }

    /**
     * Reads the receiver client's {@code ssf.userSubjectFormat} attribute,
     * validates it against {@link SsfUserSubjectFormats#ALLOWED}, and copies
     * it onto the given {@link StreamConfig} so the mapper can pick it up
     * when building the user portion of an SSF SET. Rejects the stream
     * create/update with {@link SsfException} when the attribute is set to
     * an unsupported format, giving the receiver a clean 400 instead of a
     * silent fallback at emission time.
     *
     * <p>A {@code null} or blank attribute is intentionally allowed — it
     * means "use {@link SsfUserSubjectFormats#DEFAULT}", which matches the
     * transmitter's behavior before this knob was added.
     */
    protected void applyUserSubjectFormatFromClient(StreamConfig streamConfig, ClientModel receiverClient) {
        String userSubjectFormat = receiverClient.getAttribute(ClientStreamStore.SSF_STREAM_USER_SUBJECT_FORMAT_KEY);
        if (userSubjectFormat == null || userSubjectFormat.isBlank()) {
            streamConfig.setUserSubjectFormat(null);
            return;
        }
        if (!SsfUserSubjectFormats.isAllowed(userSubjectFormat)) {
            throw new SsfException("Invalid stream configuration: user subject format " + userSubjectFormat
                    + " is not in the transmitter allow-list " + SsfUserSubjectFormats.ALLOWED);
        }
        streamConfig.setUserSubjectFormat(userSubjectFormat);
    }

    /**
     * Deletes a stream.
     *
     * @param streamId The stream ID
     * @return true if the stream was deleted, false if not found
     */
    public boolean deleteStream(String streamId) {
        StreamConfig existingStream = streamStore.getStream(streamId);

        if (existingStream == null) {
            return false;
        }

        streamStore.deleteStream(streamId);

        // Cascade-purge any outstanding outbox rows for this receiver
        // client. PUSH rows would eventually reach DEAD_LETTER on their
        // own (and the dead-letter retention purge would clean them up
        // later), but POLL rows have no consumer once the stream is
        // gone — they'd sit forever. Scoped on clientId rather than
        // streamId because we run with one stream per client; if the
        // receiver re-creates a stream immediately the new
        // (clientId, jti) inserts are fresh, so over-deleting can't
        // strand a legitimate row.
        String clientId = existingStream.getClientId();
        RealmModel realm = session.getContext().getRealm();
        if (clientId != null) {
            OutboxStore outboxStore = outboxStoreFactory.apply(session);
            int purged = outboxStore.deleteByOwner(SsfOutboxKinds.PUSH, clientId)
                    + outboxStore.deleteByOwner(SsfOutboxKinds.POLL, clientId);
            if (purged > 0) {
                log.debugf("Stream delete cascade: purged %d outbox rows. realm=%s client=%s streamId=%s",
                        purged, realm.getName(), clientId, streamId);
            }
        }

        ClientModel client = session.getContext().getClient();
        log.debugf("Stream deleted. realm=%s client=%s streamId=%s",
                realm.getName(), client.getClientId(), streamId);

        return true;
    }

    /**
     * Gets the status of a stream.
     *
     * @param streamId The stream ID
     * @return The stream status, or null if not found
     */
    public StreamStatus getStreamStatus(String streamId) {
        StreamConfig stream = streamStore.getStream(streamId);

        if (stream == null) {
            return null;
        }

        StreamStatus status = new StreamStatus();
        status.setStreamId(streamId);
        status.setStatus(stream.getStatus().getStatusCode());
        status.setReason(stream.getStatusReason());

        return status;
    }

    /**
     * Updates the status of a stream.
     *
     * @param newStreamStatus The updated stream status
     * @return The updated stream status, or null if not found
     */
    /**
     * Admin-initiated status update: same flow as
     * {@link #updateStreamStatus(StreamStatus)} but explicitly
     * associated with a receiver client that the admin selected from
     * the admin UI. Temporarily rewrites
     * {@code session.getContext().getClient()} to the target receiver
     * so the downstream code (ClientStreamStore reads the receiver
     * from session context, the stream-updated SET dispatch reads it
     * for log context) sees the correct client. Restored in a
     * finally so the admin's own session context isn't left pointing
     * at the receiver.
     */
    public StreamStatus updateStreamStatusAsAdmin(StreamStatus newStreamStatus, ClientModel receiverClient) {
        ClientModel previousClient = session.getContext().getClient();
        session.getContext().setClient(receiverClient);
        try {
            return updateStreamStatus(newStreamStatus);
        } finally {
            session.getContext().setClient(previousClient);
        }
    }

    public StreamStatus updateStreamStatus(StreamStatus newStreamStatus) {

        if (newStreamStatus == null) {
            return null;
        }

        String streamId = newStreamStatus.getStreamId();
        if (streamId == null) {
            return null;
        }

        StreamConfig stream = streamStore.getStream(streamId);
        if (stream == null) {
            return null;
        }

        StreamStatus currentStreamStatus = streamStore.getStreamStatus(streamId);
        if (Objects.equals(currentStreamStatus.getStatus(), newStreamStatus.getStatus())) {
            // return current stream status
            return currentStreamStatus;
        }

        // TODO check if new status is allowed

        // Update the stream status
        streamStore.updateStreamStatus(streamId, newStreamStatus);
        stream.setUpdatedAt(Time.currentTime());

        // Update stream status
        StreamStatus streamStatus = streamStore.updateStreamStatus(streamId, newStreamStatus);

        RealmModel realm = session.getContext().getRealm();
        ClientModel client = session.getContext().getClient();
        log.debugf("Stream status updated. realm=%s client=%s streamId=%s status_old=%s status_new=%s",
                realm.getName(), client.getClientId(), streamId,
                currentStreamStatus.getStatus(), streamStatus.getStatus()
        );

        // SSF stream-status semantics: align the in-flight outbox
        // backlog with the new status BEFORE the stream-updated SET
        // is enqueued, so the SET row inserted right after this pass
        // stays PENDING and reaches the receiver:
        //
        //   * → disabled       ⇒ DISCARD all undelivered (PENDING + HELD).
        //                        The spec says a disabled stream MUST NOT
        //                        transmit AND "will not hold any events
        //                        for later transmission" — re-enable does
        //                        not resurrect the backlog.
        //   * enabled → paused ⇒ HOLD all PENDING (drainer / poll endpoint
        //                        stop serving them; symmetric to
        //                        dispatchEvent's hold of new events for
        //                        paused streams).
        //   * → enabled (from paused or disabled) ⇒ RELEASE all HELD back
        //                        to PENDING. For disabled→enabled this is
        //                        a no-op because the discard above wiped
        //                        any HELD rows; harmless.
        String newStatusCode = streamStatus.getStatus();
        String oldStatusCode = currentStreamStatus.getStatus();
        OutboxStore outboxStore = outboxStoreFactory.apply(session);
        String ownerId = stream.getClientId();
        try {
            if (StreamStatusValue.disabled.getStatusCode().equals(newStatusCode)) {
                // SSF spec: a disabled stream MUST NOT transmit AND
                // "will not hold any events for later transmission".
                // Discard PENDING+HELD across both kinds.
                outboxStore.deleteQueuedByOwner(SsfOutboxKinds.PUSH, ownerId);
                outboxStore.deleteQueuedByOwner(SsfOutboxKinds.POLL, ownerId);
            } else if (StreamStatusValue.enabled.getStatusCode().equals(newStatusCode)) {
                outboxStore.releaseHeldForOwner(SsfOutboxKinds.PUSH, ownerId);
                outboxStore.releaseHeldForOwner(SsfOutboxKinds.POLL, ownerId);
            } else if (StreamStatusValue.paused.getStatusCode().equals(newStatusCode)
                    && StreamStatusValue.enabled.getStatusCode().equals(oldStatusCode)) {
                outboxStore.holdPendingForOwner(SsfOutboxKinds.PUSH, ownerId);
                outboxStore.holdPendingForOwner(SsfOutboxKinds.POLL, ownerId);
            }
        } catch (Exception e) {
            log.warnf(e, "Failed to align outbox backlog with new stream status. realm=%s client=%s streamId=%s newStatus=%s",
                    realm.getName(), client.getClientId(), streamId, newStatusCode);
        }

        // SSF §8.1.2: notify the receiver of the new status with a
        // stream-updated SET on every transition. The spec carves out
        // an explicit exception for the inactivity-timeout path
        // ("If the Transmitter decides to pause or disable the stream,
        // it MUST send a Stream Updated Event") which is incompatible
        // with the paused/disabled MUST-NOT-transmit rule; we treat
        // the SET as a transmitter-initiated signal that lives outside
        // the normal dispatch gate. deliverEvent bypasses the
        // dispatcher's status gate so the event goes out regardless of
        // the new (or old) status.
        try {
            StreamConfig refreshed = streamStore.getStream(streamId);
            if (refreshed != null) {
                SsfSecurityEventToken updatedEvent = transmitterProvider.securityEventTokenMapper()
                        .generateStreamUpdatedEvent(refreshed, streamStatus);
                if (updatedEvent != null) {
                    transmitterProvider.securityEventTokenDispatcher()
                            .deliverEvent(updatedEvent, refreshed);
                }
            }
        } catch (Exception e) {
            // Status persistence already succeeded — surface the
            // dispatch failure as a warning rather than rolling back
            // the status change itself; the receiver can re-derive
            // the current status via GET /streams/status.
            log.warnf(e, "Failed to dispatch stream-updated SET. realm=%s client=%s streamId=%s",
                    realm.getName(), client.getClientId(), streamId);
        }

        return streamStatus;
    }
}
