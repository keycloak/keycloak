package org.keycloak.ssf.transmitter.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.ssf.SsfException;
import org.keycloak.ssf.event.InitiatingEntity;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.caep.CaepEvent;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.ssf.event.stream.SsfStreamUpdatedEvent;
import org.keycloak.ssf.event.stream.SsfStreamVerificationEvent;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.stream.StreamStatus;
import org.keycloak.ssf.subject.ComplexSubjectId;
import org.keycloak.ssf.subject.EmailSubjectId;
import org.keycloak.ssf.subject.IssuerSubjectId;
import org.keycloak.ssf.subject.OpaqueSubjectId;
import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.support.SsfUtil;

import org.jboss.logging.Logger;


/**
 * Generator for Security Event Tokens (SETs).
 */
public class SecurityEventTokenMapper {

    protected static final Logger log = Logger.getLogger(SecurityEventTokenMapper.class);

    // Each group matches a single path segment ([^/]+) rather than .* — path
    // segments are UUIDs that never contain '/', and .* across segment
    // boundaries enables polynomial backtracking on adversarial input like
    // "users//credentials/a/credentials/a/…" (CodeQL js/polynomial-redos).
    protected static final Pattern USER_LOGGED_OUT_BY_ADMIN_PATH_PATTERN = Pattern.compile("^users/([^/]+)/logout$");

    protected static final Pattern USER_RESET_PASSWORD_BY_ADMIN_PATH_PATTERN = Pattern.compile("^users/([^/]+)/reset-password$");

    protected static final Pattern USER_CREDENTIALS_CHANGED_BY_ADMIN_PATH_PATTERN = Pattern.compile("^users/([^/]+)/credentials/([^/]+)$");

    public static final String KC_CREDENTIAL_ID = "kc_credential_id";

    public static final String KC_CREDENTIAL_TYPE = "kc_credential_type";

    public static final String KC_CREDENTIAL_USER_LABEL = "kc_credential_user_label";

    /**
     * Issuer URL resolver. Invoked lazily at token-build time rather than
     * at construction so that off-request callers (e.g. the scheduled
     * SSF outbox drainer) that only need a mapper-less slice of the
     * transmitter provider don't trip over {@code HttpRequest}-bound
     * hostname resolution.
     */
    protected final Function<KeycloakSession, String> issuerGenerator;

    protected String issuer;

    protected final KeycloakSession session;

    protected final SsfTransmitterConfig transmitterConfig;

    public SecurityEventTokenMapper(KeycloakSession session, SsfTransmitterConfig transmitterConfig, Function<KeycloakSession, String> issuerGenerator) {
        this.session = session;
        this.issuerGenerator = issuerGenerator;
        this.transmitterConfig = transmitterConfig;
    }

    protected String getIssuer() {
        if (issuer == null) {
            issuer = issuerGenerator.apply(session);
        }
        return issuer;
    }

    /**
     * Generates a verification event for a stream.
     *
     * @param stream The stream configuration
     * @param state  The verification state
     * @return The verification event as a JSON string
     */
    /**
     * Generates a stream-updated SET communicating a stream status change to
     * the receiver, per SSF §8.1.5. Subject is the stream itself (opaque
     * {@code stream_id}), event payload carries the new status and the
     * optional reason — same shape used by {@code GET /stream/status}.
     *
     * <p>Callers are expected to dispatch the returned token via
     * {@link org.keycloak.ssf.transmitter.delivery.SecurityEventTokenDispatcher#deliverEvent
     * deliverEvent} (gate-bypassing, async) so the receiver still sees the
     * status change even when the new status is {@code paused}/{@code disabled}.
     */
    public SsfSecurityEventToken generateStreamUpdatedEvent(StreamConfig stream, StreamStatus newStatus) {
        try {
            SsfSecurityEventToken token = newSecurityEventToken(stream);
            token.setTxn(UUID.randomUUID().toString());

            OpaqueSubjectId subId = new OpaqueSubjectId();
            subId.setId(stream.getStreamId());
            token.setSubjectId(subId);

            SsfStreamUpdatedEvent payload = new SsfStreamUpdatedEvent();
            payload.setStatus(newStatus);
            payload.setReason(newStatus.getReason());

            Map<String, Object> events = new HashMap<>();
            events.put(SsfStreamUpdatedEvent.TYPE, payload);
            token.setEvents(events);

            return token;
        } catch (Exception e) {
            log.error("Error generating stream-updated event", e);
            return null;
        }
    }

