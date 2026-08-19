package org.keycloak.ssf.transmitter.subject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
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
 * <p><b>Organizations are the exception.</b> {@code OrganizationProvider.getByMember(user)}
 * is an id-keyed database query, not an attribute read, so a detached user cannot
 * answer it. The organization-derived facts the pipeline needs — the tenant alias for
 * {@code +tenant} subject formats, and the org-level notify / tombstone verdicts —
 * are resolved eagerly at capture time and served from the fields below.
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
     * The {@code ssf.*} attributes of every organization the user belonged to, in
     * membership order. Backs the org legs of the subject filter, which ask
     * "is <em>any</em> of the user's organizations notified / excluded".
     */
    private final List<Map<String, List<String>>> organizationSsfAttributes;

    protected PurgedUserSnapshot(KeycloakSession session,
                                 RealmModel realm,
                                 String id,
                                 String tenantAlias,
                                 List<Map<String, List<String>>> organizationSsfAttributes) {
        super(session, realm, id);
        this.tenantAlias = tenantAlias;
        this.organizationSsfAttributes = organizationSsfAttributes;
    }

    public String getTenantAlias() {
        return tenantAlias;
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
     * query on every client deletion.
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

        String tenantAlias = null;
        List<Map<String, List<String>>> orgAttributes = new ArrayList<>();
        if (Organizations.isEnabled(session)) {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            List<OrganizationModel> organizations = orgProvider.getByMember(user).toList();
            for (OrganizationModel org : organizations) {
                orgAttributes.add(copySsfAttributes(org.getAttributes()));
            }
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
                tenantAlias, List.copyOf(orgAttributes));

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
     * Returns the snapshot captured for {@code userId} in this session, or
     * {@code null} when the user was not deleted on this request.
     */
    public static PurgedUserSnapshot lookup(KeycloakSession session, String userId) {
        if (session == null || userId == null) {
            return null;
        }
        return session.getAttribute(sessionAttributeKey(userId), PurgedUserSnapshot.class);
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
    public static PurgedUserSnapshot lookupBySubject(KeycloakSession session, SubjectId subjectId) {
        if (session == null || subjectId == null) {
            return null;
        }
        if (subjectId instanceof ComplexSubjectId complex) {
            return lookupBySubject(session, complex.getUser());
        }
        if (subjectId instanceof IssuerSubjectId issuerSubject) {
            return lookup(session, issuerSubject.getSub());
        }
        if (subjectId instanceof OpaqueSubjectId opaqueSubject) {
            return lookup(session, opaqueSubject.getId());
        }
        if (subjectId instanceof EmailSubjectId emailSubject) {
            String email = emailSubject.getEmail();
            if (email == null || email.isBlank()) {
                return null;
            }
            return session.getAttribute(sessionEmailAttributeKey(email), PurgedUserSnapshot.class);
        }
        return null;
    }

    /**
     * Resolves {@code userId} to a live user, falling back to this request's
     * purge snapshot when the row is already gone. The single substitution point
     * the emission path uses in place of a bare {@code getUserById}.
     */
    public static UserModel resolveUserOrSnapshot(KeycloakSession session, RealmModel realm, String userId) {
        if (session == null || userId == null) {
            return null;
        }
        UserModel user = realm != null ? session.users().getUserById(realm, userId) : null;
        return user != null ? user : lookup(session, userId);
    }

    public boolean isAnyOrganizationNotified(String clientId) {
        return anyOrganizationHasValue(SsfNotifyAttributes.attributeKey(clientId),
                SsfNotifyAttributes.ATTRIBUTE_VALUE_TRUE);
    }

    public boolean isAnyOrganizationExcluded(String clientId) {
        return anyOrganizationHasValue(SsfNotifyAttributes.attributeKey(clientId),
                SsfNotifyAttributes.ATTRIBUTE_VALUE_FALSE);
    }

    /**
     * The most recent org-level removal tombstone across the user's organizations,
     * or {@code null} when none carry one. Most-recent mirrors the live filter's
     * {@code anyMatch} over memberships — if any organization is still inside the
     * grace window, the event is delivered.
     */
    public Long getOrganizationRemovedAt(String clientId) {
        String key = SsfNotifyAttributes.removedAtKey(clientId);
        Long latest = null;
        for (Map<String, List<String>> attributes : organizationSsfAttributes) {
            List<String> values = attributes.get(key);
            if (values == null || values.isEmpty()) {
                continue;
            }
            Long parsed = parseEpochSeconds(values.get(0));
            if (parsed != null && (latest == null || parsed > latest)) {
                latest = parsed;
            }
        }
        return latest;
    }

    private boolean anyOrganizationHasValue(String key, String value) {
        for (Map<String, List<String>> attributes : organizationSsfAttributes) {
            List<String> values = attributes.get(key);
            if (values != null && values.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static Long parseEpochSeconds(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Narrows an organization's attribute map to the {@code ssf.*} keys. Organization
     * attributes are operator-defined and unbounded; only the SSF ones are ever read
     * back, so the snapshot does not retain the rest.
     */
    private static Map<String, List<String>> copySsfAttributes(Map<String, List<String>> attributes) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        if (attributes == null) {
            return copy;
        }
        for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
            String key = entry.getKey();
            if (key == null || entry.getValue() == null) {
                continue;
            }
            if (key.startsWith(SsfNotifyAttributes.ATTRIBUTE_PREFIX)
                    || key.startsWith(SsfNotifyAttributes.REMOVED_AT_PREFIX)) {
                copy.put(key, List.copyOf(entry.getValue()));
            }
        }
        return copy;
    }

    private static String sessionAttributeKey(String userId) {
        return SESSION_ATTRIBUTE_PREFIX + userId;
    }

    private static String sessionEmailAttributeKey(String email) {
        return SESSION_ATTRIBUTE_EMAIL_PREFIX + email.toLowerCase();
    }
}
