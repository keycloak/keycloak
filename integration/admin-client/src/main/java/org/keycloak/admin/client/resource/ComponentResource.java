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
package org.keycloak.admin.client.resource;

import java.util.List;

import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ComponentTypeRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ComponentResource {
    @GET
    ComponentRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void update(ComponentRepresentation rep);

    @DELETE
    void remove();

    /**
     * List of subcomponent types that are available to configure for a particular parent component.
     *
     * @param subtype fully qualified name of the java class of the provider, which is subtype of this component
     * @return
     */
    @GET
    @Path("sub-component-types")
    @Produces(MediaType.APPLICATION_JSON)
    List<ComponentTypeRepresentation> getSubcomponentConfig(@QueryParam("type") String subtype);
}
