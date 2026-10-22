/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.migration.migrators;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.keycloak.utils.EmailValidationUtil;

import org.jboss.logging.Logger;

import static org.keycloak.models.OrganizationDomainModel.ANY_DOMAIN;

/**
 * Computes how the 26.7 domain settings of organization identity providers ({@code kc.org.domain},
 * {@code kc.org.excluded.domains} and {@code kc.org.broker.redirect.mode.email-matches}) map to the 26.8 routing on
 * organization domains, for the database migration and for the import of a realm exported before 26.8.
 *
 * <p>In 26.7, an email was redirected to the first identity provider with {@code email-matches} enabled that did not
 * exclude its domain and that either had the domain {@code ANY} or the organization domain that matched the email most
 * specifically. In 26.8, the most specific matching organization domain carries the identity provider. So:
 * <ul>
 *     <li>an organization domain is routed to the first identity provider that redirected its emails in 26.7, or, if
 *     none did, to the first identity provider without {@code email-matches} linked to it ({@code ANY} or the same
 *     domain);</li>
 *     <li>an excluded domain of an identity provider with {@code email-matches} that is strictly narrower than an
 *     organization domain is added to the organization, routed to the identity provider that redirected its emails in
 *     26.7, or unrouted if none did. It is not added when its emails went to the same identity provider as the other
 *     emails of the organization domain;</li>
 *     <li>any other excluded domain had no effect on the redirect, and is not migrated.</li>
 * </ul>
 * In 26.7 the order of the identity providers of an organization was not defined (with the realm cache it came from a
 * hash set). When several identity providers redirected the same emails, the migration takes the first by alias and
 * logs the others.
 *
 * <p>An organization domain name is unique in a realm, so an excluded domain is not added when the database would see
 * it as the name of another domain, or when it overlaps a domain of another organization that is at least as
 * specific (by the 26.8 rules) as the organization domain it is part of, so that adding it would move email domains to
 * this organization. The result only depends on the data, not on the order in which it is read.
 */
public final class DomainIdpRoutingMigration {

    private static final Logger logger = Logger.getLogger(DomainIdpRoutingMigration.class);

    private static final String WILDCARD_PREFIX = "*.";
    private static final int MIN_DOMAIN_PARTS = 2;
    private static final int MAX_DOMAIN_PARTS = 10;

    /**
     * A domain of an organization, as it was before the migration.
     */
    public record Domain(String organizationId, String name) {
    }

    /**
     * An identity provider of an organization with its 26.7 domain settings.
     *
     * @param domain the value of {@code kc.org.domain}, or {@code null}
     * @param excludedDomains the value of {@code kc.org.excluded.domains}, or {@code null}
     * @param autoRedirect the value of {@code kc.org.broker.redirect.mode.email-matches}
     */
    public record IdentityProvider(String id, String alias, String organizationId, String domain, String excludedDomains,
                                   boolean autoRedirect) {
    }

    /**
     * Routes the existing domain {@code name} of the organization to the identity provider.
     */
    public record Routing(String organizationId, String name, IdentityProvider identityProvider) {
    }

    /**
     * The existing domains to route, and the domains to add for excluded domains, for one realm. The identity provider
     * of a domain to add is {@code null} if the domain is unrouted.
     */
    public record Result(List<Routing> routings, List<Routing> newDomains) {
    }

    private record Candidate(IdentityProvider identityProvider, String name, String key, String matchingDomain,
                             IdentityProvider redirect) {
    }

    private DomainIdpRoutingMigration() {
    }

