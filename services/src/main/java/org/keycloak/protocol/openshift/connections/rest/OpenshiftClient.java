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
package org.keycloak.protocol.openshift.connections.rest;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.protocol.openshift.connections.rest.api.Api;
import org.keycloak.protocol.openshift.connections.rest.apis.Apis;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OpenshiftClient {
    protected String token;
    protected String baseUrl;
    protected ResteasyClient httpClient;
    private final ResteasyWebTarget target;
    private final ResteasyWebTarget api;
    private final ResteasyWebTarget apis;


     protected OpenshiftClient(String baseUrl, String token, ResteasyClient httpClient) {
        this.httpClient = httpClient;
        this.token = token;
        this.baseUrl = baseUrl;
        target = httpClient.target(baseUrl);
        target.register(new BearerAuthFilter(token));
        api = target.path("api");
        apis = target.path("apis");
    }

    public static OpenshiftClient instance(String baseUrl, String token) {
        return new OpenshiftClient(baseUrl, token, new ResteasyClientBuilder().connectionPoolSize(1).disableTrustManager().build());
    }

    public Api api() {
        return api.proxy(Api.class);
    }
    public Apis apis() {
        return apis.proxy(Apis.class);
    }

    public void close() {
        httpClient.close();
    }
}
