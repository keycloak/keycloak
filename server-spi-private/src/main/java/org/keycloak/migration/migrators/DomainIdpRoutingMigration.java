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

import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.keycloak.utils.EmailValidationUtil;

import org.jboss.logging.Logger;

import static org.keycloak.models.OrganizationDomainModel.ANY_DOMAIN;

/**
 * Maps the 26.7 domain settings of organization identity providers ({@code kc.org.domain},
 * {@code kc.org.excluded.domains}, {@code kc.org.broker.redirect.mode.email-matches}) to the 26.8 domain routing, for
 * the database migration and for the import of an older realm export.
 *
 * <p>Each domain is routed to the identity provider that 26.7 redirected its emails to. An excluded domain is added as
 * a domain only if it is part of an organization domain and its emails went elsewhere. In 26.7 the order of the
 * identity providers was not defined (the realm cache used a hash set), so ties are resolved by alias and logged.
 */
public final class DomainIdpRoutingMigration {

    private static final Logger logger = Logger.getLogger(DomainIdpRoutingMigration.class);

    private static final String WILDCARD_PREFIX = "*.";
    private static final int MIN_DOMAIN_PARTS = 2;
    private static final int MAX_DOMAIN_PARTS = 10;
    // the length of ORG_DOMAIN.NAME
    private static final int MAX_NAME_LENGTH = 255;
    private static final Pattern LETTERS_AND_DIGITS = Pattern.compile("[\\p{L}\\p{M}\\p{N}]+");

    public record Domain(String organizationId, String name) {
    }

    /**
     * An identity provider with the values of its {@code kc.org.*} settings.
     */
    public record IdentityProvider(String id, String alias, String organizationId, String domain, String excludedDomains,
                                   boolean autoRedirect) {
    }

    public record Routing(String organizationId, String name, IdentityProvider identityProvider) {
    }

    /**
     * The identity provider of a new domain is {@code null} if the domain is unrouted.
     */
    public record Result(List<Routing> routings, List<Routing> newDomains) {
    }

    private record Candidate(IdentityProvider identityProvider, String name, String matchingDomain,
                             IdentityProvider redirect) {

        String organizationId() {
            return identityProvider.organizationId();
        }
    }

    private DomainIdpRoutingMigration() {
    }

    /**
     * @param namesCheckedByDatabase {@code true} if the caller only inserts a new domain when the database, with its
     * collation, finds no domain with the same name; otherwise names that may be the same are not added
     */
    public static Result migrate(Map<String, String> organizationNames, Collection<Domain> domains,
                                 Collection<IdentityProvider> identityProviders, boolean namesCheckedByDatabase) {
        Map<String, List<String>> domainsByOrganization = new HashMap<>();
        domains.forEach(domain -> domainsByOrganization.computeIfAbsent(domain.organizationId(), id -> new ArrayList<>()).add(domain.name()));
        domainsByOrganization.values().forEach(names -> names.sort(Comparator.naturalOrder()));

        // email-matches first, then by alias
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
                        continue;
                    }

