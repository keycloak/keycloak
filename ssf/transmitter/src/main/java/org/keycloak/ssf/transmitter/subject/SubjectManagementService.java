package org.keycloak.ssf.transmitter.subject;

import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.ssf.SsfException;
import org.keycloak.ssf.subject.AddSubjectRequest;
import org.keycloak.ssf.subject.ComplexSubjectId;
import org.keycloak.ssf.subject.OpaqueSubjectId;
import org.keycloak.ssf.subject.RemoveSubjectRequest;
import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.subject.SubjectResolution;
import org.keycloak.ssf.subject.SubjectResolver;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;

import org.jboss.logging.Logger;

/**
 * Orchestrates the add/remove subject lifecycle: resolves the subject
 * to a Keycloak entity via {@link SubjectResolver}, verifies stream
 * ownership, and toggles the {@code ssf.notify.<clientId>} attribute
 * on the resolved entity via {@link SsfNotifyAttributes}.
 */
public class SubjectManagementService {

    private static final Logger log = Logger.getLogger(SubjectManagementService.class);

    protected final KeycloakSession session;

    public SubjectManagementService(KeycloakSession session) {
        this.session = session;
    }

    public SubjectManagementResult addSubject(String callerClientId,
                                              AddSubjectRequest request) {
        SubjectManagementResult ownershipResult = checkOwnership(callerClientId, request.getStreamId());
        if (ownershipResult != null) {
            return ownershipResult;
        }

        String clientClientId = resolveClientClientId(callerClientId);
        SubjectResolution resolution = resolveSubject(request.getSubject());
        return registerSubjectForNotification(clientClientId, resolution);
    }

    protected SubjectManagementResult registerSubjectForNotification(String callerClientId, SubjectResolution resolution) {
        if (resolution instanceof SubjectResolution.User u) {
            // Re-adding wins over any prior receiver-driven removal —
            // clear the SSF §9.3 tombstone so the dispatcher uses the
            // fresh include marker instead of falling through to a
            // stale grace-window check.
            SsfNotifyAttributes.clearRemovedAtForUser(u.user(), callerClientId);
            SsfNotifyAttributes.setForUser(u.user(), callerClientId);
            log.debugf("SSF subject added. clientId=%s userId=%s", callerClientId, u.user().getId());
            return SubjectManagementResult.OK;
        }
        if (resolution instanceof SubjectResolution.Organization o) {
            SsfNotifyAttributes.clearRemovedAtForOrganization(o.organization(), callerClientId);
            SsfNotifyAttributes.setForOrganization(o.organization(), callerClientId);
            log.debugf("SSF subject added. clientId=%s orgId=%s", callerClientId, o.organization().getId());
            return SubjectManagementResult.OK;
        }
        if (resolution instanceof SubjectResolution.NotFound) {
            return SubjectManagementResult.SUBJECT_NOT_FOUND;
        }
        return SubjectManagementResult.FORMAT_UNSUPPORTED;
    }

    public SubjectManagementResult removeSubject(String callerClientId,
                                                 RemoveSubjectRequest request) {
        SubjectManagementResult ownershipResult = checkOwnership(callerClientId, request.getStreamId());
        if (ownershipResult != null) {
            return ownershipResult;
        }

        String clientClientId = resolveClientClientId(callerClientId);
        SubjectResolution resolution = resolveSubject(request.getSubject());

        // Receiver-driven path: stamp the SSF §9.3 tombstone so the
        // dispatcher can honor a configured grace window. Admin-driven
        // removes go through removeSubjectByAdmin and skip the stamp.
        return unregisterSubjectForNotification(clientClientId, resolution, true);
    }

    protected SubjectManagementResult excludeSubjectFromNotification(String callerClientId, SubjectResolution resolution) {
        if (resolution instanceof SubjectResolution.User u) {
            // Explicit exclusion is an admin-trusted action — no grace
            // window. Clear any prior receiver-driven tombstone so the
            // exclude marker takes effect immediately.
            SsfNotifyAttributes.clearRemovedAtForUser(u.user(), callerClientId);
            SsfNotifyAttributes.excludeForUser(u.user(), callerClientId);
            log.debugf("SSF subject excluded. clientId=%s userId=%s", callerClientId, u.user().getId());
            return SubjectManagementResult.OK;
        }
        if (resolution instanceof SubjectResolution.Organization o) {
            SsfNotifyAttributes.clearRemovedAtForOrganization(o.organization(), callerClientId);
            SsfNotifyAttributes.excludeForOrganization(o.organization(), callerClientId);
            log.debugf("SSF subject excluded. clientId=%s orgId=%s", callerClientId, o.organization().getId());
            return SubjectManagementResult.OK;
        }
        if (resolution instanceof SubjectResolution.NotFound) {
            return SubjectManagementResult.SUBJECT_NOT_FOUND;
        }
        return SubjectManagementResult.FORMAT_UNSUPPORTED;
    }

