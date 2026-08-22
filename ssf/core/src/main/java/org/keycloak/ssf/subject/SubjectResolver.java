package org.keycloak.ssf.subject;

import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.utils.Organizations;

import org.jboss.logging.Logger;

/**
 * Resolves a {@link SubjectId} to a concrete Keycloak entity. Widens
 * {@link SubjectUserLookup}'s scope to also handle organization
 * subjects via {@link ComplexSubjectId#getTenant()}.
 *
 * <p>Supported subject shapes:
 * <ul>
 *     <li>User-identifying ({@code email}, {@code iss_sub}, user-opaque)
 *         → resolves to a {@link UserModel}.</li>
 *     <li>Complex subject with a {@code tenant} component whose inner
 *         subject carries an opaque id → resolves to an
 *         {@code OrganizationModel} (only when
 *         {@link Profile.Feature#ORGANIZATION} is enabled).</li>
 * </ul>
 * Everything else yields {@link SubjectResolution.UnsupportedFormat}.
 */
public class SubjectResolver {

    /**
     * Base namespace of the URI tenant scheme for Organizations. Not
     * resolvable on its own: an explicit identifier segment is
     * required — {@link #ORG_URN_ALIAS_PREFIX} or
     * {@link #ORG_URN_ID_PREFIX} — so a URN always states which lookup
     * it wants instead of relying on alias/id heuristics. These URNs
     * are the only URI forms {@link #resolveOrganization} resolves to
     * a local organization — arbitrary URIs must not resolve, see the
     * {@code uri} branch there.
     */
    public static final String ORG_URN_PREFIX = "urn:keycloak:org:";

    /** {@code urn:keycloak:org:alias:<alias>} — resolves strictly by organization alias. */
    public static final String ORG_URN_ALIAS_PREFIX = ORG_URN_PREFIX + "alias:";

    /** {@code urn:keycloak:org:id:<id>} — resolves strictly by internal organization id. */
    public static final String ORG_URN_ID_PREFIX = ORG_URN_PREFIX + "id:";

    private static final Logger log = Logger.getLogger(SubjectResolver.class);

    /**
     * Attempts to resolve the given subject to a Keycloak entity.
     */
    public static SubjectResolution resolve(KeycloakSession session, RealmModel realm, SubjectId subjectId) {

        if (subjectId instanceof ComplexSubjectId complex) {
            return resolveComplex(session, realm, complex);
        }

        UserModel user = SubjectUserLookup.lookupUser(session, realm, subjectId);
        if (user != null) {
            return new SubjectResolution.User(user);
        }

        if (subjectId instanceof EmailSubjectId
                || subjectId instanceof IssuerSubjectId
                || subjectId instanceof OpaqueSubjectId) {
            return SubjectResolution.NOT_FOUND;
        }

        return SubjectResolution.UNSUPPORTED_FORMAT;
    }

    private static SubjectResolution resolveComplex(KeycloakSession session, RealmModel realm, ComplexSubjectId complex) {

        // User component — try first, most common case.
        if (complex.getUser() != null) {
            UserModel user = SubjectUserLookup.lookupUser(session, realm, complex.getUser());
            if (user != null) {
                return new SubjectResolution.User(user);
            }
            return SubjectResolution.NOT_FOUND;
        }

        // Tenant component → Organization.
        if (complex.getTenant() != null) {
            return resolveOrganization(session, complex.getTenant());
        }

        return SubjectResolution.UNSUPPORTED_FORMAT;
    }

    /**
     * Resolves a tenant subject to an organization. This is the shared
     * contract for tenant subject members — {@code opaque} (internal
     * org id only — aliases go through the {@code alias:} URN form),
     * {@code iss_sub} (sub as internal org id, realm issuer only), {@code email}
     * (org domain or alias) and {@code uri}
     * ({@code urn:keycloak:org:alias:<alias>} or
     * {@code urn:keycloak:org:id:<id>}, each resolving strictly by the
     * named identifier) are all understood.
     * Callers that gate on "the supplied tenant subject member must
     * resolve" (e.g. the synthetic event emitter) must use this rather
     * than a narrower lookup so a tenant format accepted elsewhere is
     * not rejected there.
     */
    public static SubjectResolution resolveOrganization(KeycloakSession session, SubjectId tenantSubject) {
        if (!Organizations.isEnabled(session)) {
            log.debugf("Organization feature is disabled — cannot resolve tenant subject");
            return SubjectResolution.UNSUPPORTED_FORMAT;
        }

        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);

        // opaque id → internal org id only. Aliases are addressed via
        // the explicit urn:keycloak:org:alias: URN form instead, so an
        // opaque value never needs a lookup heuristic — a UUID-shaped
        // alias can't shadow another organization's id or vice versa.
        if (tenantSubject instanceof OpaqueSubjectId opaque) {
            return resolveOrgById(orgProvider, opaque.getId());
        }

