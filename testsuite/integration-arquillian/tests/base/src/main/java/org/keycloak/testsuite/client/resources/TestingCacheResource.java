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

package org.keycloak.testsuite.client.resources;

import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.keycloak.utils.MediaType;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface TestingCacheResource {


    @GET
    @Path("/contains/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    boolean contains(@PathParam("id") String id);

    @GET
    @Path("/contains-uuid/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    boolean containsUuid(@PathParam("id") String id);

    @GET
    @Path("/enumerate-keys")
    @Produces(MediaType.APPLICATION_JSON)
    Set<String> enumerateKeys();


    @GET
    @Path("/size")
    @Produces(MediaType.APPLICATION_JSON)
    int size();

    @GET
    @Path("/clear")
    @Consumes(MediaType.TEXT_PLAIN_UTF_8)
    void clear();

    @POST
    @Path("/remove-key/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    void removeKey(@PathParam("id") String id);

    /**
     * Enforce calling of the expiration on the particular infinispan cache. This will immediately expire the expired cache entries, so that they won't be available in the cache.
     * Without calling this, expired entries would be removed by the infinispan expiration (probably by infinispan periodic background cleaner task)
     */
    @POST
    @Path("/process-expiration")
    @Produces(MediaType.APPLICATION_JSON)
    void processExpiration();
}
