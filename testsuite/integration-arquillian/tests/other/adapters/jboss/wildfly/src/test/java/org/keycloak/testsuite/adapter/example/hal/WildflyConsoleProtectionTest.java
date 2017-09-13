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
package org.keycloak.testsuite.adapter.example.hal;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;

import java.util.List;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppServerWelcomePage;
import org.keycloak.testsuite.util.WaitUtils;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;

/**
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AppServerContainer("app-server-wildfly")
//@AdapterLibsLocationProperty("adapter.libs.wildfly")
public class WildflyConsoleProtectionTest extends AbstractAdapterTest {

    @Page
    protected AppServerWelcomePage appServerWelcomePage;

    @Page
    protected AccountUpdateProfilePage accountUpdateProfilePage;

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/wildfly-integration/wildfly-management-realm.json"));
    }

    @Before
    public void beforeAuthTest() {
        super.beforeAuthTest();

        try {
            OnlineManagementClient clientWorkerNodeClient = ManagementClient.online(OnlineOptions
                    .standalone()
                    .hostAndPort("localhost", 10190)
                    .build());

            // Create a realm for both wildfly console and mgmt interface
            clientWorkerNodeClient.execute("/subsystem=keycloak/realm=jboss-infra:add(auth-server-url=http://localhost:8180/auth,realm-public-key=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB)");

            // Create a secure-deployment in order to protect mgmt interface
            clientWorkerNodeClient.execute("/subsystem=keycloak/secure-deployment=wildfly-management:add(realm=jboss-infra,resource=wildfly-management,principal-attribute=preferred_username,bearer-only=true,ssl-required=EXTERNAL)");

            // Protect HTTP mgmt interface with Keycloak adapter
            clientWorkerNodeClient.execute("/core-service=management/management-interface=http-interface:undefine-attribute(name=security-realm)");
            clientWorkerNodeClient.execute("/subsystem=elytron/http-authentication-factory=keycloak-mgmt-http-authentication:add(security-domain=KeycloakDomain,http-server-mechanism-factory=wildfly-management,mechanism-configurations=[{mechanism-name=KEYCLOAK,mechanism-realm-configurations=[{realm-name=KeycloakOIDCRealm,realm-mapper=keycloak-oidc-realm-mapper}]}])");
            clientWorkerNodeClient.execute("/core-service=management/management-interface=http-interface:write-attribute(name=http-authentication-factory,value=keycloak-mgmt-http-authentication)");
            clientWorkerNodeClient.execute("/core-service=management/management-interface=http-interface:write-attribute(name=http-upgrade, value={enabled=true, sasl-authentication-factory=management-sasl-authentication})");

            // Enable RBAC where roles are obtained from the identity
            clientWorkerNodeClient.execute("/core-service=management/access=authorization:write-attribute(name=provider,value=rbac)");
            clientWorkerNodeClient.execute("/core-service=management/access=authorization:write-attribute(name=use-identity-roles,value=true)");

            // Create a secure-server in order to publish the wildfly console configuration via mgmt interface
            clientWorkerNodeClient.execute("/subsystem=keycloak/secure-server=wildfly-console:add(realm=jboss-infra,resource=wildfly-console,public-client=true)");

            // reload
            clientWorkerNodeClient.execute("reload");
        } catch (Exception cause) {
            throw new RuntimeException("Failed to configure app server", cause);
        }
    }

    @Test
    public void testLogin() throws InterruptedException {
        appServerWelcomePage.navigateToConsole();
        appServerWelcomePage.login("admin", "admin");
        WaitUtils.pause(2000);
        assertTrue(appServerWelcomePage.isCurrent());
    }

    @Test
    public void testUserCanAccessAccountService() throws InterruptedException {
        appServerWelcomePage.navigateToConsole();
        appServerWelcomePage.login("admin", "admin");
        WaitUtils.pause(2000);
        appServerWelcomePage.navigateToAccessControl();
        appServerWelcomePage.navigateManageProfile();
        assertTrue(accountUpdateProfilePage.isCurrent());
    }
}