    /**
     * @param organizationNames the names of the organizations of the realm by ID, used in log messages
     * @param domains all organization domains of the realm
     * @param identityProviders all identity providers of the realm that are linked to an organization
     */
    public static Result migrate(Map<String, String> organizationNames, Collection<Domain> domains,
                                 Collection<IdentityProvider> identityProviders) {
        Map<String, List<String>> domainsByOrganization = new HashMap<>();
        domains.forEach(domain -> domainsByOrganization.computeIfAbsent(domain.organizationId(), id -> new ArrayList<>()).add(domain.name()));
        domainsByOrganization.values().forEach(names -> names.sort(Comparator.naturalOrder()));

        // organization ID -> identity providers in the order the migration uses: email-matches first, then by alias
        Map<String, List<IdentityProvider>> identityProvidersByOrganization = new TreeMap<>();
        identityProviders.stream()
                .filter(idp -> idp.domain() != null)
                .sorted(Comparator.comparing(IdentityProvider::autoRedirect).reversed()
                        .thenComparing(IdentityProvider::alias, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(IdentityProvider::alias, Comparator.nullsFirst(Comparator.naturalOrder())))
                .forEach(idp -> identityProvidersByOrganization.computeIfAbsent(idp.organizationId(), id -> new ArrayList<>()).add(idp));

        List<Routing> routings = new ArrayList<>();
        List<Candidate> candidates = new ArrayList<>();

        identityProvidersByOrganization.forEach((organizationId, organizationIdentityProviders) -> {
            String organizationName = organizationNames.get(organizationId);
            List<String> organizationDomains = domainsByOrganization.getOrDefault(organizationId, List.of());
            Map<String, IdentityProvider> owners = new HashMap<>();

            for (String name : organizationDomains) {
                IdentityProvider owner = findIdentityProvider(organizationIdentityProviders, name, name, organizationName);
                owners.put(name, owner);
                if (owner != null) {
                    routings.add(new Routing(organizationId, name, owner));
                }
            }

            Set<String> seen = new HashSet<>();

            for (IdentityProvider idp : organizationIdentityProviders) {
                if (!idp.autoRedirect()) {
                    // the excluded domains of an identity provider without email-matches had no effect
                    continue;
                }

                for (String excluded : parseDomains(idp.excludedDomains())) {
                    if (organizationDomains.stream().anyMatch(excluded::equalsIgnoreCase) || !seen.add(excluded)) {
                        // organization domains are routed above
                        continue;
                    }

                    String matchingDomain = organizationDomains.stream()
                            .filter(name -> isNarrower(excluded, name))
                            .max(DomainIdpRoutingMigration::compareSpecificity)
                            .orElse(null);

                    if (matchingDomain == null) {
                        logger.infof("Excluded domain '%s' of identity provider '%s' is not migrated: it is not part of a domain of organization '%s', so it had no effect",
                                excluded, idp.alias(), organizationName);
                        continue;
                    }

                    IdentityProvider redirect = findIdentityProvider(organizationIdentityProviders, excluded, matchingDomain, organizationName);

                    if (Objects.equals(redirect, owners.get(matchingDomain))) {
                        // its emails went to the same identity provider as the other emails of the domain
                        continue;
                    }

                    if (!isValidDomain(excluded)) {
                        logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: it is not a valid organization domain",
                                excluded, idp.alias());
                        continue;
                    }

                    candidates.add(new Candidate(idp, excluded, toKey(excluded), matchingDomain, redirect));
                }
            }
        });

        return new Result(routings, addExcludedDomains(organizationNames, domains, candidates));
    }

    /**
     * The identity provider that 26.7 redirected the emails of {@code domain} to, given that their most specific
     * organization domain is {@code matchingDomain}. If none did, the first identity provider without email-matches
     * linked to it; its excluded domains had no effect.
     */
    private static IdentityProvider findIdentityProvider(List<IdentityProvider> identityProviders, String domain,
                                                         String matchingDomain, String organizationName) {
        List<IdentityProvider> found = identityProviders.stream()
                .filter(idp -> ANY_DOMAIN.equals(idp.domain()) || idp.domain().equals(matchingDomain))
                .filter(idp -> !idp.autoRedirect() || parseDomains(idp.excludedDomains()).stream().noneMatch(excluded -> covers(excluded, domain)))
                .toList();

        List<String> redirecting = found.stream().filter(IdentityProvider::autoRedirect).map(IdentityProvider::alias).toList();

        if (redirecting.size() > 1) {
            logger.infof("Domain '%s' of organization '%s' is routed to identity provider '%s'. In 26.7 it could be redirected to any of %s, in no defined order",
                    domain, organizationName, redirecting.get(0), redirecting);
        }

        return found.isEmpty() ? null : found.get(0);
    }

    private static List<Routing> addExcludedDomains(Map<String, String> organizationNames, Collection<Domain> domains,
                                                   List<Candidate> candidates) {
        // by key, so that names that a case- or accent-insensitive database collation treats as equal end up together
        Map<String, Map<String, Candidate>> candidatesByKey = new TreeMap<>();

        for (Candidate candidate : candidates) {
            String organizationId = candidate.identityProvider().organizationId();
            Domain conflict = domains.stream()
                    .filter(domain -> isInConflict(candidate, domain))
                    .findFirst()
                    .orElse(null);

            if (conflict != null) {
                if (conflict.organizationId().equals(organizationId)) {
                    logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: the database may treat it as the same name as the domain '%s' of organization '%s', so the email domains it excluded are routed again. Review the domains of organization '%s'",
                            candidate.name(), candidate.identityProvider().alias(), conflict.name(), organizationNames.get(organizationId),
                            organizationNames.get(organizationId));
                } else {
                    logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: it overlaps the domain '%s' of organization '%s'. Review the domains of organization '%s'",
                            candidate.name(), candidate.identityProvider().alias(), conflict.name(),
                            organizationNames.get(conflict.organizationId()), organizationNames.get(organizationId));
                }
                continue;
            }

