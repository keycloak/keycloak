package org.keycloak.ssf.transmitter.subject;

import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.ssf.subject.AddSubjectRequest;
import org.keycloak.ssf.subject.RemoveSubjectRequest;
import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.subject.SubjectResolution;
import org.keycloak.ssf.subject.SubjectResolver;
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
            SsfNotifyAttributes.setForUser(u.user(), callerClientId);
            log.debugf("SSF subject added. clientId=%s userId=%s", callerClientId, u.user().getId());
            return SubjectManagementResult.OK;
        }
        if (resolution instanceof SubjectResolution.Organization o) {
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

        return unregisterSubjectForNotification(clientClientId, resolution);
    }

    protected SubjectManagementResult excludeSubjectFromNotification(String callerClientId, SubjectResolution resolution) {
        if (resolution instanceof SubjectResolution.User u) {
            SsfNotifyAttributes.excludeForUser(u.user(), callerClientId);
            log.debugf("SSF subject excluded. clientId=%s userId=%s", callerClientId, u.user().getId());
            return SubjectManagementResult.OK;
        }
        if (resolution instanceof SubjectResolution.Organization o) {
            SsfNotifyAttributes.excludeForOrganization(o.organization(), callerClientId);
            log.debugf("SSF subject excluded. clientId=%s orgId=%s", callerClientId, o.organization().getId());
            return SubjectManagementResult.OK;
        }
        if (resolution instanceof SubjectResolution.NotFound) {
            return SubjectManagementResult.SUBJECT_NOT_FOUND;
        }
        return SubjectManagementResult.FORMAT_UNSUPPORTED;
    }

    protected SubjectManagementResult unregisterSubjectForNotification(String callerClientId, SubjectResolution resolution) {
        if (resolution instanceof SubjectResolution.User u) {
            SsfNotifyAttributes.clearForUser(u.user(), callerClientId);
            log.debugf("SSF subject removed. clientId=%s userId=%s", callerClientId, u.user().getId());
            return SubjectManagementResult.OK;
        }
        if (resolution instanceof SubjectResolution.Organization o) {
            SsfNotifyAttributes.clearForOrganization(o.organization(), callerClientId);
            log.debugf("SSF subject removed. clientId=%s orgId=%s", callerClientId, o.organization().getId());
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
        SubjectManagementResult result = unregisterSubjectForNotification(clientClientId, resolution);
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
