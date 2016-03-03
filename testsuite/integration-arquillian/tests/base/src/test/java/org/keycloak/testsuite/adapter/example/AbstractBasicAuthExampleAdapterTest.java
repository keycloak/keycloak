/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.adapter.example;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.BasicAuthExample;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.EXAMPLE;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;

public abstract class AbstractBasicAuthExampleAdapterTest extends AbstractExampleAdapterTest {

    @Page
    private BasicAuthExample basicAuthExample;

    @Deployment(name = BasicAuthExample.DEPLOYMENT_NAME)
    private static WebArchive basicAuthExample() throws IOException {
        return exampleDeployment("examples-basicauth");
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm(new File(EXAMPLES_HOME_DIR + "/basic-auth/basicauthrealm.json")));
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(EXAMPLE);
    }

    @Test
    public void testBasicAuthExample() {
        String value = "hello";
        Client client = ClientBuilder.newClient();

        Response response = client.target(basicAuthExample
                .setTemplateValues("admin", "password", value).buildUri()).request().get();
        assertEquals(200, response.getStatus());
        assertEquals(value, response.readEntity(String.class));
        response.close();

        response = client.target(basicAuthExample
                .setTemplateValues("invalid-user", "password", value).buildUri()).request().get();
        assertEquals(401, response.getStatus());
        String readResponse = response.readEntity(String.class);
        assertTrue(readResponse.contains("Unauthorized") || readResponse.contains("Status 401"));
        response.close();

        response = client.target(basicAuthExample
                .setTemplateValues("admin", "invalid-password", value).buildUri()).request().get();
        assertEquals(401, response.getStatus());
        readResponse = response.readEntity(String.class);
        assertTrue(readResponse.contains("Unauthorized") || readResponse.contains("Status 401"));
        response.close();

        client.close();
    }

}