    public SsfSecurityEventToken generateVerificationEvent(StreamConfig stream, String state) {
        try {
            SsfSecurityEventToken verificationEventToken = newSecurityEventToken(stream);

            // Set transaction ID
            verificationEventToken.setTxn(UUID.randomUUID().toString());

            // Set subject ID
            OpaqueSubjectId subId = new OpaqueSubjectId();
            subId.setId(stream.getStreamId());
            verificationEventToken.setSubjectId(subId);

            // Set events
            Map<String, Object> events = new HashMap<>();
            SsfStreamVerificationEvent verificationEvent = new SsfStreamVerificationEvent();
            verificationEvent.setState(state);
            events.put(SsfStreamVerificationEvent.TYPE, verificationEvent);
            verificationEventToken.setEvents(events);

            return verificationEventToken;
        } catch (Exception e) {
            log.error("Error generating verification event", e);
            return null;
        }
    }

    protected SsfSecurityEventToken newSecurityEventToken(StreamConfig stream) {
        SsfSecurityEventToken securityEventToken = new SsfSecurityEventToken();

        securityEventToken.setJti(SecretGenerator.getInstance().generateSecureID());
        securityEventToken.setIss(getIssuer());
        securityEventToken.setIat(Time.currentTime());

        // Set the SET audience to the stream's audience
        if (stream.getDelivery() != null && stream.getDelivery().getEndpointUrl() != null) {
            securityEventToken.setAud(stream.getAudience().toArray(new String[0]));
        }

        return securityEventToken;
    }


    /**
     * Generates a session revoked event.
     *
     * @param event
     * @param sessionId            The ID of the revoked session
     * @param userId               The ID of the user
     * @param eventTokenCustomizer
     * @param adminEvent
     * @param stream
     * @param reason               The reason for the revocation
     * @return The session revoked event as a SecurityEventToken
     */
    public SsfSecurityEventToken generateSessionRevokedEvent(Event userEvent, AdminEvent adminEvent, StreamConfig stream, String reason) {
        try {

            String sessionId = userEvent.getSessionId();
            String userId = userEvent.getUserId();

            SsfSecurityEventToken eventToken = newSecurityEventToken(stream);
            eventToken.setTxn(UUID.randomUUID().toString());

            // Set subject ID (complex subject with user and session,
            // plus tenant when the configured user-subject format
            // carries the +tenant composition suffix).
            ComplexSubjectId subId = new ComplexSubjectId();

            subId.setUser(buildUserSubjectId(eventToken, userId, stream));

            OpaqueSubjectId sessionSubject = new OpaqueSubjectId();
            sessionSubject.setId(sessionId);
            subId.setSession(sessionSubject);

            addTenantIfConfigured(subId, userId, stream);

            eventToken.setSubjectId(subId);

            // Set events
            Map<String, Object> events = new HashMap<>();
            CaepSessionRevoked sessionRevokedEvent = new CaepSessionRevoked();
            applyInitiatingEntity(userEvent, adminEvent, sessionRevokedEvent);

            if (reason != null) {
                sessionRevokedEvent.setReasonAdmin(Map.of("en", reason));
            }
            sessionRevokedEvent.setEventTimestamp(Time.currentTime());

            events.put(CaepSessionRevoked.TYPE, sessionRevokedEvent);
            eventToken.setEvents(events);

            return eventToken;
        } catch (Exception e) {
            log.error("Error generating session revoked event", e);
            return null;
        }
    }