    /**
     * Clears the include / exclude marker for the resolved subject and,
     * when {@code applyTombstone} is true, stamps the SSF §9.3 grace
     * tombstone so the dispatcher can keep delivering events for the
     * configured grace window. Tombstone is intentionally skipped on
     * admin-driven removes — operator actions are trusted and take
     * effect immediately. Receivers cannot opt out: enabling the grace
     * via SPI means accepting that legitimate churn-removes also get
     * the grace tail.
     */
    protected SubjectManagementResult unregisterSubjectForNotification(String callerClientId, SubjectResolution resolution, boolean applyTombstone) {
        if (resolution instanceof SubjectResolution.User u) {
            if (applyTombstone) {
                SsfNotifyAttributes.stampRemovedAtForUser(u.user(), callerClientId);
            }
            SsfNotifyAttributes.clearForUser(u.user(), callerClientId);
            log.debugf("SSF subject removed. clientId=%s userId=%s tombstone=%s",
                    callerClientId, u.user().getId(), applyTombstone);
            return SubjectManagementResult.OK;
        }
        if (resolution instanceof SubjectResolution.Organization o) {
            if (applyTombstone) {
                SsfNotifyAttributes.stampRemovedAtForOrganization(o.organization(), callerClientId);
            }
            SsfNotifyAttributes.clearForOrganization(o.organization(), callerClientId);
            log.debugf("SSF subject removed. clientId=%s orgId=%s tombstone=%s",
                    callerClientId, o.organization().getId(), applyTombstone);
            return SubjectManagementResult.OK;
        }
        if (resolution instanceof SubjectResolution.NotFound) {
            return SubjectManagementResult.SUBJECT_NOT_FOUND;
        }
        return SubjectManagementResult.FORMAT_UNSUPPORTED;
    }

    /**
     * Resolves a {@link SubjectId} to a Keycloak entity. Protected so
     * subclasses can plug in custom resolution logic — e.g. additional
     * subject formats or alternative lookup strategies.
     */
    protected SubjectResolution resolveSubject(SubjectId subjectId) {
        RealmModel realm = session.getContext().getRealm();
        return SubjectResolver.resolve(session, realm, subjectId);
    }

    /**
     * Admin-driven add: resolves by admin shorthand type (user-id,
     * user-email, org-alias) and sets the notify attribute. Skips
     * ownership check — admin is trusted.
     *
     * @return a result with the resolved entity type and id, or a
     *         failure indicator.
     */
    public AdminSubjectResult addSubjectByAdmin(String clientId, String type, String value) {
        String clientClientId = resolveClientClientId(clientId);
        SubjectResolution resolution = resolveByAdminType(type, value);
        SubjectManagementResult result = registerSubjectForNotification(clientClientId, resolution);
        return toAdminResult(result, resolution);
    }

    /**
     * Admin-driven ignore: resolves by admin shorthand type and sets the
     * notify attribute to {@code false} (explicit exclusion).
     */
    public AdminSubjectResult ignoreSubjectByAdmin(String clientId, String type, String value) {
        String clientClientId = resolveClientClientId(clientId);
        SubjectResolution resolution = resolveByAdminType(type, value);
        SubjectManagementResult result = excludeSubjectFromNotification(clientClientId, resolution);
        return toAdminResult(result, resolution);
    }

    /**
     * Admin-driven remove: resolves by admin shorthand type and clears
     * the notify attribute.
     */
    public AdminSubjectResult removeSubjectByAdmin(String clientId, String type, String value) {
        String clientClientId = resolveClientClientId(clientId);
        SubjectResolution resolution = resolveByAdminType(type, value);
        // Admin-driven removes deliberately skip the SSF §9.3 grace
        // tombstone — operator actions are trusted and take effect
        // immediately. Compromised-receiver protection only applies to
        // the receiver-driven remove path.
        SubjectManagementResult result = unregisterSubjectForNotification(clientClientId, resolution, false);
        return toAdminResult(result, resolution);
    }

