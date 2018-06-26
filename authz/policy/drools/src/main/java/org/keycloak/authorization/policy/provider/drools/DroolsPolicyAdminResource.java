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

import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.representations.idm.authorization.RulePolicyRepresentation;
import org.keycloak.services.ErrorResponse;
import org.kie.api.runtime.KieContainer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DroolsPolicyAdminResource implements PolicyProviderAdminService {

    private final DroolsPolicyProviderFactory factory;

    public DroolsPolicyAdminResource(DroolsPolicyProviderFactory factory) {
        this.factory = factory;
    }

    @Path("/resolveModules")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/json")
    public Response resolveModules(RulePolicyRepresentation policy) {
        return Response.ok(getContainer(policy).getKieBaseNames()).build();
    }

    @Path("/resolveSessions")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resolveSessions(RulePolicyRepresentation policy) {
        return Response.ok(getContainer(policy).getKieSessionNamesInKieBase(policy.getModuleName())).build();
    }

    private KieContainer getContainer(RulePolicyRepresentation policy) {
        final String groupId = policy.getArtifactGroupId();
        final String artifactId = policy.getArtifactId();
        final String version = policy.getArtifactVersion();
        try {
            return this.factory.getKieContainer(groupId, artifactId, version);
        } catch (RuntimeException re) {
            throw new WebApplicationException(ErrorResponse.error(
                    "Unable to locate artifact " + groupId + ":" + artifactId + ":" + version, Response.Status.BAD_REQUEST));
        }
    }
}
