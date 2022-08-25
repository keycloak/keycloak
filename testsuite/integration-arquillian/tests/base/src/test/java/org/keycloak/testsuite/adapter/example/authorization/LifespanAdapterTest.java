/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.adapter.example.authorization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.adapter.page.PhotozClientAuthzTestApp;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.util.javascript.ResponseValidator;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
public class LifespanAdapterTest extends AbstractPhotozExampleAdapterTest {

    @Deployment(name = PhotozClientAuthzTestApp.DEPLOYMENT_NAME)
    public static WebArchive deploymentClient() throws IOException {
        return exampleDeployment(PhotozClientAuthzTestApp.DEPLOYMENT_NAME);
    }

    @Deployment(name = RESOURCE_SERVER_ID, managed = false, testable = false)
    public static WebArchive deploymentResourceServer() throws IOException {
        return exampleDeployment(RESOURCE_SERVER_ID,
              webArchive -> webArchive.addAsWebInfResource(new File(TEST_APPS_HOME_DIR + "/photoz/keycloak-cache-lifespan-authz-service.json"), "keycloak.json"));
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadRealm(new File(TEST_APPS_HOME_DIR + "/photoz/photoz-realm.json"));
        realm.setAccessTokenLifespan(70); // must increase lifespan of access token in order to use bigger offset in test cases
        testRealms.add(realm);
    }

    @Test
    public void testPathConfigInvalidation() throws Exception {
        loginToClientPage(aliceUser);
        assertSuccess();

        ResourceRepresentation resource = getAuthorizationResource().resources().findByName("Profile Resource").get(0);
        AuthorizationResource authorizationResource = getAuthorizationResource();

        authorizationResource.resources().resource(resource.getId()).remove();
        assertThat(getAuthorizationResource().resources().findByName("Profile Resource").isEmpty(), Matchers.is(true));

        loginToClientPage(aliceUser);

        // should throw an error because the resource was removed and cache entry did not expire yet
        assertFailure();

        setTimeOffsetOfAdapter(40);

        loginToClientPage(aliceUser);
        assertSuccess();
        setTimeOffsetOfAdapter(0);

        try (Response response = authorizationResource.resources().create(resource)) {
            resource = response.readEntity(ResourceRepresentation.class);
        }

        loginToClientPage(aliceUser);
        assertSuccess();

        RealmResource realm = this.realmsResouce().realm(REALM_NAME);
        UserRepresentation userRepresentation = realm.users().search(aliceUser.getUsername()).get(0);
        UserResource userResource = realm.users().get(userRepresentation.getId());
        
        userRepresentation.setEmail("alice@anotherdomain.org");
        
        userResource.update(userRepresentation);
        loginToClientPage(aliceUser);
        assertTicket();

        try {
            PolicyRepresentation resourceInstancePermission = new PolicyRepresentation();

            resourceInstancePermission.setName("View User Permission");
            resourceInstancePermission.setType("resource");

            Map<String, String> config = new HashMap<>();

            config.put("resources", JsonSerialization.writeValueAsString(Collections.singletonList(resource.getId())));
            config.put("applyPolicies", JsonSerialization.writeValueAsString(Collections.singletonList("Only From @keycloak.org or Admin")));

            resourceInstancePermission.setConfig(config);
            authorizationResource.policies().create(resourceInstancePermission);
        } catch (IOException e) {
            throw new RuntimeException("Error creating policy.", e);
        }

        loginToClientPage(aliceUser);
        // should throw an error because the resource was removed and cache entry did not expire yet
        assertFailure();

        userRepresentation.setEmail("alice@keycloak.org");

        userResource.update(userRepresentation);
        loginToClientPage(aliceUser);
        assertSuccess();
    }

    private void assertSuccess() {
        assertState(true);
    }

    private void assertFailure() {
        assertState(false);
    }

    private void assertState(boolean state) {
        clientPage.viewProfile((ResponseValidator) response -> {
            Object res = response.get("res");
            assertThat(res, Matchers.notNullValue());
            Matcher<String> matcher = Matchers.containsString("userName");
            assertThat(res.toString(), state ? matcher : Matchers.not(matcher));
        });
    }

    private void assertTicket() {
        clientPage.viewProfile((ResponseValidator) response -> {
            Object headers = response.get("responseHeaders");
            assertThat(headers, Matchers.notNullValue());

            List<String> headersList = Arrays.asList(headers.toString().split("\r\n"));
            String wwwAuthenticate = headersList.stream()
                    .filter(s -> s.toLowerCase().startsWith("www-authenticate:"))
                    .findFirst()
                    .orElse(null);

            assertThat(wwwAuthenticate, Matchers.notNullValue());
            assertThat(wwwAuthenticate, Matchers.containsString("UMA"));
        });
    }

    public void setTimeOffsetOfAdapter(int offset) {
        this.driver.navigate().to(clientPage.getInjectedUrl() + "timeOffset.jsp?offset=" + offset);
    }
}