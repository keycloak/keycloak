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
package org.keycloak.testsuite.adapter.example.authorization;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.keycloak.common.Profile.Feature.AUTHORIZATION;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;

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
public class DefaultAuthzConfigAdapterTest extends AbstractExampleAdapterTest {

    private static final String REALM_NAME = "hello-world-authz";
    private static final String RESOURCE_SERVER_ID = "hello-world-authz-service";

    @ArquillianResource
    private Deployer deployer;

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(AUTHORIZATION);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(
                loadRealm(new File(TEST_APPS_HOME_DIR + "/hello-world-authz-service/hello-world-authz-realm.json")));
    }

    @Deployment(name = RESOURCE_SERVER_ID, managed = false)
    public static WebArchive deployment() throws IOException {
        return exampleDeployment(RESOURCE_SERVER_ID);
    }

    @Test
    public void testDefaultAuthzConfig() throws Exception {
        try {
            configureAuthorizationServices();
            this.deployer.deploy(RESOURCE_SERVER_ID);

            login();

            assertTrue(this.driver.getPageSource().contains("Your permissions are"));
            assertTrue(this.driver.getPageSource().contains("Default Resource"));

            boolean hasDefaultPermission = false;
            boolean hasDefaultPolicy = false;

            for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
                if ("Default Policy".equals(policy.getName())) {
                    hasDefaultPolicy = true;
                }
                if ("Default Permission".equals(policy.getName())) {
                    hasDefaultPermission = true;
                }
            }

            assertTrue(hasDefaultPermission);
            assertTrue(hasDefaultPolicy);
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    private void login() throws MalformedURLException {
        this.driver.navigate().to(getResourceServerUrl() + "/");
        this.loginPage.form().login("alice", "alice");
    }

    private URL getResourceServerUrl() throws MalformedURLException {
        return new URL(ServerURLs.getAppServerContextRoot() + "/" + RESOURCE_SERVER_ID);
    }

    private void configureAuthorizationServices() {
        ClientsResource clients = realmsResouce().realm(REALM_NAME).clients();
        ClientRepresentation client = clients.findByClientId(RESOURCE_SERVER_ID).get(0);

        client.setAuthorizationServicesEnabled(false);

        // disables authorization services and remove authorization configuration from the client app
        clients.get(client.getId()).update(client);

        client.setAuthorizationServicesEnabled(true);

        // enable authorization services in order to generate the default config and continue with tests
        clients.get(client.getId()).update(client);
    }

    private AuthorizationResource getAuthorizationResource() throws FileNotFoundException {
        return getClientResource(RESOURCE_SERVER_ID).authorization();
    }

    private ClientResource getClientResource(String clientId) {
        ClientsResource clients = this.realmsResouce().realm(REALM_NAME).clients();
        ClientRepresentation resourceServer = clients.findByClientId(clientId).get(0);
        return clients.get(resourceServer.getId());
    }
}
