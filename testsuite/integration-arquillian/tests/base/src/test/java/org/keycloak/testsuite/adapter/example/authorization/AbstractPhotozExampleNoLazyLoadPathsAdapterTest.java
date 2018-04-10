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

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.keycloak.testsuite.adapter.page.PhotozClientAuthzTestApp;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractPhotozExampleNoLazyLoadPathsAdapterTest extends AbstractPhotozExampleAdapterTest {

    @Deployment(name = PhotozClientAuthzTestApp.DEPLOYMENT_NAME)
    public static WebArchive deploymentClient() throws IOException {
        return exampleDeployment(PhotozClientAuthzTestApp.DEPLOYMENT_NAME);
    }

    @Deployment(name = RESOURCE_SERVER_ID, managed = false, testable = false)
    public static WebArchive deploymentResourceServer() throws IOException {
        return exampleDeployment(RESOURCE_SERVER_ID);
    }

}