    /**
     * Generates a credential change event.
     *
     * <p>The CAEP {@code change_type} is supplied by the caller because
     * Keycloak's {@code UPDATE_CREDENTIAL} / {@code REMOVE_CREDENTIAL} /
     * {@code RESET_PASSWORD} event types each map to a different CAEP
     * change_type (UPDATE / DELETE / UPDATE) and the dispatcher in
     * {@link #toSecurityEventToken(Event, StreamConfig)} knows the right
     * value for each. The {@code credentialType} string is read from
     * {@code userEvent.getDetails().get(Details.CREDENTIAL_TYPE)} when
     * present and falls back to {@code credentialTypeFallback} otherwise
     * — used for events like {@code RESET_PASSWORD} that don't set the
     * detail but where the credential type is implicit ("password").
     *
     * <p>Distinguishing CREATE from UPDATE inside this path is left
     * unimplemented: Keycloak fires {@code UPDATE_CREDENTIAL} for both
     * "first credential of this type added" and "existing credential
     * modified" without a marker on the event itself, so a heuristic
     * here would be guesswork. UPDATE is the conservative default for
     * additions and modifications.
     */
    public SsfSecurityEventToken generateCredentialChangeEvent(Event userEvent,
                                                               AdminEvent adminEvent,
                                                               StreamConfig streamConfig,
                                                               CaepCredentialChange.ChangeType changeType,
                                                               String credentialTypeFallback) {
        try {

            String userId = userEvent.getUserId();
            String credentialType = userEvent.getDetails() != null
                    ? userEvent.getDetails().get(Details.CREDENTIAL_TYPE)
                    : null;
            if (credentialType == null || credentialType.isBlank()) {
                credentialType = credentialTypeFallback;
            }

            SsfSecurityEventToken eventToken = newSecurityEventToken(streamConfig);
            eventToken.setTxn(UUID.randomUUID().toString());

            // Set subject ID — composeUserSubject wraps in a complex
            // subject with a tenant sibling when the configured format
            // carries the +tenant composition suffix; otherwise the
            // bare user subject goes on as before.
            eventToken.setSubjectId(composeUserSubject(eventToken, userId, streamConfig));

            String caepCredentialType = narrowCaepCredentialType(credentialType);

            CaepCredentialChange credentialChangeEvent = new CaepCredentialChange();
            credentialChangeEvent.setChangeType(changeType);
            credentialChangeEvent.setEventTimestamp(Time.currentTime());
            credentialChangeEvent.setCredentialType(caepCredentialType);
            applyInitiatingEntity(userEvent, adminEvent, credentialChangeEvent);
            applyCustomAttributes(userEvent, adminEvent, credentialChangeEvent);

            // Set events
            Map<String, Object> events = new HashMap<>();
            events.put(CaepCredentialChange.TYPE, credentialChangeEvent);
            eventToken.setEvents(events);

            return eventToken;
        } catch (Exception e) {
            log.error("Error generating credential change event", e);
            return null;
        }
    }

    protected void applyCustomAttributes(Event userEvent, AdminEvent adminEvent, CaepCredentialChange credentialChangeEvent) {
        // Keycloak user events aren't required to carry a details map —
        // RESET_PASSWORD in particular doesn't populate it. Skip the
        // custom-attribute enrichment in that case rather than NPE.
        Map<String, String> userEventDetails = userEvent != null ? userEvent.getDetails() : null;
        if (userEventDetails == null) {
            return;
        }
        String kcCredentialId = userEventDetails.get(Details.CREDENTIAL_ID);
        String kcCredentialType = userEventDetails.get(Details.CREDENTIAL_TYPE);
        String kcUserLabel = userEventDetails.get(Details.CREDENTIAL_USER_LABEL);

        credentialChangeEvent.setAttributeValue(KC_CREDENTIAL_ID, kcCredentialId);
        credentialChangeEvent.setAttributeValue(KC_CREDENTIAL_TYPE, kcCredentialType);
        credentialChangeEvent.setAttributeValue(KC_CREDENTIAL_USER_LABEL, kcUserLabel);
    }

