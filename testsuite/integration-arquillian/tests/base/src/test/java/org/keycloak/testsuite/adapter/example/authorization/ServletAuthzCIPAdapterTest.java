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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_DEPRECATED)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT7)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT8)
@AppServerContainer(ContainerConstants.APP_SERVER_TOMCAT9)
public class ServletAuthzCIPAdapterTest extends AbstractServletAuthzAdapterTest {

    @Deployment(name = RESOURCE_SERVER_ID, managed = false)
    public static WebArchive deployment() throws IOException {
        return exampleDeployment(RESOURCE_SERVER_ID)
                .addAsWebInfResource(new File(TEST_APPS_HOME_DIR + "/servlet-authz-app/keycloak-claim-information-point-authz-service.json"), "keycloak.json");
    }

    @Test
    public void testClaimInformationPoint() {
        performTests(() -> {
            login("alice", "alice");
            assertWasNotDenied();

            this.driver.navigate().to(getResourceServerUrl() + "/protected/context/context.jsp?request-claim=unexpected-value");

            assertWasDenied();

            this.driver.navigate().to(getResourceServerUrl() + "/protected/context/context.jsp?request-claim=expected-value");
            assertWasNotDenied();
            hasText("Access granted: true");

            this.driver.navigate().to(getResourceServerUrl() + "/protected/context/context.jsp");

            assertWasDenied();
        });
    }

    @Test
    public void testReuseBodyAfterClaimProcessing() {
        performTests(() -> {
            OAuthClient.AccessTokenResponse response = oauth.realm("servlet-authz").clientId("servlet-authz-app")
                    .doGrantAccessTokenRequest("secret", "alice", "alice");
            Client client = ClientBuilder.newClient();
            Map<String, String> body = new HashMap();
            
            body.put("test", "test-value");
            
            Response post = client.target(getResourceServerUrl() + "/protected/filter/body")
                    .request()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + response.getAccessToken())
                    .post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));

            body = post.readEntity(Map.class);

            Assert.assertEquals("test-value", body.get("test"));
        });
    }
}