            Map<String, Candidate> byOrganization = candidatesByKey.computeIfAbsent(candidate.key(), key -> new TreeMap<>());
            // two identity providers of one organization may exclude the same domain, possibly in different case
            byOrganization.merge(organizationId, candidate, (a, b) -> a.name().compareTo(b.name()) <= 0 ? a : b);
        }

        List<Routing> newDomains = new ArrayList<>();

        for (Map<String, Candidate> byOrganization : candidatesByKey.values()) {
            if (byOrganization.size() > 1) {
                // only reachable with names that differ in case or accents but not as domains
                byOrganization.values().forEach(candidate -> logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: another organization excludes a domain with the same name",
                        candidate.name(), candidate.identityProvider().alias()));
                continue;
            }

            Candidate candidate = byOrganization.values().iterator().next();
            newDomains.add(new Routing(candidate.identityProvider().organizationId(), candidate.name(), candidate.redirect()));
        }

        return newDomains;
    }

    /**
     * Whether adding the candidate would conflict with the existing domain: the names are equal for the database,
     * or the domain belongs to another organization and decided between the organizations for some email domains
     * of the candidate, at least as specific as the domain of the organization that the candidate is part of.
     */
    private static boolean isInConflict(Candidate candidate, Domain domain) {
        if (candidate.key().equals(toKey(domain.name()))) {
            return true;
        }

        return !domain.organizationId().equals(candidate.identityProvider().organizationId())
                && overlaps(candidate.name(), domain.name())
                && compareSpecificity(domain.name(), candidate.matchingDomain()) >= 0
                && compareSpecificity(domain.name(), candidate.name()) <= 0;
    }

    private static Set<String> parseDomains(String value) {
        Set<String> domains = new LinkedHashSet<>();

        if (value != null) {
            for (String domain : value.split(",")) {
                String trimmed = domain.trim();
                if (!trimmed.isEmpty()) {
                    domains.add(trimmed);
                }
            }
        }

        return domains;
    }

    /**
     * Whether all email domains that match {@code domain} also match {@code excluded}.
     */
    static boolean covers(String excluded, String domain) {
        if (isWildcard(domain)) {
            return isWildcard(excluded) && isSameDomain(baseDomain(domain), excluded);
        }
        return isSameDomain(domain, excluded);
    }

    /**
     * Whether all email domains that match {@code excluded} also match {@code domain}, but not the other way around.
     */
    static boolean isNarrower(String excluded, String domain) {
        return isSameDomain(baseDomain(excluded), domain) && !covers(excluded, domain);
    }

    /**
     * Whether an email domain exists that matches both domains.
     */
    static boolean overlaps(String domain, String otherDomain) {
        return isSameDomain(baseDomain(domain), otherDomain) || isSameDomain(baseDomain(otherDomain), domain);
    }

    /**
     * Orders domains as {@code Organizations.getMatchingDomain}: more parts are more specific, and with the same number
     * of parts an exact domain is more specific than a wildcard domain.
     */
    static int compareSpecificity(String domain, String otherDomain) {
        int result = Integer.compare(getPartsSize(baseDomain(domain)), getPartsSize(baseDomain(otherDomain)));
        return result != 0 ? result : Boolean.compare(!isWildcard(domain), !isWildcard(otherDomain));
    }

    /**
     * Same as {@code Organizations.isSameDomain}.
     */
    private static boolean isSameDomain(String domain, String expectedDomain) {
        String canonicalDomain = domain.toLowerCase();
        String pattern = expectedDomain.toLowerCase();

        if (canonicalDomain.equals(pattern)) {
            return true;
        }

        if (pattern.startsWith(WILDCARD_PREFIX)) {
            String baseDomain = pattern.substring(2);
            return canonicalDomain.equals(baseDomain) || canonicalDomain.endsWith("." + baseDomain);
        }

        return false;
    }

    /**
     * Same rules as {@code Organizations.validateDomain}.
     */
    static boolean isValidDomain(String domain) {
        String name = domain;

        if (domain.contains(WILDCARD_PREFIX)) {
            if (!domain.startsWith(WILDCARD_PREFIX)) {
                return false;
            }

            name = domain.substring(WILDCARD_PREFIX.length());
            int parts = getPartsSize(name);

            if (name.contains("*") || parts < MIN_DOMAIN_PARTS || parts > MAX_DOMAIN_PARTS) {
                return false;
            }
        }

        return !name.isBlank() && EmailValidationUtil.isValidEmail("user@" + name);
    }

    /**
     * The name of a domain as a case- and accent-insensitive database collation compares it.
     */
    private static String toKey(String domain) {
        return Normalizer.normalize(domain, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private static boolean isWildcard(String domain) {
        return domain.startsWith(WILDCARD_PREFIX);
    }

    private static String baseDomain(String domain) {
        return isWildcard(domain) ? domain.substring(WILDCARD_PREFIX.length()) : domain;
    }

    private static int getPartsSize(String domain) {
        return Math.toIntExact(domain.chars().filter(c -> c == '.').count()) + 1;
    }
}