    protected AdminSubjectResult toAdminResult(SubjectManagementResult result, SubjectResolution resolution) {
        if (result == SubjectManagementResult.OK) {
            if (resolution instanceof SubjectResolution.User u) {
                return new AdminSubjectResult(result, "user", u.user().getId());
            }
            if (resolution instanceof SubjectResolution.Organization o) {
                return new AdminSubjectResult(result, "organization", o.organization().getId());
            }
        }
        return new AdminSubjectResult(result, null, null);
    }

    public SubjectResolution resolveByAdminType(String type, String value) {
        RealmModel realm = session.getContext().getRealm();

        if ("user-id".equals(type)) {
            UserModel user = session.users().getUserById(realm, value);
            return user != null ? new SubjectResolution.User(user) : SubjectResolution.NOT_FOUND;
        }
        if ("user-email".equals(type)) {
            UserModel user = session.users().getUserByEmail(realm, value);
            return user != null ? new SubjectResolution.User(user) : SubjectResolution.NOT_FOUND;
        }
        if ("user-username".equals(type)) {
            UserModel user = session.users().getUserByUsername(realm, value);
            return user != null ? new SubjectResolution.User(user) : SubjectResolution.NOT_FOUND;
        }
        if ("org-alias".equals(type)) {
            if (!Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
                return SubjectResolution.UNSUPPORTED_FORMAT;
            }
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            if (orgProvider == null) {
                return SubjectResolution.UNSUPPORTED_FORMAT;
            }
            var org = orgProvider.getByAlias(value);
            return org != null ? new SubjectResolution.Organization(org) : SubjectResolution.NOT_FOUND;
        }

        return SubjectResolution.UNSUPPORTED_FORMAT;
    }

    /**
     * Resolves the admin shorthand {@code (type, value)} into a typed
     * {@link SubjectId} suitable for handing straight to the synthetic
     * event emitter. For user subjects ({@code user-id} /
     * {@code user-email} / {@code user-username}) the sub_id is built
     * via {@link org.keycloak.ssf.transmitter.event.SecurityEventTokenMapper#buildSubjectForReceiver
     * buildSubjectForReceiver} so it honors the receiver's configured
     * {@code ssf.userSubjectFormat}. For {@code org-alias} the result
     * is a {@link ComplexSubjectId} with only a {@code tenant} facet
     * (so the emitter routes it as an org-scoped event).
     *
     * <p>Throws {@link SsfException} with an operator-friendly message
     * for each failure — unresolvable subject, unknown type, or the
     * mapper's fail-loud cases (missing email, no organization for a
     * {@code +tenant} format). The admin endpoint catches and surfaces
     * as 400.
     */
    public SubjectId resolveSubjectForEmit(StreamConfig stream,
                                           String subjectType,
                                           String subjectValue) {
        SubjectResolution resolution = resolveByAdminType(subjectType, subjectValue);
        if (resolution instanceof SubjectResolution.User userRes) {
            SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
            return transmitter.securityEventTokenMapper()
                    .buildSubjectForReceiver(stream, userRes.user().getId());
        }
        if (resolution instanceof SubjectResolution.Organization orgRes) {
            ComplexSubjectId complex = new ComplexSubjectId();
            OpaqueSubjectId tenant = new OpaqueSubjectId();
            tenant.setId(orgRes.organization().getAlias());
            complex.setTenant(tenant);
            return complex;
        }
        if (resolution instanceof SubjectResolution.NotFound) {
            throw new SsfException("Subject not found for type=" + subjectType
                    + " value=" + subjectValue);
        }
        // UNSUPPORTED_FORMAT (unknown type or organization feature disabled).
        throw new SsfException("Unsupported subjectType: " + subjectType);
    }

    /**
     * Resolves the internal client UUID to the human-readable OAuth
     * {@code client_id} used as the {@code ssf.notify} attribute key.
     * Falls back to the input if the client can't be resolved (e.g.
     * deleted mid-request).
     */
    protected String resolveClientClientId(String clientUuid) {
        RealmModel realm = session.getContext().getRealm();
        var client = realm.getClientById(clientUuid);
        return client != null ? client.getClientId() : clientUuid;
    }

    protected SubjectManagementResult checkOwnership(String callerClientId, String streamId) {
        ClientStreamStore streamStore = new ClientStreamStore(session);
        var client = session.getContext().getRealm().getClientById(callerClientId);
        if (client == null) {
            return SubjectManagementResult.STREAM_NOT_FOUND;
        }
        StreamConfig stream = streamStore.getStreamForClient(client);
        if (stream == null || !streamId.equals(stream.getStreamId())) {
            return SubjectManagementResult.STREAM_NOT_FOUND;
        }
        return null;
    }
}
