package org.keycloak.ssf.transmitter.subject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.ssf.subject.ComplexSubjectId;
import org.keycloak.ssf.subject.EmailSubjectId;
import org.keycloak.ssf.subject.IssuerSubjectId;
import org.keycloak.ssf.subject.OpaqueSubjectId;
import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.storage.adapter.AbstractInMemoryUserAdapter;

/**
 * A detached, read-only stand-in for a user that is about to be deleted.
 *
 * <p>Keycloak removes the user row before it fires the admin / user event that
 * drives SSF emission, so by the time the transmitter runs, every
 * {@code session.users().getUserById(...)} in the pipeline returns {@code null}.
 * This snapshot is captured on {@link UserModel.UserPreRemovedEvent} — the one
 * hook that still runs while the user exists — and stashed on the
 * {@link KeycloakSession} so the emission path can substitute it wherever the
 * live lookup misses.
 *
 * <p>It deliberately <em>is</em> a {@link UserModel} rather than a bag of fields.
 * Everything downstream that needs the user does attribute reads —
 * {@link SubjectSubscriptionFilter} and {@link SsfSubjectInclusionResolver} read
 * {@code ssf.notify.<clientId>}, and the mapper's email subject format reads
 * {@code getEmail()}, which is itself an attribute read. Copying the attribute map
 * into an in-memory adapter therefore makes those call sites work unmodified.
 *
 * <p><b>Organization membership is the single exception.</b>
 * {@code OrganizationProvider.getByMember(user)} is an id-keyed database query, not
 * an attribute read, so a detached user cannot answer it. Only that one fact is
 * captured here, as a list of aliases: the organizations themselves outlive the
 * user, so callers re-resolve live {@link OrganizationModel}s through
 * {@code getByAlias} and then run exactly the same logic they run for a live user.
 * Nothing about the organizations' own state is copied, which keeps the pluggable
 * {@link SsfSubjectInclusionResolver} authoritative for purged users as well.
 *
 * <p>The snapshot lives only as long as the request. Everything that reads it
 * (token construction, the subject gates, narrowing and signing) runs inline on the
 * deleting thread before the transaction commits; the outbox stores an already-signed
 * SET and never resolves a user again.
 */
public class PurgedUserSnapshot extends AbstractInMemoryUserAdapter {

    private static final String SESSION_ATTRIBUTE_PREFIX = "ssf.purgedUser.";

    /**
     * Secondary index. The dispatcher's subject gate re-resolves the user from the
     * token's own {@code sub_id} rather than from the event's user id, and for a
     * stream configured with the {@code email} subject format that identifier is an
     * address, not a UUID. Stashing the snapshot under both keys lets that gate find
     * it without the filter having to know how the subject was built.
     */
    private static final String SESSION_ATTRIBUTE_EMAIL_PREFIX = "ssf.purgedUserByEmail.";

    /**
     * The user's primary organization alias, resolved managed-preferred to mirror
     * {@code SecurityEventTokenMapper.buildTenantSubject}. {@code null} when the
     * organization feature is off or the user belonged to no organization.
     */
    private final String tenantAlias;

    /**
     * Aliases of every organization the user belonged to, in membership order. The
     * stand-in for {@code getByMember} — see the class javadoc.
     */
    private final List<String> organizationAliases;

    protected PurgedUserSnapshot(KeycloakSession session,
                                 RealmModel realm,
                                 String id,
                                 String tenantAlias,
                                 List<String> organizationAliases) {
        super(session, realm, id);
        this.tenantAlias = tenantAlias;
        this.organizationAliases = organizationAliases;
    }

    public String getTenantAlias() {
        return tenantAlias;
    }

    /**
     * Aliases of the organizations this user belonged to at deletion time. Callers
     * resolve them to live {@link OrganizationModel}s rather than reading captured
     * organization state.
     */
    public List<String> getOrganizationAliases() {
        return organizationAliases;
    }

    /**
     * The realm the user was deleted from. Snapshots are keyed by user id alone, so
     * callers use this to reject a snapshot from a different realm.
     */
    public RealmModel getRealm() {
        return realm;
    }

