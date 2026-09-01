package org.keycloak.ssf.transmitter.subject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
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
import org.keycloak.ssf.transmitter.support.SsfUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractInMemoryUserAdapter;

import org.jboss.logging.Logger;

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

    private static final Logger log = Logger.getLogger(PurgedUserSnapshot.class);

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

    /**
     * Whether the removal only dropped Keycloak's local copy, leaving the account
     * itself in place — see {@link #isLocalRemovalOnly}.
     */
    private final boolean localRemovalOnly;

    protected PurgedUserSnapshot(KeycloakSession session,
                                 RealmModel realm,
                                 String id,
                                 String tenantAlias,
                                 List<String> organizationAliases,
                                 boolean localRemovalOnly) {
        super(session, realm, id);
        this.tenantAlias = tenantAlias;
        this.organizationAliases = organizationAliases;
        this.localRemovalOnly = localRemovalOnly;
    }

    /**
     * True when the deletion removed only Keycloak's local copy of a federated user
     * while the authoritative store kept the account.
     *
     * <p>{@code LDAPStorageProvider.removeUser} returns {@code true} without touching
     * the directory when the provider's edit mode is {@code READ_ONLY} or
     * {@code UNSYNCED} — its own log line says the user "will be re-imported from LDAP
     * again once searched in Keycloak". That {@code true} reaches
     * {@code UserResource.deleteUser} and {@code DeleteAccount} as a successful
     * deletion, and both then fire the event this transmitter maps to
     * {@code account-purged}.
     *
     * <p>Receivers drive data-retention deletion off that event, so emitting it for an
     * account that still exists and will reappear at the next lookup is worse than
     * emitting nothing: the downstream deletion is not reversible. The purge generator
     * therefore drops the event when this is set. {@code READ_ONLY} is the default LDAP
     * edit mode, so this is the common federation setup rather than an edge case.
     *
     * <p>LDAP is the motivating case, not the rule. Because a wrongly emitted purge is
     * not recoverable downstream, the test is deliberately conservative: everything is
     * treated as a local removal unless the deletion is known to be authoritative — see
     * {@link #isLocalRemovalOnly(RealmModel, UserModel)} for what qualifies and why an
     * absent edit mode does not.
     */
    public boolean isLocalRemovalOnly() {
        return localRemovalOnly;
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
     * The realm the user was deleted from. Session keys are scoped by realm id, so
     * a snapshot can only be found from the realm it was captured in.
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
     * Captures {@code user} into this request's {@link PurgedUserSnapshots}, indexed by
     * user id and, when the user has one, by email.
     *
     * <p>No-op when the user is a service account — those are not human subjects and
     * never produce a purge SET, so capturing one would only cost an organization
     * query on every client deletion; when the deletion is not bound to an HTTP
     * request (see {@link #isRequestBound}); and once
     * {@link PurgedUserSnapshots#MAX_SNAPSHOTS_PER_SESSION} snapshots are already
     * held for this request.
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

        PurgedUserSnapshots snapshots = PurgedUserSnapshots.of(session);
        if (snapshots.isFull()) {
            // Truncation is logged at WARN rather than DEBUG: dropping a snapshot can
            // only ever cost an event, and that must never happen quietly. The latch
            // lives on the set rather than being inferred from its size, so a bulk
            // request leaves one line instead of thousands while a later discard still
            // frees a slot that the next capture can use.
            if (snapshots.markBoundWarned()) {
                log.warnf("SSF: reached %d purge snapshots in a single request; further deletions in it "
                                + "will not be captured while the set stays full, and any purge events they "
                                + "would have produced are lost. Expected for bulk deletion (partial import "
                                + "OVERWRITE, organization removal), which do not emit purge events.",
                        PurgedUserSnapshots.MAX_SNAPSHOTS_PER_SESSION);
            }
            return;
        }

        String tenantAlias = null;
        List<String> organizationAliases = List.of();
        if (Organizations.isEnabled(session)) {
            // Read without authorization filtering — see organizationsOf.
            CapturedOrganizations organizations = AdminPermissionsSchema.runWithoutAuthorization(
                    session, () -> captureOrganizations(session, user));
            organizationAliases = organizations.aliases();
            tenantAlias = organizations.tenantAlias();
        }

        PurgedUserSnapshot snapshot = new PurgedUserSnapshot(session, realm, user.getId(),
                tenantAlias, organizationAliases, isLocalRemovalOnly(realm, user));

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

        // Group and role memberships matter because SsfSubjectInclusionResolver
        // documents group-attribute lookups and role-based opt-ins as supported
        // extension points: a resolver doing either would see an empty membership
        // set for a purged user and silently reach a different verdict than it does
        // for a live one. AbstractInMemoryUserAdapter stores only the ids and
        // resolves them back through the realm on read, so — exactly like the
        // organization aliases above — nothing about the groups or roles themselves
        // is copied, and they outlive the user. The membership rows are deleted by
        // JpaUserProvider.removeUser, which runs after this hook, so they are still
        // readable here.
        user.getGroupsStream().forEach(snapshot::joinGroup);
        user.getRoleMappingsStream().forEach(snapshot::grantRole);

        snapshot.setReadonly(true);

        snapshots.put(idKey(realm, user.getId()), emailKey(realm, snapshot.getEmail()), snapshot);
    }

    /**
     * The organization facts the snapshot keeps: the alias of every membership, and
     * the managed-preferred primary used for {@code +tenant} subjects.
     */
    private record CapturedOrganizations(List<String> aliases, String tenantAlias) {
    }

    /**
     * Reads the user's memberships while the user still exists. Must be called inside
     * {@code runWithoutAuthorization} — see {@link #organizationsOf}.
     */
    private static CapturedOrganizations captureOrganizations(KeycloakSession session, UserModel user) {
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        List<OrganizationModel> organizations = orgProvider.getByMember(user).toList();
        // Managed-preferred, matching the mapper's multi-org resolution policy:
        // the organization that provisioned the user wins, otherwise the first
        // membership. Keeps the purge event's tenant subject identical to the
        // one every earlier event for this user carried.
        OrganizationModel tenant = organizations.stream()
                .filter(candidate -> orgProvider.isManagedMember(candidate, user))
                .findFirst()
                .orElseGet(() -> organizations.stream().findFirst().orElse(null));
        return new CapturedOrganizations(
                organizations.stream().map(OrganizationModel::getAlias).toList(),
                tenant == null ? null : tenant.getAlias());
    }

    /**
     * Resolves whether removing {@code user} will leave the account in place in its
     * authoritative store — see {@link #isLocalRemovalOnly()}.
     *
     * <p>Read while the user still exists, because the answer lives on the federation
     * provider the user links to and nothing can be asked about it afterwards.
     *
     * <p><b>Unknown never means "purged".</b> Only two cases are treated as a real
     * removal: a user with no federation link at all, and a provider whose edit mode is
     * explicitly {@code WRITABLE}, which is the mode that propagates the delete
     * upstream. Everything else — {@code READ_ONLY}, {@code UNSYNCED}, an edit mode that
     * is absent, or a federation link that no longer resolves — is treated as a local
     * removal and suppresses the event.
     *
     * <p>Erring this way is not symmetric bookkeeping. A purge a receiver should not
     * have been told about drives data-retention deletion that cannot be undone; a purge
     * it is not told about only costs it a signal. Reading an absent edit mode as a real
     * removal was wrong for exactly that reason: providers disagree on what the missing
     * value means, and {@code KerberosConfig.getEditMode} defaults it to
     * {@code UNSYNCED} while {@code KerberosFederationProvider.removeUser} returns
     * {@code true} without deleting the principal at any edit mode.
     *
     * <p>{@code WRITABLE} stays an allowlist rather than provider-specific knowledge.
     * Kerberos does not offer it — {@code KerberosFederationProviderFactory} exposes only
     * {@code READ_ONLY} and {@code UNSYNCED} — so in practice it selects LDAP, plus any
     * third-party provider that declares the mode. A provider declaring {@code WRITABLE}
     * without actually deleting upstream would still emit; that is its own claim to make,
     * and an allowlist of provider ids here would only go stale.
     */
    private static boolean isLocalRemovalOnly(RealmModel realm, UserModel user) {
        String federationLink = user.getFederationLink();
        if (federationLink == null) {
            return false;
        }
        ComponentModel component = realm.getComponent(federationLink);
        if (component == null) {
            return true;
        }
        String editMode = component.getConfig().getFirst(LDAPConstants.EDIT_MODE);
        return !UserStorageProvider.EditMode.WRITABLE.name().equals(editMode);
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
        if (session == null || realm == null || userId == null) {
            return null;
        }
        PurgedUserSnapshots snapshots = PurgedUserSnapshots.find(session);
        return snapshots == null ? null : snapshots.byId(idKey(realm, userId));
    }

    /**
     * Drops the snapshot for {@code userId}, called by the event listener once it has
     * finished emitting for that deletion.
     *
     * <p>This is what keeps {@link PurgedUserSnapshots#MAX_SNAPSHOTS_PER_SESSION} out
     * of the way of the paths that matter. A request that deletes users <em>and</em> emits for them
     * returns to zero retained snapshots after each one, so it can delete any number
     * without approaching the bound. Only snapshots that are never consumed accumulate
     * — which is precisely the bulk deletion case (partial import {@code OVERWRITE},
     * organization removal), where no purge event is produced and the snapshot was
     * dead weight from the moment it was taken.
     *
     * <p>Deliberately not conditional on the event being a purge: a user can only be
     * deleted once per request, so discarding after any event for that user is safe,
     * and a miss here costs nothing beyond retention.
     */
    public static void discard(KeycloakSession session, RealmModel realm, String userId) {
        if (session == null || realm == null || userId == null) {
            return;
        }
        PurgedUserSnapshots snapshots = PurgedUserSnapshots.find(session);
        if (snapshots == null) {
            return;
        }
        PurgedUserSnapshot snapshot = snapshots.byId(idKey(realm, userId));
        if (snapshot == null) {
            return;
        }
        snapshots.remove(idKey(realm, userId), emailKey(realm, snapshot.getEmail()));
    }

    /**
     * Resolves a snapshot from an RFC 9493 subject identifier, mirroring the
     * identifier shapes {@code SubjectUserLookup} understands. Used by the
     * dispatcher-side subject gate, which re-resolves the user from the token's own
     * {@code sub_id} rather than from the originating event.
     *
     * <p>Complex subjects are drilled into their {@code user} member, matching how
     * the filter unwraps them for live users.
     *
     * <p>An {@code iss_sub} subject only names a Keycloak user id when this
     * transmitter issued it — see {@link #isOwnIssuer}.
     */
    public static PurgedUserSnapshot lookupBySubject(KeycloakSession session, RealmModel realm, SubjectId subjectId) {
        if (session == null || realm == null || subjectId == null) {
            return null;
        }
        if (subjectId instanceof ComplexSubjectId complex) {
            return lookupBySubject(session, realm, complex.getUser());
        }
        if (subjectId instanceof IssuerSubjectId issuerSubject) {
            if (!isOwnIssuer(session, issuerSubject.getIss())) {
                return null;
            }
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
            PurgedUserSnapshots snapshots = PurgedUserSnapshots.find(session);
            return snapshots == null ? null : snapshots.byEmail(emailKey(realm, email));
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
     *
     * <p><b>Read without authorization filtering.</b> Both lookups are FGAP-filtered:
     * {@code getByMember} applies {@code AdminPermissionsSchema.applyAuthorizationFilters}
     * directly, and {@code getByAlias} defaults to {@code getAllStream}, which does the
     * same. Under fine-grained admin permissions an admin who may manage a user but not
     * view organizations would therefore see no memberships, and the org-derived
     * verdicts would silently change with the identity of whoever triggered the event:
     * an org-excluded subject would be delivered under {@code default_subjects=ALL}, an
     * org-notified one dropped under {@code NONE}, and a {@code +tenant} subject would
     * fail to build. What a receiver is told about a subject is system bookkeeping — it
     * cannot depend on the acting admin's visibility — so this reads as the system,
     * mirroring {@code UserStorageManager.isReadOnlyOrganizationMember} and
     * {@code DefaultLazyLoader}.
     *
     * <p>The result is materialized <em>inside</em> the unfiltered scope on purpose.
     * {@code runWithoutAuthorization} suppresses filtering by setting a session
     * attribute and clearing it in a {@code finally}, so a lazily returned stream would
     * run its query after the scope closed and be filtered after all.
     */
    public static List<OrganizationModel> organizationsOf(KeycloakSession session, UserModel user) {
        if (session == null || user == null || !Organizations.isEnabled(session)) {
            return List.of();
        }
        return AdminPermissionsSchema.runWithoutAuthorization(session, () -> {
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
        });
    }

    /**
     * Whether {@code iss} is the issuer this transmitter stamps onto the subjects it
     * builds, and therefore whether an {@code iss_sub} subject's {@code sub} is a
     * Keycloak user id at all.
     *
     * <p>Without this, the snapshot index would answer for any issuer:
     * {@code SubjectUserLookup} reads {@code sub} as an <em>external</em> IdP subject
     * whenever {@code iss} is not ours, resolving it through federated identity, and a
     * foreign {@code sub} that happened to equal a captured user id would otherwise
     * short-circuit to the wrong subject. Keeping the two in agreement matters more
     * now that the filter consults the snapshot <em>before</em> the live lookup for
     * purge events.
     *
     * <p>Compared against {@link SsfUtil#getIssuerUrl} rather than
     * {@code SubjectUserLookup.isRealmIssuer}: the transmitter's issuer is what
     * {@code SecurityEventTokenMapper.buildUserSubjectId} actually stamps, and it is
     * not always the realm issuer — a realm carrying a {@code frontendUrl} attribute
     * uses that value verbatim, and the fallback resolves the base URI without the
     * {@code FRONTEND} url type. Gating on the realm issuer would reject the
     * transmitter's own subjects in those deployments and drop every purge.
     *
     * <p>Any failure to resolve the issuer is treated as "not ours", which costs at
     * most a snapshot miss that the live lookup then handles.
     */
    private static boolean isOwnIssuer(KeycloakSession session, String iss) {
        if (iss == null) {
            return false;
        }
        try {
            return iss.equals(SsfUtil.getIssuerUrl(session));
        } catch (RuntimeException e) {
            log.debugf(e, "SSF: could not resolve transmitter issuer while matching purge snapshot for iss=%s", iss);
            return false;
        }
    }

    /**
     * Index keys are scoped by realm id so a snapshot can only ever be found from the
     * realm it was captured in. User ids are realm-unique in practice, but the
     * external-id component of a federated id is not guaranteed to be, and a
     * cross-realm hit would gate one realm's event on another realm's subject.
     * Scoping is free and removes the need to compare realms after the fact.
     */
    private static String idKey(RealmModel realm, String userId) {
        return realm.getId() + "." + userId;
    }

    /**
     * Key into the secondary index. The dispatcher's subject gate re-resolves the user
     * from the token's own {@code sub_id} rather than from the event's user id, and for
     * a stream configured with the {@code email} subject format that identifier is an
     * address, not a UUID. Indexing the snapshot by email as well lets that gate find
     * it without the filter having to know how the subject was built.
     *
     * <p>{@code null} for a user with no email, which the set stores as "no secondary
     * index entry" rather than indexing under a null key.
     */
    private static String emailKey(RealmModel realm, String email) {
        if (email == null) {
            return null;
        }
        // Same normalization AbstractInMemoryUserAdapter applies when storing the
        // email, so the index key matches what getEmail() returns.
        return realm.getId() + "." + KeycloakModelUtils.toLowerCaseSafe(email);
    }
}
