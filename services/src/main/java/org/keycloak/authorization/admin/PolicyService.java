/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.admin;

import java.io.IOException;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyService extends BasePolicyService {
    public PolicyService(ResourceServer resourceServer, AuthorizationProvider authorization, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        super(resourceServer, authorization, auth, adminEvent);
    }

    @Path("{type}")
    public Object getResourceServiceOrTypeService(@PathParam("type") String type) {
        try {
            return getTypeService(type);
        } catch (ErrorResponseException ex) {
            return getResourceService(type);
        }
    }

    @Path("/by-type/{policy-type}")
    public PolicyTypeService getTypeService(@PathParam("policy-type") String type) {
        PolicyProviderFactory providerFactory = getPolicyProviderFactory(type);

        if (providerFactory != null) {
            return new PolicyTypeService(type, resourceServer, authorization, auth, adminEvent);
        }
        throw new ErrorResponseException("No policy provider found for this type.", "Invalid type", Status.NOT_FOUND);
    }

    @Path("/by-id/{policy-id}")
    public PolicyResourceService getResourceService(@PathParam("policy-id") String policyId) {
        Policy policy = authorization.getStoreFactory().getPolicyStore().findById(resourceServer, policyId);

        return new PolicyResourceService(policy, resourceServer, authorization, auth, adminEvent);
    }

    protected AbstractPolicyRepresentation doCreateRepresentation(String payload) {
        PolicyRepresentation representation;

        try {
            representation = JsonSerialization.readValue(payload, PolicyRepresentation.class);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to deserialize representation", cause);
        }

        return representation;
    }
}
