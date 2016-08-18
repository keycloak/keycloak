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

package org.keycloak.testsuite.client;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.testsuite.client.resources.TestApplicationResource;
import org.keycloak.testsuite.client.resources.TestExampleCompanyResource;
import org.keycloak.testsuite.client.resources.TestingResource;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KeycloakTestingClient {
    private final ResteasyWebTarget target;
    private final ResteasyClient client;

    KeycloakTestingClient(String serverUrl, ResteasyClient resteasyClient) {
        client = resteasyClient != null ? resteasyClient : new ResteasyClientBuilder().connectionPoolSize(10).build();
        target = client.target(serverUrl);
    }

    public static KeycloakTestingClient getInstance(String serverUrl) {
        return new KeycloakTestingClient(serverUrl, null);
    }

    public TestingResource testing() {
        return target.proxy(TestingResource.class);
    }

    public TestApplicationResource testApp() { return target.proxy(TestApplicationResource.class); }

    public TestExampleCompanyResource testExampleCompany() { return target.proxy(TestExampleCompanyResource.class); }

    public void close() {
        client.close();
    }
}