    protected void applyInitiatingEntity(Event userEvent, AdminEvent adminEvent, CaepEvent caepEvent) {
        if (adminEvent != null) {
            caepEvent.setInitiatingEntity(InitiatingEntity.ADMIN);
        } else {
            caepEvent.setInitiatingEntity(InitiatingEntity.USER);
        }
    }

    protected String narrowCaepCredentialType(String credentialType) {

        // best effort attempt at mapping Keycloak credential types to CAEP credential types
        // see: https://openid.net/specs/openid-caep-1_0-final.html#section-3.3.1-1

        if (credentialType == null) {
            return "unknown";
        }

        if (PasswordCredentialModel.TYPE.equals(credentialType)) {
            return "password";
        }

        if (OTPCredentialModel.TYPE.equals(credentialType)) {
            return "app";
        }

        if (WebAuthnCredentialModel.TYPE_TWOFACTOR.equals(credentialType)) {
            // could be fido2-platform or fido2-roaming, assume roaming
            return "fido2-roaming";
        }

        if (WebAuthnCredentialModel.TYPE_PASSWORDLESS.equals(credentialType)) {
            // could be fido2-platform, assume platform
            return "fido2-platform";
        }

        return credentialType;
    }

    /**
     * Builds a Security Event Token for a synthetic event produced by the
     * admin-facing event emitter endpoint — i.e. an event that Keycloak
     * did not observe itself but that a trusted IAM management client
     * wants the transmitter to forward as if it had.
     *
     * <p>The caller supplies the already-resolved event type URI, the
     * deserialized event payload, and the RFC 9493 {@link SubjectId} the
     * emitter chose. The {@code sub_id} is passed through verbatim — the
     * emitter is trusted to pick a format appropriate for the receiver
     * (which, unlike natively emitted events, is outside the transmitter's
     * knowledge since the upstream system owns the subject identity).
     *
     * <p>The SET is otherwise built with the standard header
     * (iss, jti, iat, aud, txn) identical to natively emitted events.
     *
     * <p>Returns {@code null} if construction fails (logged).
     */
    public SsfSecurityEventToken generateSyntheticEvent(StreamConfig stream,
                                                        String eventTypeUri,
                                                        Object eventPayload,
                                                        SubjectId subjectId) {
        try {
            SsfSecurityEventToken token = newSecurityEventToken(stream);
            token.setTxn(UUID.randomUUID().toString());
            token.setSubjectId(subjectId);

            Map<String, Object> events = new HashMap<>();
            events.put(eventTypeUri, eventPayload);
            token.setEvents(events);

            return token;
        } catch (Exception e) {
            log.error("Error generating synthetic event", e);
            return null;
        }
    }

    /**
     * Builds the SSF subject identifier for a Keycloak user according to
     * the stream's configured user subject format (falling back to
     * {@link SsfUserSubjectFormats#DEFAULT iss_sub}). Invoked for every
     * event type whose {@code sub_id} carries a user identifier — both
     * as the {@code user} field of a {@link ComplexSubjectId} (e.g. for
     * {@link CaepSessionRevoked}) and as the top-level {@code sub_id}
     * of simpler events (e.g. {@link CaepCredentialChange}).
     *
     * <p>When the configured format is {@code email} and no email is
     * available for the user (user deleted mid-event, or simply has
     * no email on record), this method throws an {@link SsfException}
     * — silently substituting {@code iss_sub} would deliver a SET
     * shaped differently from what the receiver negotiated, which a
     * strict receiver would reject and a lenient receiver would
     * misroute. The caller catches the exception, logs it, and drops
     * the event. The receiver therefore sees no signal rather than a
     * misrepresented one; operators see the warning and can fix the
     * user record or change the stream's subject format.
     */
    protected SubjectId buildUserSubjectId(SsfSecurityEventToken eventToken, String userId, StreamConfig stream) {

        // Strip any composition suffix (e.g. "+tenant") — that part is
        // applied by addTenantIfConfigured at the outer complex-subject
        // level. This method only computes the user-identifying portion.
        String format = SsfUserSubjectFormats.userPartOf(
                SsfUserSubjectFormats.resolveForStream(stream, transmitterConfig));

        if (EmailSubjectId.TYPE.equals(format)) {
            String email = lookupUserEmail(userId);
            if (email == null || email.isBlank()) {
                throw new SsfException("Configured user subject format is 'email' but no email is available for user "
                        + userId + " (stream " + (stream != null ? stream.getStreamId() : null) + ")");
            }
            EmailSubjectId emailSubject = new EmailSubjectId();
            emailSubject.setEmail(email);
            return emailSubject;
        }

        // Reuse the issuer that newSecurityEventToken already resolved onto
        // the token — one lookup per event instead of two, and the subject
        // identity stays consistent with the SET's top-level iss.
        IssuerSubjectId issSubject = new IssuerSubjectId();
        issSubject.setIss(eventToken.getIss());
        issSubject.setSub(userId);
        return issSubject;
    }

