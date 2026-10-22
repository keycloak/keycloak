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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.migration.migrators.DomainIdpRoutingMigration.Domain;
import org.keycloak.migration.migrators.DomainIdpRoutingMigration.IdentityProvider;
import org.keycloak.migration.migrators.DomainIdpRoutingMigration.Result;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class DomainIdpRoutingMigrationTest {

    private final Map<String, String> organizationNames = new HashMap<>();
    private final List<Domain> domains = new ArrayList<>();
    private final List<IdentityProvider> identityProviders = new ArrayList<>();

    @Test
    public void testExcludedSubdomainBecomesUnroutedDomain() {
        org("o", "*.o.com");
        idp("i", "o", "ANY", "x.o.com, *.y.o.com", true);

        Result result = migrate();

        assertThat(routings(result), contains("o *.o.com i true"));
        assertThat(newDomains(result), containsInAnyOrder("o x.o.com -", "o *.y.o.com -"));
    }

    @Test
    public void testExcludedDomainCoveringOrganizationDomain() {
        // in 26.7 no email domain of o.com, a.p.com or *.b.p.com was redirected
        org("o", "o.com", "a.p.com", "*.b.p.com", "*.q.com");
        idp("i", "o", "ANY", "*.o.com, a.p.com, *.p.com", true);

        Result result = migrate();

        assertThat(routings(result), contains("o *.q.com i true"));
        assertThat(newDomains(result), empty());
    }

    @Test
    public void testExcludedDomainWithoutEffectIsNotMigrated() {
        org("o", "o.com", "*.p.com");
        idp("i", "o", "ANY", "gmail.com, *.o.com, sub.o.com, *.q.com, a.p.com", true);

        Result result = migrate();

        // only a.p.com is part of an organization domain; the others never matched an email domain of the organization
        assertThat(routings(result), contains("o *.p.com i true"));
        assertThat(newDomains(result), contains("o a.p.com -"));
    }

    @Test
    public void testExcludedDomainsOfIdentityProviderWithoutEmailMatchesHadNoEffect() {
        org("o", "o.com", "*.p.com");
        idp("i", "o", "ANY", "o.com, x.p.com", false);

        Result result = migrate();

        assertThat(routings(result), contains("o *.p.com i false", "o o.com i false"));
        assertThat(newDomains(result), empty());
    }

    @Test
    public void testSameExcludedDomainInTwoOrganizations() {
        org("p", "p.com");
        org("q", "q.com");
        idp("ip", "p", "ANY", "gmail.com", true);
        idp("iq", "q", "ANY", "gmail.com", true);

        Result result = migrate();

        assertThat(routings(result), contains("p p.com ip true", "q q.com iq true"));
        assertThat(newDomains(result), empty());
    }

    @Test
    public void testExcludedDomainOfAnotherOrganization() {
        org("a", "sub.w.com");
        org("w", "*.w.com");
        idp("iw", "w", "ANY", "sub.w.com", true);

        Result result = migrate();

        assertThat(routings(result), contains("w *.w.com iw true"));
        assertThat(newDomains(result), empty());
    }

    @Test
    public void testSameExcludedSubdomainInNestedOrganizationsIsOrderIndependent() {
        // x.a.p.com resolved to q, the organization with the more specific domain, so q's exclusion applied
        for (String[] aliases : new String[][] {{"a", "b"}, {"b", "a"}}) {
            organizationNames.clear();
            domains.clear();
            identityProviders.clear();
            org("p", "*.p.com");
            org("q", "*.a.p.com");
            idp(aliases[0], "p", "ANY", "x.a.p.com", true);
            idp(aliases[1], "q", "ANY", "x.a.p.com", true);

            Result result = migrate();

            assertThat(newDomains(result), contains("q x.a.p.com -"));
        }
    }

    @Test
    public void testExcludedDomainThatWouldChangeTheOrganizationOfAnEmailDomain() {
        // p wins the email domains of *.a.o.com, so o's exclusions inside it are not migrated
        org("o", "*.o.com");
        org("p", "*.a.o.com");
        idp("i", "o", "ANY", "*.a.o.com, y.a.o.com, b.o.com", true);

        Result result = migrate();

        assertThat(newDomains(result), contains("o b.o.com -"));
    }

    @Test
    public void testMoreSpecificDomainOfAnotherOrganizationIsNoConflict() {
        org("o", "*.o.com");
        org("p", "y.x.o.com");
        idp("i", "o", "ANY", "*.x.o.com", true);

        Result result = migrate();

        // y.x.o.com is more specific than *.x.o.com and keeps resolving to p
        assertThat(newDomains(result), contains("o *.x.o.com -"));
    }

    @Test
    public void testSameExcludedDomainInTwoIdentityProvidersOfOneOrganization() {
        org("o", "*.o.com");
        idp("i1", "o", "ANY", "X.o.com", true);
        idp("i2", "o", "ANY", "x.o.com, y.o.com", true);

        Result result = migrate();

        // y.o.com was redirected to i1, the other emails to either; i1 comes first by alias
        assertThat(routings(result), contains("o *.o.com i1 true"));
        assertThat(newDomains(result), containsInAnyOrder("o X.o.com -"));
    }

    @Test
    public void testExcludedSubdomainOfOneIdentityProviderStillRedirectedByAnother() {
        org("o", "*.o.com");
        idp("a", "o", "ANY", null, true);
        idp("b", "o", "ANY", "x.o.com", true);

        Result result = migrate();

        // 26.7 redirected x.o.com to a, like the rest of *.o.com
        assertThat(routings(result), contains("o *.o.com a true"));
        assertThat(newDomains(result), empty());
    }

    @Test
    public void testExcludedSubdomainGoesToTheIdentityProviderThatRedirectedIt() {
        org("o", "*.o.com");
        idp("a", "o", "ANY", "x.o.com", true);
        idp("b", "o", "ANY", null, true);
        idp("c", "o", "ANY", "y.o.com, z.o.com", true);
        idp("n", "o", "ANY", null, false);

        Result result = migrate();

        assertThat(routings(result), contains("o *.o.com a true"));
        assertThat(newDomains(result), contains("o x.o.com b true"));
    }

    @Test
    public void testExcludedSubdomainLinkedToIdentityProviderWithoutEmailMatches() {
        org("o", "*.o.com");
        idp("a", "o", "ANY", "x.o.com", true);
        idp("n", "o", "ANY", "*.o.com", false);

        Result result = migrate();

        // x.o.com was not redirected, but n stays linked to it, as to every domain of the organization
        assertThat(routings(result), contains("o *.o.com a true"));
        assertThat(newDomains(result), contains("o x.o.com n false"));
    }

    @Test
    public void testExcludedDomainOfIdentityProviderWithSpecificDomain() {
        org("o", "*.o.com", "p.com");
        idp("i", "o", "*.o.com", "contractors.o.com, gmail.com", true);
        idp("j", "o", "p.com", "*.p.com", true);
        idp("k", "o", "p.com", null, false);

        Result result = migrate();

        // j excluded all of p.com, so p.com keeps the link to k
        assertThat(routings(result), contains("o *.o.com i true", "o p.com k false"));
        assertThat(newDomains(result), contains("o contractors.o.com -"));
    }

    @Test
    public void testExcludedDomainEqualToAnotherOrganizationDomain() {
        org("o", "o.com", "p.com");
        idp("i", "o", "p.com", "o.com", true);
        idp("j", "o", null, null, true);

        Result result = migrate();

        assertThat(routings(result), contains("o p.com i true"));
        assertThat(newDomains(result), empty());
    }

    @Test
    public void testExcludedDomainIsCaseInsensitive() {
        org("o", "*.O.com", "Sub.O.com");
        idp("i", "o", "ANY", "sub.o.com, Deep.o.com, deep.O.COM", true);

        Result result = migrate();

        assertThat(routings(result), contains("o *.O.com i true"));
        assertThat(newDomains(result), contains("o Deep.o.com -"));
    }

    @Test
    public void testInvalidExcludedDomainIsNotMigrated() {
        org("o", "*.o.com");
        org("p", "*.p.com");
        idp("io", "o", "ANY", "*.x*.o.com, x.*.o.com", true);
        idp("ip", "p", "ANY", "*.com", true);

        Result result = migrate();

        // *.com is not a valid domain, but in 26.7 it excluded every email domain of p
        assertThat(routings(result), contains("o *.o.com io true"));
        assertThat(newDomains(result), empty());
    }

    @Test
    public void testExcludedDomainsThatOnlyDifferInAccents() {
        // a case- and accent-insensitive collation (such as MySQL's utf8mb4_unicode_ci) treats these names as equal
        org("o", "*.cafe.o.com");
        org("p", "*.café.o.com");
        org("q", "*.q.com", "café.q.com");
        idp("io", "o", "ANY", "x.cafe.o.com", true);
        idp("ip", "p", "ANY", "x.café.o.com", true);
        idp("iq", "q", "ANY", "cafe.q.com, a.q.com, Á.q.com", true);

        Result result = migrate();

        assertThat(newDomains(result), contains("q a.q.com -"));
    }

    @Test
    public void testIdentityProviderThatRedirectedWinsTheDomain() {
        org("o", "o.com", "p.com");
        idp("a", "o", "ANY", null, false);
        idp("b", "o", "ANY", null, true);
        idp("c", "o", "p.com", null, true);
        idp("d", "o", "o.com", null, false);

        Result result = migrate();

        // 26.7 redirected to b or c; the migration takes the first by alias with email-matches enabled
        assertThat(routings(result), contains("o o.com b true", "o p.com b true"));
    }

    @Test
    public void testFirstIdentityProviderByAliasWinsTheDomain() {
        org("o", "o.com");
        idp("b", "o", "o.com", null, false);
        idp("a", "o", "ANY", null, false);
        idp("Okta", "o", "ANY", null, true);
        idp("azure", "o", "ANY", null, true);

        Result result = migrate();

        // by alias, ignoring case
        assertThat(routings(result), contains("o o.com azure true"));
    }

    @Test
    public void testDomainValidation() {
        assertThat(DomainIdpRoutingMigration.isValidDomain("o.com"), is(true));
        assertThat(DomainIdpRoutingMigration.isValidDomain("*.o.com"), is(true));
        assertThat(DomainIdpRoutingMigration.isValidDomain("*.com"), is(false));
        assertThat(DomainIdpRoutingMigration.isValidDomain("x.*.com"), is(false));
        assertThat(DomainIdpRoutingMigration.isValidDomain("*.*.o.com"), is(false));
        assertThat(DomainIdpRoutingMigration.isValidDomain("o..com"), is(false));
    }

    private Result migrate() {
        return DomainIdpRoutingMigration.migrate(organizationNames, domains, identityProviders);
    }

    private void org(String id, String... names) {
        organizationNames.put(id, id);
        for (String name : names) {
            domains.add(new Domain(id, name));
        }
    }

    private void idp(String alias, String organizationId, String domain, String excludedDomains, boolean autoRedirect) {
        identityProviders.add(new IdentityProvider("id-" + alias, alias, organizationId, domain, excludedDomains, autoRedirect));
    }

    private static List<String> routings(Result result) {
        return result.routings().stream()
                .map(routing -> routing.organizationId() + " " + routing.name() + " " + routing.identityProvider().alias()
                        + " " + routing.identityProvider().autoRedirect())
                .toList();
    }

    private static List<String> newDomains(Result result) {
        return result.newDomains().stream()
                .map(domain -> domain.organizationId() + " " + domain.name() + " " + (domain.identityProvider() == null ? "-"
                        : domain.identityProvider().alias() + " " + domain.identityProvider().autoRedirect()))
                .toList();
    }
}
