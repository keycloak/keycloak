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
package org.keycloak.connections.jpa.updater.liquibase.custom;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Runs the 26.8.0 domain routing migration against the schema it finds at that point of the changelog: ORG_DOMAIN
 * already has REALM_ID and UK_ORG_DOMAIN_REALM_NAME, and the IdP-organization link and the domain settings are
 * still in IDENTITY_PROVIDER and IDENTITY_PROVIDER_CONFIG.
 */
public class JpaUpdate26_8_0_DomainIdpRoutingMigrationTest {

    private Connection connection;

    @Before
    public void createSchema() throws Exception {
        createSchema(null);
    }

    private void createSchema(String collation) throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE", "sa", "");
        connection.setAutoCommit(false);
        if (collation != null) {
            execute("SET COLLATION " + collation);
        }
        execute("CREATE TABLE REALM (ID VARCHAR(36) PRIMARY KEY)",
                "CREATE TABLE ORG (ID VARCHAR(255) PRIMARY KEY, REALM_ID VARCHAR(255) NOT NULL, NAME VARCHAR(255) NOT NULL)",
                "CREATE TABLE IDENTITY_PROVIDER (INTERNAL_ID VARCHAR(36) PRIMARY KEY, REALM_ID VARCHAR(36), PROVIDER_ALIAS VARCHAR(255), ORGANIZATION_ID VARCHAR(255))",
                "CREATE TABLE IDENTITY_PROVIDER_CONFIG (IDENTITY_PROVIDER_ID VARCHAR(36) NOT NULL, VALUE CLOB, NAME VARCHAR(255) NOT NULL, PRIMARY KEY (IDENTITY_PROVIDER_ID, NAME))",
                "CREATE TABLE ORG_DOMAIN (ID VARCHAR(36) NOT NULL, NAME VARCHAR(255) NOT NULL, VERIFIED BOOLEAN NOT NULL, ORG_ID VARCHAR(255) NOT NULL, REALM_ID VARCHAR(255) NOT NULL,"
                        + " IDP_ID VARCHAR(36), AUTO_REDIRECT BOOLEAN DEFAULT FALSE NOT NULL, PRIMARY KEY (ID, NAME),"
                        + " CONSTRAINT UK_ORG_DOMAIN_REALM_NAME UNIQUE (REALM_ID, NAME))",
                "CREATE TABLE ORG_IDENTITY_PROVIDER (ORGANIZATION_ID VARCHAR(255) NOT NULL, IDENTITY_PROVIDER_ID VARCHAR(36) NOT NULL,"
                        + " AUTO_MEMBERSHIP BOOLEAN DEFAULT TRUE NOT NULL, MEMBERSHIP_TYPE VARCHAR(255) DEFAULT 'UNMANAGED', PRIMARY KEY (ORGANIZATION_ID, IDENTITY_PROVIDER_ID))");
    }

    @After
    public void close() throws Exception {
        connection.close();
    }

    @Test
    public void testExcludedSubdomain() throws Exception {
        realm("r");
        org("r", "o", "*.o.com");
        idp("r", "i1", "o", "ANY", "sub.o.com", true);

        migrate();

        assertDomains("r o *.o.com i1 redirect", "r o sub.o.com -");
    }

    @Test
    public void testExcludedDomainOutsideOrganizationDomains() throws Exception {
        realm("r");
        org("r", "o", "o.com", "*.p.com");
        idp("r", "i1", "o", "ANY", "x.com, *.o.com, *.q.com, p.com, *.a.p.com");

        migrate();

        // *.o.com covers o.com, which stays unrouted; x.com, *.o.com and *.q.com are not part of a routed domain,
        // so in 26.7 they had no effect
        assertDomains("r o o.com -", "r o *.p.com i1 redirect", "r o p.com -", "r o *.a.p.com -");
    }

    @Test
    public void testSameExcludedDomainInTwoIdentityProvidersOfOneOrganization() throws Exception {
        realm("r");
        org("r", "o", "*.o.com");
        idp("r", "i1", "o", "ANY", "x.o.com", false);
        idp("r", "i2", "o", "ANY", "x.o.com, y.o.com", true);

        migrate();

        // 26.7 redirected to i2, the identity provider with email-matches, except x.o.com and y.o.com, which keep the
        // link to i1; the exclusion of i1 had no effect
        assertDomains("r o *.o.com i2 redirect", "r o x.o.com i1", "r o y.o.com i1");
    }

    @Test
    public void testExcludedSubdomainRedirectedByAnotherIdentityProvider() throws Exception {
        realm("r");
        org("r", "o", "*.o.com");
        idp("r", "i1", "o", "ANY", "x.o.com");
        idp("r", "i2", "o", "ANY", null);

        migrate();

        // 26.7 redirected x.o.com to i2, the rest of *.o.com to i1 or i2
        assertDomains("r o *.o.com i1 redirect", "r o x.o.com i2 redirect");
    }

    @Test
    public void testFirstIdentityProviderByAliasWinsTheDomain() throws Exception {
        realm("r");
        org("r", "o", "o.com");
        idp("r", "i2", "o", "ANY", null, true);
        idp("r", "i3", "o", "o.com", null, true);
        idp("r", "i1", "o", "ANY", null, false);

        migrate();

        assertDomains("r o o.com i2 redirect");
    }

    @Test
    public void testExcludedDomainOfAnotherOrganization() throws Exception {
        realm("r");
        org("r", "a", "a.com", "sub.w.com");
        org("r", "w", "*.w.com");
        idp("r", "iw", "w", "ANY", "a.com, sub.w.com");

        migrate();

        assertDomains("r a a.com -", "r a sub.w.com -", "r w *.w.com iw redirect");
    }

    @Test
    public void testSameExcludedDomainInTwoOrganizations() throws Exception {
        realm("r");
        org("r", "p", "p.com");
        org("r", "q", "q.com");
        idp("r", "ip", "p", "ANY", "gmail.com");
        idp("r", "iq", "q", "ANY", "gmail.com");

        migrate();

        assertDomains("r p p.com ip redirect", "r q q.com iq redirect");
    }

    @Test
    public void testSameExcludedSubdomainInTwoOrganizations() throws Exception {
        realm("r");
        org("r", "p", "*.p.com");
        org("r", "q", "*.a.p.com");
        idp("r", "ip", "p", "ANY", "x.a.p.com");
        idp("r", "iq", "q", "ANY", "x.a.p.com");

        migrate();

        // x.a.p.com resolved to q, which has the more specific domain, so only q's exclusion applied
        assertDomains("r p *.p.com ip redirect", "r q *.a.p.com iq redirect", "r q x.a.p.com -");
    }

    @Test
    public void testSameExcludedSubdomainInTwoOrganizationsInReverseOrder() throws Exception {
        realm("r");
        org("r", "p", "*.p.com");
        org("r", "q", "*.a.p.com");
        idp("r", "iq", "p", "ANY", "x.a.p.com");
        idp("r", "ip", "q", "ANY", "x.a.p.com");

        migrate();

        assertDomains("r p *.p.com iq redirect", "r q *.a.p.com ip redirect", "r q x.a.p.com -");
    }

    @Test
    public void testExcludedDomainsWithAccentInsensitiveCollation() throws Exception {
        // like MySQL's and MariaDB's utf8mb4 collations, compares names without case and accents
        connection.close();
        createSchema("ENGLISH STRENGTH PRIMARY");
        realm("r");
        org("r", "o", "*.o.com");
        org("r", "p", "*.café.o.com");
        org("r", "q", "*.q.com", "café.q.com");
        idp("r", "io", "o", "ANY", "x.cafe.o.com");
        idp("r", "ip", "p", "ANY", "x.café.o.com");
        idp("r", "iq", "q", "ANY", "cafe.q.com, a.q.com");

        migrate();

        assertDomains("r o *.o.com io redirect", "r p *.café.o.com ip redirect", "r q *.q.com iq redirect", "r q café.q.com iq redirect", "r q a.q.com -");
    }

    @Test
    public void testExcludedDomainLongerThanDomainName() throws Exception {
        realm("r");
        org("r", "o", "*.o.com");
        // a valid domain, but with the wildcard longer than ORG_DOMAIN.NAME
        String name = "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(56) + ".o.com";
        idp("r", "i1", "o", "ANY", "*." + name + ", x.o.com");

        migrate();

        assertDomains("r o *.o.com i1 redirect", "r o x.o.com -");
    }

    @Test
    public void testExcludedDomainsWithExpansionsWithAccentInsensitiveCollation() throws Exception {
        connection.close();
        createSchema("ENGLISH STRENGTH PRIMARY");
        realm("r");
        org("r", "o", "*.o.com", "Æ.o.com");
        idp("r", "io", "o", "ANY", "straße.o.com, strasse.o.com, ae.o.com, á.o.com, a.o.com");

        migrate();

        // the collation treats straße.o.com as strasse.o.com, ae.o.com as Æ.o.com and á.o.com as a.o.com, so only the
        // first of them is added
        assertDomains("r o *.o.com io redirect", "r o Æ.o.com io redirect", "r o a.o.com -", "r o strasse.o.com -");
    }

    @Test
    public void testExcludedDomainsThatOnlyDifferInAccents() throws Exception {
        realm("r");
        org("r", "o", "*.o.com");
        idp("r", "io", "o", "ANY", "straße.o.com, strasse.o.com, á.o.com, a.o.com");

        migrate();

        // the default collation of H2, like the one of PostgreSQL, keeps all names
        assertDomains("r o *.o.com io redirect", "r o a.o.com -", "r o strasse.o.com -", "r o straße.o.com -", "r o á.o.com -");
    }

    @Test
    public void testSameExcludedDomainInTwoRealms() throws Exception {
        realm("r1");
        realm("r2");
        org("r1", "o1", "*.o.com");
        org("r2", "o2", "*.o.com");
        idp("r1", "i1", "o1", "ANY", "x.o.com");
        idp("r2", "i2", "o2", "ANY", "x.o.com");

        migrate();

        assertDomains("r1 o1 *.o.com i1 redirect", "r1 o1 x.o.com -", "r2 o2 *.o.com i2 redirect", "r2 o2 x.o.com -");
    }

    @Test
    public void testExcludedDomainIsCaseInsensitive() throws Exception {
        realm("r");
        org("r", "o", "*.O.com", "Sub.O.com");
        idp("r", "i1", "o", "ANY", "sub.o.com, Deep.o.com, deep.O.COM");

        migrate();

        // the exclusion list was matched case-insensitively, so Sub.O.com stays unrouted
        assertDomains("r o *.O.com i1 redirect", "r o Sub.O.com -", "r o Deep.o.com -");
    }

    @Test
    public void testSpecificDomain() throws Exception {
        realm("r");
        org("r", "o", "o.com", "p.com");
        idp("r", "i1", "o", "p.com", null, true);

        migrate();

        assertDomains("r o o.com -", "r o p.com i1 redirect");
    }

    private void migrate() throws Exception {
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        SqlStatement[] statements = new JpaUpdate26_8_0_DomainIdpRoutingMigration().generateStatements(database);

        // Liquibase executes the statements after the task has generated all of them
        for (SqlStatement statement : statements) {
            for (Sql sql : SqlGeneratorFactory.getInstance().generateSql(statement, database)) {
                execute(sql.toSql());
            }
        }
        connection.commit();
    }

    /**
     * Expects the ORG_DOMAIN rows as "realm org domain idp", where idp is "-" when the domain is not routed, and is
     * followed by "redirect" when AUTO_REDIRECT is set.
     */
    private void assertDomains(String... expected) throws Exception {
        List<String> actual = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT d.REALM_ID, d.ORG_ID, d.NAME, i.PROVIDER_ALIAS, d.AUTO_REDIRECT FROM ORG_DOMAIN d"
                     + " LEFT JOIN IDENTITY_PROVIDER i ON i.INTERNAL_ID = d.IDP_ID")) {
            while (rs.next()) {
                actual.add(rs.getString(1) + " " + rs.getString(2) + " " + rs.getString(3) + " " + (rs.getString(4) == null ? "-" : rs.getString(4))
                        + (rs.getBoolean(5) ? " redirect" : ""));
            }
        }
        assertThat(actual, containsInAnyOrder(expected));
    }

    private void realm(String id) throws Exception {
        execute("INSERT INTO REALM (ID) VALUES ('" + id + "')");
    }

    private void org(String realmId, String id, String... domains) throws Exception {
        execute("INSERT INTO ORG (ID, REALM_ID, NAME) VALUES ('" + id + "', '" + realmId + "', '" + id + "')");
        for (String domain : domains) {
            execute("INSERT INTO ORG_DOMAIN (ID, NAME, VERIFIED, ORG_ID, REALM_ID) VALUES ('" + UUID.randomUUID() + "', '" + domain + "', TRUE, '" + id + "', '" + realmId + "')");
        }
    }

    /**
     * An identity provider with email-matches enabled: in 26.7 excluded domains only had an effect with it.
     */
    private void idp(String realmId, String alias, String orgId, String domain, String excludedDomains) throws Exception {
        idp(realmId, alias, orgId, domain, excludedDomains, true);
    }

    private void idp(String realmId, String alias, String orgId, String domain, String excludedDomains, boolean emailMatches) throws Exception {
        // the alias doubles as internal ID to keep the assertions readable
        execute("INSERT INTO IDENTITY_PROVIDER (INTERNAL_ID, REALM_ID, PROVIDER_ALIAS, ORGANIZATION_ID) VALUES ('" + alias + "', '" + realmId + "', '" + alias + "', '" + orgId + "')",
                "INSERT INTO IDENTITY_PROVIDER_CONFIG (IDENTITY_PROVIDER_ID, NAME, VALUE) VALUES ('" + alias + "', 'kc.org.domain', '" + domain + "')");
        if (excludedDomains != null) {
            execute("INSERT INTO IDENTITY_PROVIDER_CONFIG (IDENTITY_PROVIDER_ID, NAME, VALUE) VALUES ('" + alias + "', 'kc.org.excluded.domains', '" + excludedDomains + "')");
        }
        if (emailMatches) {
            execute("INSERT INTO IDENTITY_PROVIDER_CONFIG (IDENTITY_PROVIDER_ID, NAME, VALUE) VALUES ('" + alias + "', 'kc.org.broker.redirect.mode.email-matches', 'true')");
        }
    }

    private void execute(String... sqls) throws Exception {
        try (Statement st = connection.createStatement()) {
            for (String sql : sqls) {
                st.execute(sql);
            }
        }
    }
}