    /**
     * Always throws. A purged user has no credentials to manage — the rows are gone
     * along with the user. Nothing on the SSF emission path asks for them (credential
     * events are a separate mapper branch that only ever runs for live users), so a
     * caller reaching this is a bug worth surfacing rather than papering over with a
     * no-op manager that would silently report "no credentials".
     */
    @Override
    public SubjectCredentialManager credentialManager() {
        throw new UnsupportedOperationException(
                "Purged user " + getId() + " has no credential manager; the user no longer exists");
    }

    /**
     * Captures {@code user} and stashes it on the session under its user id.
     * No-op when the user is a service account — those are not human subjects and
     * never produce a purge SET, so capturing one would only cost an organization
     * query on every client deletion — and no-op when the deletion is not bound to
     * an HTTP request (see {@link #isRequestBound}).
     *
     * <p>Callers invoke this from the pre-remove hook, which fires before Keycloak
     * knows whether the removal will succeed. Capturing is therefore deliberately
     * side-effect free: a failed delete leaves an unread snapshot that dies with
     * the session.
     */
    public static void capture(KeycloakSession session, RealmModel realm, UserModel user) {
        if (session == null || realm == null || user == null || user.getId() == null) {
            return;
        }
        if (user.getServiceAccountClientLink() != null) {
            return;
        }
        if (!isRequestBound(session)) {
            return;
        }

        String tenantAlias = null;
        List<String> organizationAliases = List.of();
        if (Organizations.isEnabled(session)) {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            List<OrganizationModel> organizations = orgProvider.getByMember(user).toList();
            organizationAliases = organizations.stream().map(OrganizationModel::getAlias).toList();
            // Managed-preferred, matching the mapper's multi-org resolution policy:
            // the organization that provisioned the user wins, otherwise the first
            // membership. Keeps the purge event's tenant subject identical to the
            // one every earlier event for this user carried.
            OrganizationModel tenant = organizations.stream()
                    .filter(candidate -> orgProvider.isManagedMember(candidate, user))
                    .findFirst()
                    .orElseGet(() -> organizations.stream().findFirst().orElse(null));
            if (tenant != null) {
                tenantAlias = tenant.getAlias();
            }
        }

        PurgedUserSnapshot snapshot = new PurgedUserSnapshot(session, realm, user.getId(),
                tenantAlias, organizationAliases);

        // Copy the whole attribute map first — it carries username, email and every
        // ssf.notify.* / ssf.notifyRemovedAt.* entry the subject gates read.
        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes != null) {
            for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
                if (entry.getValue() != null) {
                    snapshot.setAttribute(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
            }
        }
        // Then re-assert the two identity fields through their typed setters. The
        // attribute map is expected to carry both, but a custom user storage provider
        // is free to surface them only via the getters, and losing the email would
        // silently degrade streams configured for the `email` subject format.
        if (user.getUsername() != null) {
            snapshot.setUsername(user.getUsername());
        }
        if (user.getEmail() != null) {
            snapshot.setEmail(user.getEmail());
        }
        snapshot.setEnabled(user.isEnabled());
        snapshot.setReadonly(true);

        session.setAttribute(sessionAttributeKey(user.getId()), snapshot);
        if (snapshot.getEmail() != null) {
            session.setAttribute(sessionEmailAttributeKey(snapshot.getEmail()), snapshot);
        }
    }

