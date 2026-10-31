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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    /**
     * Finds the domains of the realm whose name the database treats as the same as the given name.
     */
    @FunctionalInterface
    public interface DomainLookup {
        List<Domain> findByName(String name);
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
     * @param databaseLookup finds the existing domains by name in the database, and the caller only inserts a new domain
     * when the database finds no domain with the same name. If {@code null}, names that a case- and accent-insensitive
     * collation may treat as the same are not added
     */
    public static Result migrate(Map<String, String> organizationNames, Collection<Domain> domains,
                                 Collection<IdentityProvider> identityProviders, DomainLookup databaseLookup) {
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

        Map<IdentityProvider, Set<String>> exclusions = new HashMap<>();
        identityProvidersByOrganization.values().forEach(idps -> idps.forEach(idp -> exclusions.put(idp,
                parseDomains(idp.excludedDomains()).stream().map(String::toLowerCase).collect(Collectors.toSet()))));

        List<Routing> routings = new ArrayList<>();
        List<Candidate> candidates = new ArrayList<>();

        identityProvidersByOrganization.forEach((organizationId, organizationIdentityProviders) -> {
            String organizationName = organizationNames.get(organizationId);
            List<String> organizationDomains = domainsByOrganization.getOrDefault(organizationId, List.of());
            Map<String, IdentityProvider> owners = new HashMap<>();

            for (String name : organizationDomains) {
                IdentityProvider owner = findIdentityProvider(organizationIdentityProviders, exclusions, name, name, organizationName);
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
                    if (organizationDomains.stream().anyMatch(name -> name.toLowerCase().equals(excluded.toLowerCase())) || !seen.add(excluded)) {
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

                    IdentityProvider redirect = findIdentityProvider(organizationIdentityProviders, exclusions, excluded, matchingDomain, organizationName);

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

        Set<Routing> mayBeSkipped = new HashSet<>();
        Result result = new Result(routings, addExcludedDomains(organizationNames, domains, candidates, databaseLookup, mayBeSkipped));

        // a new domain that the database may skip does not count, so that the warning is not lost
        Set<IdentityProvider> routed = new HashSet<>();
        result.routings().forEach(routing -> routed.add(routing.identityProvider()));
        result.newDomains().stream().filter(domain -> !mayBeSkipped.contains(domain))
                .forEach(domain -> routed.add(domain.identityProvider()));
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
    private static IdentityProvider findIdentityProvider(List<IdentityProvider> identityProviders,
                                                         Map<IdentityProvider, Set<String>> exclusions, String domain,
                                                         String matchingDomain, String organizationName) {
        List<IdentityProvider> found = identityProviders.stream()
                .filter(idp -> ANY_DOMAIN.equals(idp.domain()) || idp.domain().equals(matchingDomain))
                .filter(idp -> !idp.autoRedirect() || !isExcluded(exclusions.get(idp), domain))
                .toList();

        List<String> redirecting = found.stream().filter(IdentityProvider::autoRedirect).map(IdentityProvider::alias).toList();

        if (redirecting.size() > 1) {
            logger.infof("Domain '%s' of organization '%s' is routed to identity provider '%s'. In 26.7 it could be redirected to any of %s, in no defined order",
                    domain, organizationName, redirecting.get(0), redirecting);
        }

        return found.isEmpty() ? null : found.get(0);
    }

    private static List<Routing> addExcludedDomains(Map<String, String> organizationNames, Collection<Domain> domains,
                                                   List<Candidate> candidates, DomainLookup databaseLookup,
                                                   Set<Routing> mayBeSkipped) {
        Collator collator = createCollator();

        // domain names are case-insensitive, also where the database compares them case-sensitively
        Map<String, List<Domain>> domainsByName = new HashMap<>();
        Map<String, List<Domain>> domainsByCollationKey = new HashMap<>();
        for (Domain domain : domains) {
            domainsByName.computeIfAbsent(domain.name().toLowerCase(), key -> new ArrayList<>()).add(domain);
            getCollationKeys(collator, domain.name()).forEach(key -> domainsByCollationKey.computeIfAbsent(key, k -> new ArrayList<>()).add(domain));
        }
        DomainLookup lookup = databaseLookup != null ? databaseLookup
                : name -> getCollationKeys(collator, name).stream()
                        .flatMap(key -> domainsByCollationKey.getOrDefault(key, List.of()).stream()).distinct().toList();
        Map<String, List<Domain>> found = new HashMap<>();

        Map<String, Map<String, Candidate>> candidatesByName = new TreeMap<>();

        for (Candidate candidate : candidates) {
            String organizationId = candidate.organizationId();
            boolean wildcard = isWildcard(candidate.name());

            if (!wildcard && !isAscii(candidate.name())) {
                // the database finds an email domain by its exact name with the collation of the column, so other
                // spellings of the name, which may belong to other organizations, would be found by it
                logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: the database may treat other spellings of a name with non-ASCII characters as the same name. Review the domains of organization '%s'",
                        candidate.name(), candidate.identityProvider().alias(), organizationNames.get(organizationId));
                continue;
            }

            Domain conflict = null;

            for (String name : getOverlappingNames(candidate.name())) {
                Set<Domain> matching = new LinkedHashSet<>(domainsByName.getOrDefault(name.toLowerCase(), List.of()));
                if (!wildcard || name.equals(candidate.name())) {
                    // a wildcard domain is never found by its exact name, so only its own name is looked up
                    matching.addAll(found.computeIfAbsent(name, lookup::findByName));
                }
                conflict = matching.stream().filter(domain -> isInConflict(candidate, name, domain)).findFirst().orElse(null);
                if (conflict != null) {
                    break;
                }
            }

            if (conflict != null) {
                logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: it overlaps the domain '%s' of organization '%s'. Review the domains of organization '%s'",
                        candidate.name(), candidate.identityProvider().alias(), conflict.name(),
                        organizationNames.get(conflict.organizationId()), organizationNames.get(organizationId));
                continue;
            }

            candidatesByName.computeIfAbsent(candidate.name().toLowerCase(), key -> new TreeMap<>())
                    .merge(organizationId, candidate, (a, b) -> a.name().compareTo(b.name()) <= 0 ? a : b);
        }

        // candidates that share one of their collation keys may have the same name for the database
        List<List<Candidate>> groups = new ArrayList<>();
        Map<String, List<Candidate>> groupsByCollationKey = new HashMap<>();

        for (Map<String, Candidate> byOrganization : candidatesByName.values()) {
            if (byOrganization.size() > 1) {
                byOrganization.values().forEach(candidate -> logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: another organization excludes a domain with the same name",
                        candidate.name(), candidate.identityProvider().alias()));
                continue;
            }

            Candidate candidate = byOrganization.values().iterator().next();
            Set<String> keys = getCollationKeys(collator, candidate.name());
            List<Candidate> group = null;

            for (String key : keys) {
                List<Candidate> other = groupsByCollationKey.get(key);
                if (other == null || other == group) {
                    continue;
                }
                if (group == null) {
                    group = other;
                    continue;
                }
                // the candidate joins two groups
                group.addAll(other);
                List<Candidate> merged = group;
                groupsByCollationKey.replaceAll((k, g) -> g == other ? merged : g);
                groups.removeIf(g -> g == other);
            }

            if (group == null) {
                group = new ArrayList<>();
                groups.add(group);
            }
            group.add(candidate);
            List<Candidate> joined = group;
            keys.forEach(key -> groupsByCollationKey.put(key, joined));
        }

        List<Routing> newDomains = new ArrayList<>();

        for (List<Candidate> similar : groups) {
            Candidate first = similar.get(0);

            if (databaseLookup == null && similar.stream().anyMatch(candidate -> !candidate.organizationId().equals(first.organizationId()))) {
                similar.forEach(candidate -> logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: another organization excludes a domain that the database may treat as the same name",
                        candidate.name(), candidate.identityProvider().alias()));
                continue;
            }

            for (Candidate candidate : similar) {
                Routing domain = new Routing(candidate.organizationId(), candidate.name(), candidate.redirect());
                if (candidate != first) {
                    String organizationName = organizationNames.get(candidate.organizationId());
                    if (databaseLookup == null) {
                        logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated: the database may treat it as the same name as '%s'. Review the domains of organization '%s'",
                                candidate.name(), candidate.identityProvider().alias(), first.name(), organizationName);
                        continue;
                    }
                    // the caller inserts the names in this order, and skips a name that the database treats as an
                    // earlier one
                    logger.warnf("Excluded domain '%s' of identity provider '%s' is not migrated if the database treats it as the same name as '%s'. Review the domains of organization '%s'",
                            candidate.name(), candidate.identityProvider().alias(), first.name(), organizationName);
                    mayBeSkipped.add(domain);
                }
                newDomains.add(domain);
            }
        }

        return newDomains;
    }

    /**
     * The names of the domains that {@link #isInConflict} can match: the domain itself and the wildcard domains of its
     * base domain and parent domains. Any other overlapping domain is more specific than the domain.
     */
    private static Set<String> getOverlappingNames(String domain) {
        Set<String> names = new LinkedHashSet<>();
        names.add(domain);
        String name = baseDomain(domain);

        while (true) {
            names.add(WILDCARD_PREFIX + name);
            int dot = name.indexOf('.');
            if (dot < 0) {
                return names;
            }
            name = name.substring(dot + 1);
        }
    }

    /**
     * Whether adding the candidate would move email domains of the domain, which the database finds by {@code name},
     * between organizations: the domain has the name of the candidate, or it is a domain of another organization that
     * is at least as specific as the organization domain of the candidate, or whose name only the database treats as
     * {@code name}. In the last case the emails of that spelling of the candidate's name go to the other organization,
     * and the database would find them by the name of the candidate.
     */
    private static boolean isInConflict(Candidate candidate, String name, Domain domain) {
        if (name.equals(candidate.name())) {
            return true;
        }

        if (domain.organizationId().equals(candidate.organizationId())) {
            return false;
        }

        return !domain.name().toLowerCase().equals(name.toLowerCase())
                || compareSpecificity(name, candidate.matchingDomain()) >= 0;
    }

    private static boolean isAscii(String name) {
        return name.chars().allMatch(c -> c < 0x80);
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
     * Same as {@link #covers} for each of the excluded domains, which are in lower case.
     */
    private static boolean isExcluded(Set<String> excludedDomains, String domain) {
        String name = domain.toLowerCase();

        if (excludedDomains.contains(name)) {
            return true;
        }

        name = baseDomain(name);

        while (true) {
            if (excludedDomains.contains(WILDCARD_PREFIX + name)) {
                return true;
            }
            int dot = name.indexOf('.');
            if (dot < 0) {
                return false;
            }
            name = name.substring(dot + 1);
        }
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
     * cannot match every collation, which is why the database migration asks the database.
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

    /**
     * The collation key of the name and, if it has {@code ß} or {@code ı}, also of the name with {@code s} and {@code i}
     * instead, which is how {@code utf8mb4_general_ci} may compare them, unlike the collator.
     */
    private static Set<String> getCollationKeys(Collator collator, String name) {
        Set<String> keys = new LinkedHashSet<>();
        keys.add(toCollationKey(collator, name));
        String folded = name.replace("ß", "s").replace("ı", "i");
        if (!folded.equals(name)) {
            keys.add(toCollationKey(collator, folded));
        }
        return keys;
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