    /**
     * If the configured user-subject format carries the {@code +tenant}
     * composition suffix, resolves the user's primary Keycloak
     * organization and adds it as the {@code tenant} member of the
     * given complex subject. No-op when the format does not include
     * tenant. Throws {@link SsfException} (caught by the calling event
     * generator and logged) when the user belongs to no organization —
     * silently dropping the tenant slot would deliver a SET shaped
     * differently from what the receiver negotiated, mirroring the
     * fail-loud behaviour of the {@code email} format with no email.
     */
    protected void addTenantIfConfigured(ComplexSubjectId complex, String userId, StreamConfig stream) {
        String format = SsfUserSubjectFormats.resolveForStream(stream, transmitterConfig);
        if (!SsfUserSubjectFormats.includesTenant(format)) {
            return;
        }
        complex.setTenant(buildTenantSubject(userId, stream));
    }

    /**
     * Builds the user subject for the given receiver using the same
     * code path as native event emission — honors the receiver's
     * configured {@code ssf.userSubjectFormat} (iss_sub / email /
     * complex.iss_sub+tenant / complex.email+tenant), with the same
     * fail-loud behaviour for missing email or no organization.
     *
     * <p>Used by the admin "Pending Events" emit form so an operator
     * can pick a user (by UUID) and let the transmitter format the
     * sub_id per the receiver's negotiated subject shape, instead of
     * the admin UI hardcoding {@code iss_sub} regardless of config.
     */
    public SubjectId buildSubjectForReceiver(StreamConfig stream, String userId) {
        SsfSecurityEventToken stub = newSecurityEventToken(stream);
        return composeUserSubject(stub, userId, stream);
    }

    /**
     * Wraps the user subject in a {@link ComplexSubjectId} when the
     * configured format includes the {@code +tenant} composition;
     * returns the bare user subject otherwise. Used by event
     * generators whose default emission shape is a single subject
     * (e.g. {@code credential-change}) so they can pick up the tenant
     * member without unconditionally switching to a complex shape.
     */
    protected SubjectId composeUserSubject(SsfSecurityEventToken eventToken, String userId, StreamConfig stream) {
        SubjectId userSubject = buildUserSubjectId(eventToken, userId, stream);
        String format = SsfUserSubjectFormats.resolveForStream(stream, transmitterConfig);
        if (!SsfUserSubjectFormats.includesTenant(format)) {
            return userSubject;
        }
        ComplexSubjectId complex = new ComplexSubjectId();
        complex.setUser(userSubject);
        complex.setTenant(buildTenantSubject(userId, stream));
        return complex;
    }

