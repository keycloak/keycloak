/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.federation.kerberos;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.component.PrioritizedComponentModel;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.models.LDAPConstants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.KerberosEmbeddedServer;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.KerberosRule;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KerberosLdapMultipleLDAPProvidersTest extends AbstractKerberosTest {

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
        ComponentRepresentation rep = getUserStorageConfiguration("kerberos-ldap", LDAPStorageProviderFactory.PROVIDER_NAME);
        // This provider works. It would be executed as 2nd provider (individual tests are supposed to add other provider, which should have lower priority to be invoked first)
        rep.getConfig().putSingle(PrioritizedComponentModel.PRIORITY, "10");
        return rep;
    }

    @Test
    public void test01spnegoWith1stProviderBrokenKerberosConfiguration() throws Exception {
        // Add LDAP, which is invoked first. The Kerberos configuration is broken, so SPNEGO workflow is failing entirely
        ComponentRepresentation rep = getUserStorageConfiguration();
        rep.setName("kerberos-ldap-foo");
        rep.getConfig().putSingle(PrioritizedComponentModel.PRIORITY, "1");
        rep.getConfig().putSingle(KerberosConstants.KERBEROS_REALM, "FOO.ORG");
        rep.getConfig().putSingle(KerberosConstants.SERVER_PRINCIPAL, "HTTP/localhost@FOO.ORG");
        Response resp = testRealmResource().components().add(rep);
        getCleanup().addComponentId(ApiUtil.getCreatedId(resp));
        resp.close();

        AccessTokenResponse tokenResponse = assertSuccessfulSpnegoLogin("hnelson2@KC2.COM", "hnelson2", "secret");
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        Assert.assertEquals(token.getEmail(), "hnelson2@kc2.com");
        UserRepresentation user = assertUser("hnelson2", "hnelson2@kc2.com", "Horatio", "Nelson", "hnelson2@KC2.COM", false);
        assertUserStorageProvider(user, "kerberos-ldap");
    }

    @Test
    public void test02spnegoWith1stProviderBrokenLookupOfKerberosUser() throws Exception {
        // Add LDAP, which is invoked first. The Kerberos configuration is OK, so SPNEGO workflow should be fine.
        // However lookup LDAP based on Kerberos principal is broken, so fallback to next provider would be needed
        ComponentRepresentation rep = getUserStorageConfiguration();
        rep.setName("kerberos-ldap-broken-lookup");
        rep.getConfig().putSingle(PrioritizedComponentModel.PRIORITY, "1");
        rep.getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(mail=nonexistent@email.org)");
        Response resp = testRealmResource().components().add(rep);
        getCleanup().addComponentId(ApiUtil.getCreatedId(resp));
        resp.close();

        AccessTokenResponse tokenResponse = assertSuccessfulSpnegoLogin("hnelson2@KC2.COM", "hnelson2", "secret");
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        Assert.assertEquals(token.getEmail(), "hnelson2@kc2.com");
        UserRepresentation user = assertUser("hnelson2", "hnelson2@kc2.com", "Horatio", "Nelson", "hnelson2@KC2.COM", false);
        assertUserStorageProvider(user, "kerberos-ldap");
    }
}