        // iss_sub → sub as org id, accepted only for this realm's own
        // issuer. Unlike user iss_sub lookups there is no
        // federated-identity analog for organizations, so a foreign iss
        // cannot be mapped to anything this server has validated — and
        // callers like the synthetic emitter forward the sub_id
        // verbatim, so resolving the org regardless would let an
        // emitter mint a signed SET whose tenant ties a local
        // organization to an issuer Keycloak never checked.
        if (tenantSubject instanceof IssuerSubjectId issSub) {
            if (!SubjectUserLookup.isRealmIssuer(session, issSub.getIss())) {
                log.debugf("Tenant iss_sub subject carries a foreign issuer — not resolvable. iss=%s", issSub.getIss());
                return SubjectResolution.NOT_FOUND;
            }
            return resolveOrgById(orgProvider, issSub.getSub());
        }

        // email → treat as internet domain (e.g. "acme.com") → getByDomainName,
        // then fall back to alias
        if (tenantSubject instanceof EmailSubjectId email) {
            String domainOrAlias = email.getEmail();
            var org = orgProvider.getByDomainName(domainOrAlias);
            if (org != null) {
                return new SubjectResolution.Organization(org);
            }
            org = orgProvider.getByAlias(domainOrAlias);
            if (org != null) {
                return new SubjectResolution.Organization(org);
            }
            return SubjectResolution.NOT_FOUND;
        }

        // uri → only the Keycloak-scoped URN forms
        // urn:keycloak:org:alias:<alias> and urn:keycloak:org:id:<id>
        // are accepted, each resolving strictly by the identifier the
        // URN names. Earlier revisions took arbitrary https URIs
        // (resolving their last path segment as an alias) and a bare
        // urn:keycloak:org:<value> shorthand with heuristic lookup —
        // both are gone: the sub_id travels verbatim in emitted SETs,
        // so resolving "https://evil.example/orgs/acme" against the
        // local org "acme" would let an emitter tie a local
        // organization to a URI namespace Keycloak never validated,
        // and the explicit segment removes any alias/id ambiguity for
        // the one URI scheme this server authoritatively owns.
        if (tenantSubject instanceof UriSubjectId uriSubject) {
            return resolveOrgByUrn(orgProvider, uriSubject.getUri());
        }

        return SubjectResolution.UNSUPPORTED_FORMAT;
    }

    /**
     * Resolves a {@code urn:keycloak:org:} tenant URN, strictly by the
     * identifier kind its segment names ({@code alias:} / {@code id:},
     * no cross-fallback). Any other URI shape — including the bare
     * {@code urn:keycloak:org:<value>} form without a segment — yields
     * {@link SubjectResolution#NOT_FOUND}; see the caller's comment
     * for why arbitrary URIs must not resolve to local orgs.
     */
    private static SubjectResolution resolveOrgByUrn(OrganizationProvider orgProvider, String uri) {
        if (uri == null) {
            return SubjectResolution.NOT_FOUND;
        }
        if (uri.startsWith(ORG_URN_ALIAS_PREFIX)) {
            String alias = uri.substring(ORG_URN_ALIAS_PREFIX.length());
            var org = alias.isBlank() ? null : orgProvider.getByAlias(alias);
            return org != null ? new SubjectResolution.Organization(org) : SubjectResolution.NOT_FOUND;
        }
        if (uri.startsWith(ORG_URN_ID_PREFIX)) {
            String id = uri.substring(ORG_URN_ID_PREFIX.length());
            var org = id.isBlank() ? null : orgProvider.getById(id);
            return org != null ? new SubjectResolution.Organization(org) : SubjectResolution.NOT_FOUND;
        }
        log.debugf("Tenant URI subject is not a urn:keycloak:org:{alias|id} URN — not resolvable. uri=%s", uri);
        return SubjectResolution.NOT_FOUND;
    }

    /**
     * Strict internal-id lookup — deliberately no alias fallback.
     * Every tenant identifier states its lookup kind now (opaque /
     * iss_sub {@code sub} = internal id, URN segments = explicit), and
     * Keycloak's own producers serialize the self-describing
     * {@code urn:keycloak:org:alias:} form, so a heuristic here would
     * only reintroduce the alias/id shadowing it exists to prevent.
     */
    private static SubjectResolution resolveOrgById(OrganizationProvider orgProvider, String orgId) {
        if (orgId == null) {
            return SubjectResolution.UNSUPPORTED_FORMAT;
        }
        var org = orgProvider.getById(orgId);
        if (org != null) {
            return new SubjectResolution.Organization(org);
        }
        return SubjectResolution.NOT_FOUND;
    }

}
