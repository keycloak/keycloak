/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.federation.kerberos;

import java.util.List;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.representations.AccessToken;
import org.keycloak.storage.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.testsuite.KerberosEmbeddedServer;
import org.keycloak.testsuite.util.KerberosRule;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KerberosLdapMultipleKerberosRealmTest extends AbstractKerberosMultipleLdapTest {
    private static final String PROVIDER_CONFIG_KEYCLOAK_GROUPA_LOCATION = "classpath:kerberos/multi-ldap/kerberos-groupA-ldap-connection.properties";
    private static final String PROVIDER_CONFIG_KEYCLOAK_GROUPB_LOCATION = "classpath:kerberos/multi-ldap/kerberos-groupB-ldap-connection.properties";
    private static final String PROVIDER_CONFIG_KC2_LOCATION = "classpath:kerberos/multi-ldap/kerberos-kc2-ldap-connection.properties";

    @ClassRule
    public static KerberosRule kerberosRuleKeycloakGroupA = new KerberosRule(PROVIDER_CONFIG_KEYCLOAK_GROUPA_LOCATION, KerberosEmbeddedServer.DEFAULT_KERBEROS_REALM);

    @ClassRule
    public static KerberosRule kerberosRuleKC2 = new KerberosRule(PROVIDER_CONFIG_KC2_LOCATION, KerberosEmbeddedServer.DEFAULT_KERBEROS_REALM_2);

    @ClassRule
    public static KerberosRule kerberosRuleKeycloakGroupB = new KerberosRule(PROVIDER_CONFIG_KEYCLOAK_GROUPB_LOCATION, KerberosEmbeddedServer.DEFAULT_KERBEROS_REALM);


    private static final List<KerberosRule> PROVIDERS_CONFIGURATION = List.of(kerberosRuleKeycloakGroupA, kerberosRuleKC2, kerberosRuleKeycloakGroupB);

    @Override
    protected List getUserStoragesConfig() {
        return PROVIDERS_CONFIGURATION;
    }

    @Override
    protected KerberosRule getKerberosRule() {
        return kerberosRuleKeycloakGroupA;
    }

    private KerberosRule currentRule = kerberosRuleKeycloakGroupA;

    @Override
    protected CommonKerberosConfig getKerberosConfig() {
        return new LDAPProviderKerberosConfig(getUserStorageConfiguration(currentRule));
    }

    @Test
    public void test01SpnegoLoginUser1A() throws Exception {
        changeKerberosExpectedRealm("groupA", "KEYCLOAK.ORG");
        changeKerberosExpectedRealm("kc2", "KC2.COM");
        changeKerberosExpectedRealm("groupB", "KEYCLOAK.ORG");

        assertSuccessfulSpnegoLogin("user1A@KEYCLOAK.ORG", "user1a", "secret");
        assertUser("user1a", "user1a@keycloak.org", null, "groupA", false);
    }

    @Test
    public void test02SpnegoLoginUser1A() throws Exception {
        changeKerberosExpectedRealm("groupA", "KEYCLOAK.ORG");
        changeKerberosExpectedRealm("kc2", "KC2.COM");
        changeKerberosExpectedRealm("groupB", "FOO");

        assertSuccessfulSpnegoLogin("user1A@KEYCLOAK.ORG", "user1a", "secret");
        assertUser("user1a", "user1a@keycloak.org", null, "groupA", false);
    }

    @Test
    public void test03SpnegoLoginUser1A() throws Exception {
        changeKerberosExpectedRealm("groupA", "KEYCLOAK.ORG");
        changeKerberosExpectedRealm("kc2", "FOO");
        changeKerberosExpectedRealm("groupB", "KEYCLOAK.ORG");

        assertSuccessfulSpnegoLogin("user1A@KEYCLOAK.ORG", "user1a", "secret");
        assertUser("user1a", "user1a@keycloak.org", null, "groupA", false);
    }

    @Test
    public void test04SpnegoLoginUser1A() throws Exception {
        changeKerberosExpectedRealm("groupA", "FOO");
        changeKerberosExpectedRealm("kc2", "KC2.COM");
        changeKerberosExpectedRealm("groupB", "KEYCLOAK.ORG");

        assertFailedSpnegoLogin("user1A@KEYCLOAK.ORG", "secret");
    }

    @Test
    public void test05SpnegoLoginUser1A() throws Exception {
        changeKerberosExpectedRealm("groupA", "KEYCLOAK.ORG");
        changeKerberosExpectedRealm("kc2", "FOO");
        changeKerberosExpectedRealm("groupB", "BAR");

        assertSuccessfulSpnegoLogin("user1A@KEYCLOAK.ORG", "user1a", "secret");
        assertUser("user1a", "user1a@keycloak.org", null, "groupA", false);
    }

    @Test
    public void test06SpnegoLoginUser1A() throws Exception {
        changeKerberosExpectedRealm("groupA", "FOO");
        changeKerberosExpectedRealm("kc2", "KC2.COM");
        changeKerberosExpectedRealm("groupB", "BAR");

        assertFailedSpnegoLogin("user1A@KEYCLOAK.ORG", "secret");
    }

    @Test
    public void test07SpnegoLoginUser1A() throws Exception {
        changeKerberosExpectedRealm("groupA", "FOO");
        changeKerberosExpectedRealm("kc2", "BAR");
        changeKerberosExpectedRealm("groupB", "KEYCLOAK.ORG");

        assertFailedSpnegoLogin("user1A@KEYCLOAK.ORG", "secret");
    }

    @Test
    public void test08SpnegoLoginUser1A() throws Exception {
        changeKerberosExpectedRealm("groupA", "FOO");
        changeKerberosExpectedRealm("kc2", "BAR");
        changeKerberosExpectedRealm("groupB", "FOO");

        assertFailedSpnegoLogin("user1A@KEYCLOAK.ORG", "secret");
    }

    @Test
    public void test09SpnegoLoginUser1B() throws Exception {
        changeKerberosExpectedRealm("groupA", "KEYCLOAK.ORG");
        changeKerberosExpectedRealm("kc2", "KC2.COM");
        changeKerberosExpectedRealm("groupB", "KEYCLOAK.ORG");
        assertSuccessfulSpnegoLogin("user1B@KEYCLOAK.ORG", "user1b", "secret");
        assertUser("user1b", "user1b@keycloak.org", null, "groupB", false);
    }

    @Test
    public void test10SpnegoLoginUser1B() throws Exception {
        changeKerberosExpectedRealm("groupA", "KEYCLOAK.ORG");
        changeKerberosExpectedRealm("kc2", "KC2.COM");
        changeKerberosExpectedRealm("groupB", "FOO");

        assertFailedSpnegoLogin("user1B@KEYCLOAK.ORG", "secret");
    }

    @Test
    public void test11SpnegoLoginUser1B() throws Exception {
        changeKerberosExpectedRealm("groupA", "KEYCLOAK.ORG");
        changeKerberosExpectedRealm("kc2", "FOO");
        changeKerberosExpectedRealm("groupB", "KEYCLOAK.ORG");

        assertSuccessfulSpnegoLogin("user1B@KEYCLOAK.ORG", "user1b", "secret");
        assertUser("user1b", "user1b@keycloak.org", null, "groupB", false);
    }

    @Test
    public void test12SpnegoLoginUser1B() throws Exception {
        changeKerberosExpectedRealm("groupA", "FOO");
        changeKerberosExpectedRealm("kc2", "KC2.COM");
        changeKerberosExpectedRealm("groupB", "KEYCLOAK.ORG");

        assertSuccessfulSpnegoLogin("user1B@KEYCLOAK.ORG", "user1b", "secret");
        assertUser("user1b", "user1b@keycloak.org", null, "groupB", false);
    }

    @Test
    public void test13SpnegoLoginUser1B() throws Exception {
        changeKerberosExpectedRealm("groupA", "KEYCLOAK.ORG");
        changeKerberosExpectedRealm("kc2", "FOO");
        changeKerberosExpectedRealm("groupB", "BAR");

        assertFailedSpnegoLogin("user1B@KEYCLOAK.ORG", "secret");
    }

    @Test
    public void test14SpnegoLoginUser1B() throws Exception {
        changeKerberosExpectedRealm("groupA", "FOO");
        changeKerberosExpectedRealm("kc2", "KC2.COM");
        changeKerberosExpectedRealm("groupB", "BAR");

        assertFailedSpnegoLogin("user1B@KEYCLOAK.ORG", "secret");
    }

    @Test
    public void test15SpnegoLoginUser1B() throws Exception {
        changeKerberosExpectedRealm("groupA", "FOO");
        changeKerberosExpectedRealm("kc2", "BAR");
        changeKerberosExpectedRealm("groupB", "KEYCLOAK.ORG");

        assertSuccessfulSpnegoLogin("user1B@KEYCLOAK.ORG", "user1b", "secret");
        assertUser("user1b", "user1b@keycloak.org", null, "groupB", false);
    }

    @Test
    public void test16SpnegoLoginUser1B() throws Exception {
        changeKerberosExpectedRealm("groupA", "FOO");
        changeKerberosExpectedRealm("kc2", "BAR");
        changeKerberosExpectedRealm("groupB", "FOO");

        assertFailedSpnegoLogin("user1B@KEYCLOAK.ORG", "secret");
    }

    @Test
    public void test17SpnegoLoginUser1C() throws Exception {
        changeKerberosExpectedRealm("groupA", "KEYCLOAK.ORG");
        changeKerberosExpectedRealm("kc2", "KC2.COM");
        changeKerberosExpectedRealm("groupB", "KEYCLOAK.ORG");

        assertSuccessfulSpnegoLogin("user1C@KC2.COM", "user1c", "secret");
        assertUser("user1c", "user1c@kc2.com", null, "groupC", false);
    }

    @Test
    public void test18SpnegoLoginUser1C() throws Exception {
        changeKerberosExpectedRealm("groupA", "KEYCLOAK.ORG");
        changeKerberosExpectedRealm("kc2", "KC2.COM");
        changeKerberosExpectedRealm("groupB", "FOO");

        assertSuccessfulSpnegoLogin("user1C@KC2.COM", "user1c", "secret");
        assertUser("user1c", "user1c@kc2.com", null, "groupC", false);
    }

    @Test
    public void test19SpnegoLoginUser1C() throws Exception {
        changeKerberosExpectedRealm("groupA", "KEYCLOAK.ORG");
        changeKerberosExpectedRealm("kc2", "FOO");
        changeKerberosExpectedRealm("groupB", "KEYCLOAK.ORG");

        assertFailedSpnegoLogin("user1C@KC2.COM", "secret");
    }

    @Test
    public void test20SpnegoLoginUser1C() throws Exception {
        changeKerberosExpectedRealm("groupA", "FOO");
        changeKerberosExpectedRealm("kc2", "KC2.COM");
        changeKerberosExpectedRealm("groupB", "KEYCLOAK.ORG");

        assertSuccessfulSpnegoLogin("user1C@KC2.COM", "user1c", "secret");
        assertUser("user1c", "user1c@kc2.com", null, "groupC", false);
    }

    @Test
    public void test21SpnegoLoginUser1C() throws Exception {
        changeKerberosExpectedRealm("groupA", "KEYCLOAK.ORG");
        changeKerberosExpectedRealm("kc2", "FOO");
        changeKerberosExpectedRealm("groupB", "BAR");

        assertFailedSpnegoLogin("user1C@KC2.COM", "secret");
    }

    @Test
    public void test22SpnegoLoginUser1C() throws Exception {
        changeKerberosExpectedRealm("groupA", "FOO");
        changeKerberosExpectedRealm("kc2", "KC2.COM");
        changeKerberosExpectedRealm("groupB", "BAR");

        assertSuccessfulSpnegoLogin("user1C@KC2.COM", "user1c", "secret");
        assertUser("user1c", "user1c@kc2.com", null, "groupC", false);
    }

    @Test
    public void test23SpnegoLoginUser1C() throws Exception {
        changeKerberosExpectedRealm("groupA", "FOO");
        changeKerberosExpectedRealm("kc2", "BAR");
        changeKerberosExpectedRealm("groupB", "KEYCLOAK.ORG");

        assertFailedSpnegoLogin("user1C@KC2.COM", "secret");
    }

    @Test
    public void test24SpnegoLoginUser1C() throws Exception {
        changeKerberosExpectedRealm("groupA", "FOO");
        changeKerberosExpectedRealm("kc2", "BAR");
        changeKerberosExpectedRealm("groupB", "FOO");

        assertFailedSpnegoLogin("user1C@KC2.COM", "secret");
    }
}
