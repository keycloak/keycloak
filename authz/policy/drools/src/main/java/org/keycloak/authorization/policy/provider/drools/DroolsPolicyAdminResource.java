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
package org.keycloak.authorization.policy.provider.drools;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.kie.api.runtime.KieContainer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DroolsPolicyAdminResource implements PolicyProviderAdminService {

    private final ResourceServer resourceServer;
    private final DroolsPolicyProviderFactory factory;

    public DroolsPolicyAdminResource(ResourceServer resourceServer, DroolsPolicyProviderFactory factory) {
        this.resourceServer = resourceServer;
        this.factory = factory;
    }

    @Override
    public void onCreate(Policy policy) {
        this.factory.update(policy);
    }

    @Override
    public void onUpdate(Policy policy) {
        this.factory.update(policy);
    }

    @Override
    public void onRemove(Policy policy) {
        this.factory.remove(policy);
    }

    @Path("/resolveModules")
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response resolveModules(PolicyRepresentation policy) {
        return Response.ok(getContainer(policy).getKieBaseNames()).build();
    }

    @Path("/resolveSessions")
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response resolveSessions(PolicyRepresentation policy) {
        return Response.ok(getContainer(policy).getKieSessionNamesInKieBase(policy.getConfig().get("moduleName"))).build();
    }

    private KieContainer getContainer(PolicyRepresentation policy) {
        String groupId = policy.getConfig().get("mavenArtifactGroupId");
        String artifactId = policy.getConfig().get("mavenArtifactId");
        String version = policy.getConfig().get("mavenArtifactVersion");
        return this.factory.getKieContainer(groupId, artifactId, version);
    }
}
