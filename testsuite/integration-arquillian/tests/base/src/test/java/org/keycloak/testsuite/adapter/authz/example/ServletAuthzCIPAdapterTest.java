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
package org.keycloak.testsuite.adapter.authz.example;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
public class ServletAuthzCIPAdapterTest extends AbstractServletAuthzAdapterTest {

    @Deployment(name = RESOURCE_SERVER_ID, managed = false)
    public static WebArchive deployment() throws IOException {
        return exampleDeployment(RESOURCE_SERVER_ID)
                .addAsWebInfResource(new File(TEST_APPS_HOME_DIR + "/servlet-authz-app/keycloak-claim-information-point-authz-service.json"), "keycloak.json");
    }

    @Test
    public void testReuseBodyAfterClaimProcessing() {
        performTests(() -> {
            OAuthClient.AccessTokenResponse response = oauth.realm("servlet-authz").clientId("servlet-authz-app")
                    .doGrantAccessTokenRequest("secret", "alice", "alice");
            Client client = AdminClientUtil.createResteasyClient();
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