    /**
     * Builds an {@link OpaqueSubjectId} carrying the user's primary
     * Keycloak organization alias. Throws {@link SsfException} when
     * the user belongs to no organization — see
     * {@link #addTenantIfConfigured} for why fail-loud is the right
     * choice.
     *
     * <p><b>Multi-org resolution policy: managed-preferred.</b> When the
     * user belongs to multiple organizations, prefers the {@code MANAGED}
     * membership (the org that provisioned the user — at most one per
     * user, per Keycloak's organization model) and falls back to the
     * first {@code UNMANAGED} membership otherwise. This gives users
     * with a clear provisioning origin (SCIM, IdP federation, JIT) a
     * stable "owning organization" answer; users that just associate
     * with one or more orgs get a deterministic-but-arbitrary first-of-
     * stream pick. Deployments that need stricter semantics
     * (managed-only) or a different policy can subclass this method.
     */
    protected SubjectId buildTenantSubject(String userId, StreamConfig stream) {
        if (session == null || userId == null) {
            throw new SsfException("Cannot build tenant subject: missing session or userId (stream "
                    + (stream != null ? stream.getStreamId() : null) + ")");
        }
        RealmModel realm = session.getContext().getRealm();
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        if (orgProvider == null) {
            throw new SsfException("Cannot build tenant subject: organization feature is not enabled (stream "
                    + (stream != null ? stream.getStreamId() : null) + ")");
        }
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            throw new SsfException("Cannot build tenant subject: user " + userId + " not found (stream "
                    + (stream != null ? stream.getStreamId() : null) + ")");
        }
        OrganizationModel org = orgProvider.getByMember(user)
                .filter(candidate -> orgProvider.isManagedMember(candidate, user))
                .findFirst()
                .orElseGet(() -> orgProvider.getByMember(user).findFirst().orElse(null));
        if (org == null) {
            throw new SsfException("Configured user subject format includes '+tenant' but user " + userId
                    + " belongs to no organization (stream " + (stream != null ? stream.getStreamId() : null) + ")");
        }
        // Emit alias rather than the internal UUID — alias is the stable,
        // human-readable organization identifier and the receiver-side
        // SubjectResolver tries getByAlias as a fallback to getById, so
        // this resolves on round-trip without requiring receivers to
        // know the transmitter's internal UUIDs.
        return createTenantSubjectId(org, user);
    }

    protected OpaqueSubjectId createTenantSubjectId(OrganizationModel org, UserModel user) {
        OpaqueSubjectId tenantSubject = new OpaqueSubjectId();
        tenantSubject.setId(org.getAlias());
        return tenantSubject;
    }

    /**
     * Resolves a Keycloak user's email address for the current realm.
     * Returns {@code null} when the session is not available, the user
     * cannot be resolved, or the user has no email — callers fall back
     * to {@code iss_sub} in that case.
     */
    protected String lookupUserEmail(String userId) {
        if (session == null || userId == null) {
            return null;
        }
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            return null;
        }
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            return null;
        }
        return user.getEmail();
    }

    /**
     * Cheap predicate that returns {@code true} iff
     * {@link #toSecurityEventToken(Event, StreamConfig)} would produce a
     * non-null SET for {@code event}, based only on event type + details
     * currently present on the event. Callers use this to short-circuit
     * the per-event stream lookup in {@code SsfTransmitterEventListener}
     * — if no mapping is possible, there is no point in hitting the
     * client store to find eligible streams.
     *
     * <p>The check deliberately mirrors the {@code switch} in
     * {@link #toSecurityEventToken(Event, StreamConfig)} so the two stay in
     * sync. New event types added to the mapper must be reflected here
     * too, otherwise the listener will silently drop them.
     */
    public boolean canConvert(Event event) {
        if (event == null || shouldIgnoreEvent(event)) {
            return false;
        }
        return switch (event.getType()) {
            case LOGOUT -> !shouldIgnoreLogout(event);
            case UPDATE_CREDENTIAL,
                 REMOVE_CREDENTIAL,
                 RESET_PASSWORD -> !shouldIgnoreCredentialChange(event);
            default -> false;
        };
    }

    /**
     * Cheap predicate that returns {@code true} iff
     * {@link #toSecurityEventToken(AdminEvent, StreamConfig)} would produce a
     * non-null SET for {@code adminEvent}. Currently the only mapped
     * admin operation is the "log out all user sessions" path
     * ({@code users/{userId}/logout}); everything else returns null and
     * should short-circuit before any stream lookup happens.
     */
    public boolean canConvert(AdminEvent adminEvent) {
        if (adminEvent == null) {
            return false;
        }
        if (!ResourceType.USER.equals(adminEvent.getResourceType())) {
            return false;
        }
        String path = adminEvent.getResourcePath();
        if (path == null) {
            return false;
        }

        for (Pattern pattern : supportedAdminPathPatters()) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }

        return false;
    }

    protected List<Pattern> supportedAdminPathPatters() {
        return List.of(USER_LOGGED_OUT_BY_ADMIN_PATH_PATTERN,
                USER_RESET_PASSWORD_BY_ADMIN_PATH_PATTERN,
                USER_CREDENTIALS_CHANGED_BY_ADMIN_PATH_PATTERN);
    }

    protected boolean shouldIgnoreEvent(Event event) {
        return false;
    }

    public SsfSecurityEventToken toSecurityEventToken(Event event, StreamConfig stream) {
        return toSecurityEventToken(event, null, stream);
    }

    public SsfSecurityEventToken toSecurityEventToken(Event event, AdminEvent adminEvent, StreamConfig stream) {

        SsfSecurityEventToken securityEvent = switch (event.getType()) {

            case LOGOUT -> {

                // ignore expired session cleanup, we only want to propagate real logouts!
                if (shouldIgnoreLogout(event)) {
                    yield null;
                }

                yield generateSessionRevokedEvent(event, adminEvent, stream, "User logout");
            }

            case UPDATE_CREDENTIAL -> {

                // ignore credential changes for credentials that are not used for authentication
                if (shouldIgnoreCredentialChange(event)) {
                    yield null;
                }

                // CAEP change_type=update covers both "first credential of
                // this type added" and "existing credential modified" —
                // Keycloak's UPDATE_CREDENTIAL fires for both without a
                // distinguishing detail, so we conservatively report
                // UPDATE for both. credential_type is read from the
                // event's Details.CREDENTIAL_TYPE.
                yield generateCredentialChangeEvent(event, adminEvent, stream,
                        CaepCredentialChange.ChangeType.UPDATE, null);
            }

            case REMOVE_CREDENTIAL -> {

                if (shouldIgnoreCredentialChange(event)) {
                    yield null;
                }

                // CAEP change_type=delete: the credential was removed
                // (required-action DeleteCredentialAction). credential_type
                // is on the event's Details.
                yield generateCredentialChangeEvent(event, adminEvent, stream,
                        CaepCredentialChange.ChangeType.DELETE, null);
            }

            case RESET_PASSWORD -> {

                if (shouldIgnoreCredentialChange(event)) {
                    yield null;
                }

                // RESET_PASSWORD is the forgot-password completion flow —
                // CAEP change_type=update on the password credential.
                // Keycloak doesn't set Details.CREDENTIAL_TYPE on this
                // event, so pass "password" as the fallback.
                yield generateCredentialChangeEvent(event, adminEvent, stream,
                        CaepCredentialChange.ChangeType.UPDATE, "password");
            }
            // Add more event mappings here as needed.
            // Deliberately NOT mapped: UPDATE_PASSWORD / UPDATE_TOTP /
            // REMOVE_TOTP — these are deprecated event types Keycloak
            // fires as clones alongside UPDATE_CREDENTIAL /
            // REMOVE_CREDENTIAL. Mapping them here would emit a
            // duplicate SET per real change.

            default -> {

                if (shouldIgnoreEvent(event)) {
                    yield null;
                }

                yield generateSecurityEventTokenFromEvent(event, stream);
            }
        };
        // Map Keycloak events to SSF events

        return securityEvent;
    }

    protected boolean shouldIgnoreCredentialChange(Event event) {
        return false;
    }

    protected boolean shouldIgnoreLogout(Event event) {
        String reason = event.getDetails().get(Details.REASON);
        return Details.USER_SESSION_EXPIRED_REASON.equals(reason) || Details.INVALID_USER_SESSION_REMEMBER_ME_REASON.equals(reason);
    }

    protected SsfSecurityEventToken generateSecurityEventTokenFromEvent(Event event, StreamConfig stream) {
        return null;
    }

    public SsfSecurityEventToken toSecurityEventToken(AdminEvent adminEvent, StreamConfig stream) {

        String userId = SsfUtil.userIdFromAdminEventPath(adminEvent);
        if (userId == null) {
            return null;
        }

        String path = adminEvent.getResourcePath();
        Matcher matcher = USER_LOGGED_OUT_BY_ADMIN_PATH_PATTERN.matcher(path);
        if (matcher.matches()) {
            return generateLogoutEventForAdminLogoutAllUserSessions(userId, adminEvent, stream);
        }

        matcher = USER_RESET_PASSWORD_BY_ADMIN_PATH_PATTERN.matcher(path);
        if (matcher.matches()) {
            return generateCredentialChangeEventForAdminAction(userId, adminEvent, stream, CaepCredentialChange.ChangeType.UPDATE, null, "password_reset");
        }

        matcher = USER_CREDENTIALS_CHANGED_BY_ADMIN_PATH_PATTERN.matcher(path);
        if (matcher.matches()) {
            String credentialId = matcher.group(2);

            CaepCredentialChange.ChangeType changeType = CaepCredentialChange.ChangeType.UPDATE;
            if (adminEvent.getOperationType() == OperationType.DELETE || adminEvent.getOperationType() == OperationType.ACTION) {
                changeType = CaepCredentialChange.ChangeType.DELETE;
            }

            return generateCredentialChangeEventForAdminAction(userId, adminEvent, stream, changeType, credentialId, "credential_management");
        }

        return null;
    }

    protected SsfSecurityEventToken generateCredentialChangeEventForAdminAction(String userId,
                                                                                AdminEvent adminEvent,
                                                                                StreamConfig stream,
                                                                                CaepCredentialChange.ChangeType changeType,
                                                                                String credentialId,
                                                                                String reason) {

        Event event = new Event();

        String credentialType = PasswordCredentialModel.TYPE;
        String userLabel = null;
        if (credentialId != null) {
            RealmModel realm = session.realms().getRealm(adminEvent.getRealmId());
            UserModel user = session.users().getUserById(realm, userId);
            CredentialModel storedCredentialById = user.credentialManager().getStoredCredentialById(credentialId);
            if (storedCredentialById != null) {
                credentialType = storedCredentialById.getType();
                userLabel = storedCredentialById.getUserLabel();
            } else if (changeType == CaepCredentialChange.ChangeType.DELETE) {
                credentialType = adminEvent.getDetails().get(Details.CREDENTIAL_TYPE);
                credentialId = adminEvent.getDetails().get(Details.CREDENTIAL_ID);
                userLabel = adminEvent.getDetails().get(Details.CREDENTIAL_USER_LABEL);
            }
        }

        event.setType(EventType.RESET_PASSWORD);
        event.setUserId(userId);
        event.setDetails(new HashMap<>());
        event.getDetails().put("admin", "true");
        event.getDetails().put(Details.REASON, reason);
        event.getDetails().put(Details.CREDENTIAL_TYPE, credentialType);
        event.getDetails().put(Details.CREDENTIAL_ID, credentialId);
        event.getDetails().put(Details.CREDENTIAL_USER_LABEL, userLabel);

        if (shouldIgnoreCredentialChange(event)) {
            return null;
        }

        return generateCredentialChangeEvent(event, adminEvent, stream, changeType, null);
    }

    protected SsfSecurityEventToken generateLogoutEventForAdminLogoutAllUserSessions(String userId, AdminEvent adminEvent, StreamConfig stream) {

        Event event = new Event();
        event.setType(EventType.LOGOUT);
        event.setUserId(userId);
        event.setSessionId("ALL"); // all sessions
        event.setDetails(new HashMap<>());
        event.getDetails().put("admin", "true");
        event.getDetails().put(Details.REASON, "logout_all_user_sessions");

        return toSecurityEventToken(event, stream);
    }
}
