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

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.KerberosConfig;
import org.keycloak.federation.kerberos.KerberosFederationProviderFactory;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.testsuite.util.KerberosRule;
import org.keycloak.util.ldap.KerberosEmbeddedServer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KerberosStandaloneCrossRealmTrustTest extends AbstractKerberosTest {

    private static final String PROVIDER_CONFIG_LOCATION = "classpath:kerberos/kerberos-standalone-connection.properties";


    @ClassRule
    public static KerberosRule kerberosRule = new KerberosRule(PROVIDER_CONFIG_LOCATION, KerberosEmbeddedServer.DEFAULT_KERBEROS_REALM);


    @ClassRule
    public static KerberosRule kerberosRule2 = new KerberosRule(PROVIDER_CONFIG_LOCATION, KerberosEmbeddedServer.DEFAULT_KERBEROS_REALM_2);


    @Override
    protected KerberosRule getKerberosRule() {
        return kerberosRule;
    }


    @Override
    protected CommonKerberosConfig getKerberosConfig() {
        return new KerberosConfig(getUserStorageConfiguration());
    }

    @Override
    protected ComponentRepresentation getUserStorageConfiguration() {
        return getUserStorageConfiguration("kerberos-standalone", KerberosFederationProviderFactory.PROVIDER_NAME);
    }


    @Test
    public void test01spnegoLoginSameRealmTest() throws Exception {
        assertSuccessfulSpnegoLogin("hnelson", "hnelson", "secret");
        assertUser("hnelson", "hnelson@keycloak.org", null, null, false);
    }


    @Test
    public void test02spnegoLoginDifferentRealmTest() throws Exception {
        // Cross-realm trust login. Realm KEYCLOAK.ORG trusts realm KC2.COM.
        // TODO: email hnelson2@keycloak.org is not very good. Will be better to have more flexibility for mapping of kerberos principals to Keycloak UserModel in KerberosFederationProvider (if needed)
        assertSuccessfulSpnegoLogin("hnelson2@KC2.COM", "hnelson2", "secret");
        assertUser("hnelson2", "hnelson2@keycloak.org", null, null, false);
    }


}
