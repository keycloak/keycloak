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
     * URI tenant scheme ({@code urn:keycloak:org:<alias>}) for Organizations: the only
     * URI form {@link #resolveOrganization} resolves to a local
     * organization — arbitrary URIs must not resolve, see the
     * {@code uri} branch there.
     */
    public static final String ORG_URN_PREFIX = "urn:keycloak:org:";

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
     * contract for tenant subject members — {@code opaque} (org id or alias),
     * {@code iss_sub} (sub as org id, realm issuer only), {@code email}
     * (org domain or alias) and {@code uri}
     * ({@code urn:keycloak:org:<alias>} only) are all understood.
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

        // opaque id → getById
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

        // uri → only the Keycloak-scoped URN form
        // ("urn:keycloak:org:<alias>") is accepted. An earlier revision
        // also took arbitrary https URIs and resolved their last path
        // segment as an alias, but — like the foreign-iss_sub case
        // above — the sub_id travels verbatim in emitted SETs, so
        // resolving "https://evil.example/orgs/acme" against the local
        // org "acme" would let an emitter tie a local organization to
        // a URI namespace Keycloak never validated. The URN scheme is
        // the only URI form this server can authoritatively interpret.
        if (tenantSubject instanceof UriSubjectId uriSubject) {
            String alias = extractOrgAliasFromUri(uriSubject.getUri());
            if (alias != null) {
                return resolveOrgByAliasOrDomain(orgProvider, alias);
            }
            return SubjectResolution.NOT_FOUND;
        }

        return SubjectResolution.UNSUPPORTED_FORMAT;
    }

    private static SubjectResolution resolveOrgById(OrganizationProvider orgProvider, String orgId) {
        if (orgId == null) {
            return SubjectResolution.UNSUPPORTED_FORMAT;
        }
        var org = orgProvider.getById(orgId);
        if (org != null) {
            return new SubjectResolution.Organization(org);
        }
        // id didn't match — try as alias
        org = orgProvider.getByAlias(orgId);
        if (org != null) {
            return new SubjectResolution.Organization(org);
        }
        return SubjectResolution.NOT_FOUND;
    }

    private static SubjectResolution resolveOrgByAliasOrDomain(OrganizationProvider orgProvider, String value) {
        var org = orgProvider.getByAlias(value);
        if (org != null) {
            return new SubjectResolution.Organization(org);
        }
        org = orgProvider.getByDomainName(value);
        if (org != null) {
            return new SubjectResolution.Organization(org);
        }
        return SubjectResolution.NOT_FOUND;
    }

    /**
     * Extracts the org alias from a {@code urn:keycloak:org:<alias>}
     * URN. Any other URI shape yields {@code null} — see the caller's
     * comment for why arbitrary URIs must not resolve to local orgs.
     */
    private static String extractOrgAliasFromUri(String uri) {
        if (uri == null || !uri.startsWith(ORG_URN_PREFIX)) {
            log.debugf("Tenant URI subject is not a urn:keycloak:org URN — not resolvable. uri=%s", uri);
            return null;
        }
        String alias = uri.substring(ORG_URN_PREFIX.length());
        return alias.isBlank() ? null : alias;
    }
}
