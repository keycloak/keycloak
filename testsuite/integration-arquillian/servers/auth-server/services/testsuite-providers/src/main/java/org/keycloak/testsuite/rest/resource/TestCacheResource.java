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

package org.keycloak.testsuite.rest.resource;

import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.infinispan.Cache;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TestCacheResource {

    private final Cache<Object, Object> cache;

    public TestCacheResource(KeycloakSession session, String cacheName) {
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        cache = provider.getCache(cacheName);
    }


    @GET
    @Path("/contains/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean contains(@PathParam("id") String id) {
        return cache.containsKey(id);
    }


    @GET
    @Path("/enumerate-keys")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> enumerateKeys() {
        return cache.keySet().stream().map((Object o) -> {

            return o.toString();

        }).collect(Collectors.toSet());
    }


    @GET
    @Path("/size")
    @Produces(MediaType.APPLICATION_JSON)
    public int size() {
        return cache.size();
    }

}
