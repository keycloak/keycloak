package org.keycloak.ssf.subject;

import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;

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

    private static SubjectResolution resolveOrganization(KeycloakSession session, SubjectId tenantSubject) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            log.debugf("Organization feature is disabled — cannot resolve tenant subject");
            return SubjectResolution.UNSUPPORTED_FORMAT;
        }

        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        if (orgProvider == null) {
            return SubjectResolution.UNSUPPORTED_FORMAT;
        }

        // opaque id → getById
        if (tenantSubject instanceof OpaqueSubjectId opaque) {
            return resolveOrgById(orgProvider, opaque.getId());
        }

        // iss_sub → sub as org id
        if (tenantSubject instanceof IssuerSubjectId issSub) {
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

        // uri → extract the path/fragment as alias or domain
        // e.g. "https://keycloak.example.com/orgs/acme" → "acme"
        //   or "urn:keycloak:org:acme" → "acme"
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

    private static String extractOrgAliasFromUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return null;
        }

        // Validate that the string is a syntactically valid URI before
        // attempting to extract an alias from it.
        java.net.URI parsed;
        try {
            parsed = java.net.URI.create(uri);
        } catch (IllegalArgumentException e) {
            log.debugf("Tenant URI subject is not a valid URI: %s", uri);
            return null;
        }

        // urn:keycloak:org:<alias>
        if ("urn".equals(parsed.getScheme()) && uri.startsWith("urn:keycloak:org:")) {
            String alias = uri.substring("urn:keycloak:org:".length());
            return alias.isBlank() ? null : alias;
        }

        // https://example.com/orgs/acme → last path segment "acme"
        String path = parsed.getPath();
        if (path != null && !path.isBlank()) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                return path.substring(lastSlash + 1);
            }
        }

        return null;
    }
}
