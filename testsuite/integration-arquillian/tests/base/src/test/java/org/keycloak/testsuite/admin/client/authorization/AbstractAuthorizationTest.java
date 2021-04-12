/*
  Copyright 2016 Red Hat, Inc. and/or its affiliates
  and other contributors as indicated by the @author tags.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

 */
package org.keycloak.testsuite.admin.client.authorization;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ResourceScopeResource;
import org.keycloak.admin.client.resource.ResourceScopesResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.client.AbstractClientTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.keycloak.common.Profile.Feature.AUTHORIZATION;
import static org.keycloak.common.Profile.Feature.UPLOAD_SCRIPTS;

import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@EnableFeature(value = UPLOAD_SCRIPTS, skipRestart = true)
public abstract class AbstractAuthorizationTest extends AbstractClientTest {

    protected static final String RESOURCE_SERVER_CLIENT_ID = "resource-server-test";

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(AUTHORIZATION);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm("authz-test");
    }

    @Override
    protected String getRealmId() {
        return "authz-test";
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(createTestRealm().build());
        super.addTestRealms(testRealms);
    }

    @After
    public void onAfterReenableAuthorization() {
        enableAuthorizationServices(false);
        enableAuthorizationServices(true);
    }

    protected ClientResource getClientResource() {
        return findClientResource(RESOURCE_SERVER_CLIENT_ID);
    }

    protected ClientRepresentation getResourceServer() {
        return findClientRepresentation(RESOURCE_SERVER_CLIENT_ID);
    }

    protected void enableAuthorizationServices(boolean enable) {
        ClientRepresentation resourceServer = getResourceServer();

        resourceServer.setAuthorizationServicesEnabled(enable);
        resourceServer.setServiceAccountsEnabled(true);
        resourceServer.setPublicClient(false);
        resourceServer.setSecret("secret");

        getClientResource().update(resourceServer);

        if (enable) {
            AuthorizationResource authorization = getClientResource().authorization();
            ResourceServerRepresentation settings = authorization.exportSettings();
            settings.setAllowRemoteResourceManagement(true);
            authorization.update(settings);
        }
    }

    protected ResourceScopeResource createDefaultScope() {
        return createScope("Test Scope", "Scope Icon");
    }

    protected ResourceScopeResource createScope(String name, String iconUri) {
        ScopeRepresentation newScope = new ScopeRepresentation();

        newScope.setName(name);
        newScope.setIconUri(iconUri);

        ResourceScopesResource resources = getClientResource().authorization().scopes();

        try (Response response = resources.create(newScope)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            ScopeRepresentation stored = response.readEntity(ScopeRepresentation.class);

            return resources.scope(stored.getId());
        }
    }

    private RealmBuilder createTestRealm() {
        return RealmBuilder.create().name("authz-test")
                .user(UserBuilder.create().username("marta").password("password"))
                .user(UserBuilder.create().username("kolo").password("password"))
                .client(ClientBuilder.create().clientId(RESOURCE_SERVER_CLIENT_ID)
                        .name(RESOURCE_SERVER_CLIENT_ID)
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/" + RESOURCE_SERVER_CLIENT_ID)
                        .defaultRoles("uma_protection")
                        .directAccessGrants());
    }
}
