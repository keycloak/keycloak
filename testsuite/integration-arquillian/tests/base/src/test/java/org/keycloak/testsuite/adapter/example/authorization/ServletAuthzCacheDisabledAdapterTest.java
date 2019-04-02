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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

import java.io.File;
import java.io.IOException;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
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
public class ServletAuthzCacheDisabledAdapterTest extends AbstractServletAuthzAdapterTest {

    @Deployment(name = RESOURCE_SERVER_ID, managed = false)
    public static WebArchive deployment() throws IOException {
        return exampleDeployment(RESOURCE_SERVER_ID)
                .addAsWebInfResource(new File(TEST_APPS_HOME_DIR + "/servlet-authz-app/keycloak-cache-disabled-authz-service.json"), "keycloak.json");
    }

    @Test
    public void testCreateNewResource() {
        performTests(() -> {
            login("alice", "alice");
            assertWasNotDenied();

            this.driver.navigate().to(getResourceServerUrl() + "/new-resource");
            assertWasNotDenied();

            ResourceRepresentation resource = new ResourceRepresentation();

            resource.setName("New Resource");
            resource.setUri("/new-resource");

            getAuthorizationResource().resources().create(resource);

            ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

            permission.setName(resource.getName() + " Permission");
            permission.addResource(resource.getName());
            permission.addPolicy("Deny Policy");

            permission = getAuthorizationResource().permissions().resource().create(permission).readEntity(ResourcePermissionRepresentation.class);

            login("alice", "alice");
            assertWasNotDenied();

            this.driver.navigate().to(getResourceServerUrl() + "/new-resource");
            assertWasDenied();

            permission = getAuthorizationResource().permissions().resource().findById(permission.getId()).toRepresentation();

            permission.removePolicy("Deny Policy");
            permission.addPolicy("Any User Policy");

            getAuthorizationResource().permissions().resource().findById(permission.getId()).update(permission);

            login("alice", "alice");
            assertWasNotDenied();

            this.driver.navigate().to(getResourceServerUrl() + "/new-resource");
            assertWasNotDenied();
        });
    }
}
