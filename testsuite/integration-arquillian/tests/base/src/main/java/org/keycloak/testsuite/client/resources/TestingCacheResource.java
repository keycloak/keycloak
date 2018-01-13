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

import org.keycloak.testsuite.rest.representation.JGroupsStats;
import org.keycloak.testsuite.rest.representation.RemoteCacheStats;
import org.keycloak.utils.MediaType;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.Set;

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

    @GET
    @Path("/jgroups-stats")
    @Produces(MediaType.APPLICATION_JSON)
    JGroupsStats getJgroupsStats();

    @GET
    @Path("/remote-cache-stats")
    @Produces(MediaType.APPLICATION_JSON)
    RemoteCacheStats getRemoteCacheStats();

    @GET
    @Path("/remote-cache-last-session-refresh/{user-session-id}")
    @Produces(MediaType.APPLICATION_JSON)
    int getRemoteCacheLastSessionRefresh(@PathParam("user-session-id") String userSessionId);

}
