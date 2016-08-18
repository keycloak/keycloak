/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.protection.permission;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.protection.permission.representation.PermissionRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Arrays;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionService extends AbstractPermissionService {

    public PermissionService(KeycloakIdentity identity, ResourceServer resourceServer, AuthorizationProvider authorization) {
        super(identity, resourceServer, authorization);
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response create(PermissionRequest request) {
        return create(Arrays.asList(request));
    }

}