    /**
     * True when this deletion is happening inside an HTTP request.
     *
     * <p>A snapshot exists solely to serve the admin or user event fired later in the
     * same request, so a deletion with no request behind it can never produce one and
     * capturing would retain data nothing will ever read. That is not hypothetical:
     * {@code DefaultWorkflowProvider.runScheduledSteps} drives every due step for every
     * enabled workflow on a single session, and its delete step removes one user per
     * iteration — so a retention run purging tens of thousands of accounts would
     * otherwise hold a snapshot for each, for the life of that session.
     *
     * <p>Deliberately a gate rather than a cap on retained snapshots. A cap would
     * silently stop capturing partway through a bulk run, which becomes silently
     * dropped events the day those paths do emit; this stays correct either way,
     * because emission for them cannot use the request-bound listener path anyway.
     *
     * <p>{@code getHttpRequest()} throws {@link RuntimeException}
     * ({@code ContextNotActiveException}) rather than returning null when no request
     * context is active, so absence has to be detected by catching. Any failure is
     * treated as "no request": the cost is a purge event not emitted on a path that
     * has none to emit, which is strictly better than unbounded retention.
     */
    private static boolean isRequestBound(KeycloakSession session) {
        try {
            return session.getContext() != null && session.getContext().getHttpRequest() != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Returns the snapshot captured for {@code userId} in this session, or
     * {@code null} when the user was not deleted on this request.
     */
    public static PurgedUserSnapshot lookup(KeycloakSession session, RealmModel realm, String userId) {
        if (session == null || userId == null) {
            return null;
        }
        PurgedUserSnapshot snapshot = session.getAttribute(sessionAttributeKey(userId), PurgedUserSnapshot.class);
        return matchesRealm(snapshot, realm) ? snapshot : null;
    }

    /**
     * Resolves a snapshot from an RFC 9493 subject identifier, mirroring the
     * identifier shapes {@code SubjectUserLookup} understands. Used by the
     * dispatcher-side subject gate, which re-resolves the user from the token's own
     * {@code sub_id} rather than from the originating event.
     *
     * <p>Complex subjects are drilled into their {@code user} member, matching how
     * the filter unwraps them for live users.
     */
    public static PurgedUserSnapshot lookupBySubject(KeycloakSession session, RealmModel realm, SubjectId subjectId) {
        if (session == null || subjectId == null) {
            return null;
        }
        if (subjectId instanceof ComplexSubjectId complex) {
            return lookupBySubject(session, realm, complex.getUser());
        }
        if (subjectId instanceof IssuerSubjectId issuerSubject) {
            return lookup(session, realm, issuerSubject.getSub());
        }
        if (subjectId instanceof OpaqueSubjectId opaqueSubject) {
            return lookup(session, realm, opaqueSubject.getId());
        }
        if (subjectId instanceof EmailSubjectId emailSubject) {
            String email = emailSubject.getEmail();
            if (email == null || email.isBlank()) {
                return null;
            }
            PurgedUserSnapshot snapshot =
                    session.getAttribute(sessionEmailAttributeKey(email), PurgedUserSnapshot.class);
            return matchesRealm(snapshot, realm) ? snapshot : null;
        }
        return null;
    }

    /**
     * Resolves {@code userId} to a live user, falling back to this request's
     * purge snapshot when the row is already gone. The single substitution point
     * the emission path uses in place of a bare {@code getUserById}.
     */
    public static UserModel resolveUserOrSnapshot(KeycloakSession session, RealmModel realm, String userId) {
        if (session == null || realm == null || userId == null) {
            return null;
        }
        UserModel user = session.users().getUserById(realm, userId);
        return user != null ? user : lookup(session, realm, userId);
    }

    /**
     * Resolves the organizations a user belonged to, whether live or purged.
     *
     * <p>For a live user this is {@code getByMember}. For a snapshot the captured
     * aliases are resolved back to live {@link OrganizationModel}s — the organizations
     * outlive the user, so callers get real models and can apply the same logic in
     * both cases. Aliases that no longer resolve (the organization was deleted in the
     * same request) are dropped.
     */
    public static List<OrganizationModel> organizationsOf(KeycloakSession session, UserModel user) {
        if (session == null || user == null || !Organizations.isEnabled(session)) {
            return List.of();
        }
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        if (user instanceof PurgedUserSnapshot snapshot) {
            List<OrganizationModel> resolved = new ArrayList<>();
            for (String alias : snapshot.getOrganizationAliases()) {
                OrganizationModel org = orgProvider.getByAlias(alias);
                if (org != null) {
                    resolved.add(org);
                }
            }
            return resolved;
        }
        return orgProvider.getByMember(user).toList();
    }

    private static boolean matchesRealm(PurgedUserSnapshot snapshot, RealmModel realm) {
        if (snapshot == null) {
            return false;
        }
        if (realm == null || snapshot.getRealm() == null) {
            return true;
        }
        return realm.getId().equals(snapshot.getRealm().getId());
    }

    private static String sessionAttributeKey(String userId) {
        return SESSION_ATTRIBUTE_PREFIX + userId;
    }

    private static String sessionEmailAttributeKey(String email) {
        // Same normalization AbstractInMemoryUserAdapter applies when storing the
        // email, so the index key matches what getEmail() returns.
        return SESSION_ATTRIBUTE_EMAIL_PREFIX + KeycloakModelUtils.toLowerCaseSafe(email);
    }
}
