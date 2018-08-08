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

import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.federation.ldap.LDAPTestContext;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.testsuite.util.KerberosRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.util.ldap.KerberosEmbeddedServer;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

import static org.keycloak.testsuite.arquillian.DeploymentTargetModifier.AUTH_SERVER_CURRENT;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KerberosLdapCrossRealmTrustTest extends AbstractKerberosTest {

    @Deployment
    @TargetsContainer(AUTH_SERVER_CURRENT)
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(UserResource.class, LDAPEmbeddedServer.class, KerberosEmbeddedServer.class)
                .addPackages(true,
                        "org.keycloak.testsuite",
                        "org.keycloak.testsuite.federation.ldap",
                        "org.keycloak.testsuite.federation.kerberos");
    }


    private static final String PROVIDER_CONFIG_LOCATION = "classpath:kerberos/kerberos-ldap-crt-connection.properties";

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
        return new LDAPProviderKerberosConfig(getUserStorageConfiguration());
    }

    @Override
    protected ComponentRepresentation getUserStorageConfiguration() {
        return getUserStorageConfiguration("kerberos-ldap", LDAPStorageProviderFactory.PROVIDER_NAME);
    }


    @Test
    public void test01SpnegoLoginCRTSuccess() throws Exception {
        // Login as user from realm KC2.COM . Realm KEYCLOAK.ORG will trust us
        AccessToken token = assertSuccessfulSpnegoLogin("hnelson2@KC2.COM", "hnelson2", "secret");

        Assert.assertEquals(token.getEmail(), "hnelson2@kc2.com");
        assertUser("hnelson2", "hnelson2@kc2.com", "Horatio", "Nelson", false);
    }


    @Test
    public void test02DisableTrust() throws Exception {
        // Remove the LDAP entry corresponding to the Kerberos principal krbtgt/KEYCLOAK.ORG@KC2.COM
        // This will effectively disable kerberos cross-realm trust
        testingClient.testing().ldap("test").removeLDAPUser("krbtgt2");


        // There is no trust among kerberos realms anymore. SPNEGO shouldn't work. There would be failure even on Apache HTTP client side
        // as it's not possible to start GSS context ( initSecContext ) due the missing trust among realms.
        try {
            Response spnegoResponse = spnegoLogin("hnelson2@KC2.COM", "secret");
            Assert.fail("Not expected to successfully login");
        } catch (Exception e) {
            // Expected
        }
    }


}
