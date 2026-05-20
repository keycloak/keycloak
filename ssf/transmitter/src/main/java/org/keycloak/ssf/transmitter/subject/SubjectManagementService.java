package org.keycloak.ssf.transmitter.subject;

import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.ssf.SsfException;
import org.keycloak.ssf.metadata.DefaultSubjects;
import org.keycloak.ssf.subject.ComplexSubjectId;
import org.keycloak.ssf.subject.OpaqueSubjectId;
import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.subject.SubjectResolution;
import org.keycloak.ssf.subject.SubjectResolver;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.resources.AddSubjectRequest;
import org.keycloak.ssf.transmitter.resources.RemoveSubjectRequest;
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

    /**
     * Read-only inspection of a subject's effective notification state
     * for a receiver client — what the dispatcher's subject gate would
     * decide if an event for this subject were dispatched right now.
     * Drives the admin UI's "Check" button so the displayed status
     * reflects the actual gate logic instead of a client-side guess.
     *
     * <p>Mirrors the dispatcher's decision order: per-user explicit
     * settings always win over org inheritance and the
     * {@code default_subjects} fallback. So a user with
     * {@code ssf.notify.<clientId>=false} reads as {@code "ignored"}
     * even when one of their organizations carries notify=true — an
     * admin who clicked "Ignore" on a specific user expects that
     * decision to stick regardless of any membership-driven defaults.
     *
     * <p>Resolution order, returning the first match:
     * <ol>
     *   <li>{@code ssf.notify.<clientId>=true} on the user →
     *       {@code "notified"}</li>
     *   <li>{@code ssf.notify.<clientId>=false} on the user →
     *       {@code "ignored"}</li>
     *   <li>For users only: any of the user's organizations carries
     *       {@code ssf.notify.<clientId>=true} →
     *       {@code "notified_via_org"}</li>
     *   <li>{@code default_subjects=ALL} branch:
     *     <ol type="a">
     *       <li>For users only: any organization with
     *           {@code ssf.notify.<clientId>=false} →
     *           {@code "ignored_via_org"}</li>
     *       <li>Otherwise → {@code "implicitly_included"}</li>
     *     </ol>
     *   </li>
     *   <li>{@code default_subjects=NONE} branch with no inclusion
     *       signal → {@code "not_notified"}.</li>
     * </ol>
     *
     * <p>SSF §9.3 removal-grace tombstones and pluggable
     * {@link SsfSubjectInclusionResolver} extensions are not yet
     * reflected here; document as a follow-up.
     */
    public AdminSubjectStatus inspectSubjectByAdmin(ClientModel client, String type, String value) {
        SubjectResolution resolution = resolveByAdminType(type, value);
        if (resolution == SubjectResolution.NOT_FOUND) {
            return new AdminSubjectStatus("not_found", null, null, null);
        }
        if (resolution == SubjectResolution.UNSUPPORTED_FORMAT) {
            return new AdminSubjectStatus("unsupported_format", null, null, null);
        }

        String receiverClientId = client.getClientId();
        DefaultSubjects defaultSubjects = DefaultSubjects.parseOrDefault(
                client.getAttribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY), null);

        if (resolution instanceof SubjectResolution.User u) {
            UserModel user = u.user();

            // 1. Per-user explicit settings always win.
            if (SsfNotifyAttributes.isUserNotified(user, receiverClientId)) {
                return new AdminSubjectStatus("notified", "user", user.getId(), null);
            }
            if (SsfNotifyAttributes.isUserExcluded(user, receiverClientId)) {
                return new AdminSubjectStatus("ignored", "user", user.getId(), null);
            }

            // 2. Org-level inclusion (any org notify=true).
            OrganizationModel notifyingOrg = firstOrgNotifying(user, receiverClientId);
            if (notifyingOrg != null) {
                return new AdminSubjectStatus("notified_via_org", "user", user.getId(), notifyingOrg.getAlias());
            }

            // 3. ALL mode: org-level exclusion check, otherwise implicitly included.
            if (defaultSubjects == DefaultSubjects.ALL) {
                OrganizationModel excludingOrg = firstOrgExcluding(user, receiverClientId);
                if (excludingOrg != null) {
                    return new AdminSubjectStatus("ignored_via_org", "user", user.getId(), excludingOrg.getAlias());
                }
                return new AdminSubjectStatus("implicitly_included", "user", user.getId(), null);
            }

            // 4. NONE mode with no inclusion signal.
            return new AdminSubjectStatus("not_notified", "user", user.getId(), null);
        }
        if (resolution instanceof SubjectResolution.Organization o) {
            OrganizationModel org = o.organization();
            if (SsfNotifyAttributes.isOrganizationNotified(org, receiverClientId)) {
                return new AdminSubjectStatus("notified", "organization", org.getId(), null);
            }
            if (SsfNotifyAttributes.isOrganizationExcluded(org, receiverClientId)) {
                return new AdminSubjectStatus("ignored", "organization", org.getId(), null);
            }
            if (defaultSubjects == DefaultSubjects.ALL) {
                return new AdminSubjectStatus("implicitly_included", "organization", org.getId(), null);
            }
            return new AdminSubjectStatus("not_notified", "organization", org.getId(), null);
        }
        return new AdminSubjectStatus("not_notified", null, null, null);
    }

    /**
     * Returns the first organization the user is a member of that
     * notifies for the receiver, or {@code null} when none does. The
     * "first" org is whichever the {@link OrganizationProvider} stream
     * returns first — typically deterministic for a given store but not
     * spec'd as ordered. We surface the alias only as informational
     * context for the admin UI; the dispatcher's gate doesn't care
     * which org tipped the decision.
     */
    protected OrganizationModel firstOrgNotifying(UserModel user, String receiverClientId) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            return null;
        }
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        if (orgProvider == null) {
            return null;
        }
        return orgProvider.getByMember(user)
                .filter(org -> SsfNotifyAttributes.isOrganizationNotified(org, receiverClientId))
                .findFirst()
                .orElse(null);
    }

    protected OrganizationModel firstOrgExcluding(UserModel user, String receiverClientId) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            return null;
        }
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        if (orgProvider == null) {
            return null;
        }
        return orgProvider.getByMember(user)
                .filter(org -> SsfNotifyAttributes.isOrganizationExcluded(org, receiverClientId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Status surfaced by {@link #inspectSubjectByAdmin}.
     *
     * @param status       canonical state name (e.g. {@code "notified"}, {@code "ignored_via_org"}).
     * @param entityType   {@code "user"} or {@code "organization"}, or {@code null} when the
     *                     subject couldn't be resolved.
     * @param entityId     id of the resolved entity, or {@code null} when not resolved.
     * @param sourceOrgAlias alias of the organization that drove the decision for the
     *                     {@code *_via_org} states, or {@code null} otherwise. Lets the
     *                     admin UI render which membership tipped the gate.
     */
    public record AdminSubjectStatus(String status, String entityType, String entityId, String sourceOrgAlias) {}

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