                    if (!isValidDomain(excluded)) {
                        logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: it is not a valid organization domain",
                                excluded, idp.alias());
                        continue;
                    }

                    candidates.add(new Candidate(idp, excluded, matchingDomain, redirect));
                }
            }
        });

        Result result = new Result(routings, addExcludedDomains(organizationNames, domains, candidates, namesCheckedByDatabase));

        Set<IdentityProvider> routed = new HashSet<>();
        result.routings().forEach(routing -> routed.add(routing.identityProvider()));
        result.newDomains().forEach(domain -> routed.add(domain.identityProvider()));
        identityProvidersByOrganization.forEach((organizationId, organizationIdentityProviders) -> organizationIdentityProviders.stream()
                .filter(idp -> !routed.contains(idp))
                .forEach(idp -> logger.warnf("No domain of organization '%s' is routed to identity provider '%s', so the email domain of its members is no longer checked. To check it, route a domain to the identity provider without enabling the redirect",
                        organizationNames.get(organizationId), idp.alias())));

        return result;
    }

    /**
     * The identity provider that 26.7 redirected the emails of {@code domain} to or, if none, the first one without
     * email-matches, whose exclusions had no effect.
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
                                                   List<Candidate> candidates, boolean namesCheckedByDatabase) {
        Collator collator = createCollator();

        // positions in domainList, to avoid scanning all domains for each candidate
        List<Domain> domainList = List.copyOf(domains);
        Map<String, List<Integer>> domainsByName = new HashMap<>();
        Map<String, List<Integer>> domainsByCollationKey = new HashMap<>();
        for (int i = 0; i < domainList.size(); i++) {
            String name = domainList.get(i).name();
            domainsByName.computeIfAbsent(name.toLowerCase(), key -> new ArrayList<>()).add(i);
            domainsByCollationKey.computeIfAbsent(toCollationKey(collator, name), key -> new ArrayList<>()).add(i);
        }

        Map<String, Map<String, Candidate>> candidatesByName = new TreeMap<>();

        for (Candidate candidate : candidates) {
            String organizationId = candidate.organizationId();
            Domain conflict = findConflictCandidates(candidate, domainsByName).stream()
                    .map(domainList::get)
                    .filter(domain -> isInConflict(candidate, domain))
                    .findFirst()
                    .orElse(null);

            if (conflict != null) {
                logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: it overlaps the domain '%s' of organization '%s'. Review the domains of organization '%s'",
                        candidate.name(), candidate.identityProvider().alias(), conflict.name(),
                        organizationNames.get(conflict.organizationId()), organizationNames.get(organizationId));
                continue;
            }

            // names that a case- and accent-insensitive collation may treat as the same
            Domain similar = domainsByCollationKey.getOrDefault(toCollationKey(collator, candidate.name()), List.of()).stream()
                    .map(domainList::get)
                    .filter(domain -> !namesCheckedByDatabase || !domain.organizationId().equals(organizationId))
                    .findFirst()
                    .orElse(null);

            if (similar != null) {
                logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: the database may treat it as the same name as the domain '%s' of organization '%s'. Review the domains of organization '%s'",
                        candidate.name(), candidate.identityProvider().alias(), similar.name(),
                        organizationNames.get(similar.organizationId()), organizationNames.get(organizationId));
                continue;
            }

            candidatesByName.computeIfAbsent(candidate.name().toLowerCase(), key -> new TreeMap<>())
                    .merge(organizationId, candidate, (a, b) -> a.name().compareTo(b.name()) <= 0 ? a : b);
        }

        Map<String, List<Candidate>> candidatesByCollationKey = new LinkedHashMap<>();

        for (Map<String, Candidate> byOrganization : candidatesByName.values()) {
            if (byOrganization.size() > 1) {
                byOrganization.values().forEach(candidate -> logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: another organization excludes a domain with the same name",
                        candidate.name(), candidate.identityProvider().alias()));
                continue;
            }

            Candidate candidate = byOrganization.values().iterator().next();
            candidatesByCollationKey.computeIfAbsent(toCollationKey(collator, candidate.name()), key -> new ArrayList<>()).add(candidate);
        }

        List<Routing> newDomains = new ArrayList<>();

        for (List<Candidate> similar : candidatesByCollationKey.values()) {
            Candidate first = similar.get(0);

            if (similar.stream().anyMatch(candidate -> !candidate.organizationId().equals(first.organizationId()))) {
                similar.forEach(candidate -> logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: another organization excludes a domain that the database may treat as the same name",
                        candidate.name(), candidate.identityProvider().alias()));
                continue;
            }

            for (Candidate candidate : similar) {
                if (candidate != first) {
                    if (!namesCheckedByDatabase) {
                        logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: the database may treat it as the same name as '%s'. Review the domains of organization '%s'",
                                candidate.name(), candidate.identityProvider().alias(), first.name(), organizationNames.get(first.organizationId()));
                        continue;
                    }
                    logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated if the database treats it as the same name as '%s'. Review the domains of organization '%s'",
                            candidate.name(), candidate.identityProvider().alias(), first.name(), organizationNames.get(first.organizationId()));
                }
                newDomains.add(new Routing(candidate.organizationId(), candidate.name(), candidate.redirect()));
            }
        }

        return newDomains;
    }

    /**
     * The domains that {@link #isInConflict} can match: the candidate and its parent domains, exact or wildcard. Any other
     * overlapping domain is more specific than the candidate.
     */
    private static Set<Integer> findConflictCandidates(Candidate candidate, Map<String, List<Integer>> domainsByName) {
        Set<Integer> positions = new TreeSet<>(domainsByName.getOrDefault(candidate.name().toLowerCase(), List.of()));
        String name = baseDomain(candidate.name()).toLowerCase();

        while (true) {
            positions.addAll(domainsByName.getOrDefault(name, List.of()));
            positions.addAll(domainsByName.getOrDefault(WILDCARD_PREFIX + name, List.of()));
            int dot = name.indexOf('.');
            if (dot < 0) {
                return positions;
            }
            name = name.substring(dot + 1);
        }
    }

    /**
     * Whether the domain is the same, or a domain of another organization that is at least as specific as the
     * organization domain of the candidate, so adding the candidate would move email domains between organizations.
     */
    private static boolean isInConflict(Candidate candidate, Domain domain) {
        if (candidate.name().equalsIgnoreCase(domain.name())) {
            return true;
        }

        return !domain.organizationId().equals(candidate.organizationId())
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
     * Same rules as {@code Organizations.validateDomain}, and fits {@code ORG_DOMAIN.NAME} (some databases count bytes).
     */
    static boolean isValidDomain(String domain) {
        if (domain.getBytes(StandardCharsets.UTF_8).length > MAX_NAME_LENGTH) {
            return false;
        }

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
     * Ignores case and accents and expands {@code ß} to {@code ss}, like the default collations of MySQL and MariaDB. It
     * cannot match every collation, which is why the database migration lets the database compare the names.
     */
    private static Collator createCollator() {
        Collator collator = Collator.getInstance(Locale.ROOT);
        collator.setStrength(Collator.PRIMARY);
        collator.setDecomposition(Collator.FULL_DECOMPOSITION);
        return collator;
    }

    private static String toCollationKey(Collator collator, String name) {
        // only the parts between punctuation: the collator ignores '-', the database collations do not
        return LETTERS_AND_DIGITS.matcher(name)
                .replaceAll(part -> HexFormat.of().formatHex(collator.getCollationKey(part.group()).toByteArray()));
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
