/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.representations.idm.UserProfileMetadata;
import org.keycloak.representations.userprofile.config.UPConfig;

/**
 * @author Vlastimil Elias <velias@redhat.com>
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface UserProfileResource {

    /**
     * @return user profile configuration
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    UPConfig getConfiguration();

    @GET
    @Path("/metadata")
    @Consumes(MediaType.APPLICATION_JSON)
    UserProfileMetadata getMetadata();

    /**
     * Updates user profile configuration. Using null as an argument could mean restart of the configuration to the default configuration
     *
     * @param config Could be null, which can mean restart to the default user-profile configuration (Can depend on the implementation)
     * @return
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    void update(UPConfig config);
}
