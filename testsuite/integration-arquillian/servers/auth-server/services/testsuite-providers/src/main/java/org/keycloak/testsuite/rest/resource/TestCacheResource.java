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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.remoting.transport.Transport;
import org.jgroups.JChannel;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;
import org.keycloak.testsuite.rest.representation.JGroupsStats;
import org.keycloak.utils.MediaType;

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

    @GET
    @Path("/clear")
    @Consumes(MediaType.TEXT_PLAIN_UTF_8)
    public void clear() {
        cache.clear();
    }

    @GET
    @Path("/jgroups-stats")
    @Produces(MediaType.APPLICATION_JSON)
    public JGroupsStats getJgroupsStats() {
        Transport transport = cache.getCacheManager().getTransport();
        if (transport == null) {
            return new JGroupsStats(0, 0, 0, 0);
        } else {
            try {
                // Need to use reflection due some incompatibilities between ispn 8.2.6 and 9.0.1
                JChannel channel = (JChannel) transport.getClass().getMethod("getChannel").invoke(transport);

                return new JGroupsStats(channel.getSentBytes(), channel.getSentMessages(), channel.getReceivedBytes(), channel.getReceivedMessages());
            } catch (Exception nsme) {
                throw new RuntimeException(nsme);
            }
        }
    }


    @GET
    @Path("/remote-cache-stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getRemoteCacheStats() {
        RemoteCache remoteCache = InfinispanUtil.getRemoteCache(cache);
        if (remoteCache == null) {
            return new HashMap<>();
        } else {
            return remoteCache.stats().getStatsMap();
        }
    }


    @GET
    @Path("/remote-cache-last-session-refresh/{user-session-id}")
    @Produces(MediaType.APPLICATION_JSON)
    public int getRemoteCacheLastSessionRefresh(@PathParam("user-session-id") String userSessionId) {
        RemoteCache remoteCache = InfinispanUtil.getRemoteCache(cache);
        if (remoteCache == null) {
            return -1;
        } else {
            UserSessionEntity userSession = (UserSessionEntity) remoteCache.get(userSessionId);
            if (userSession == null) {
                return -1;
            } else {
                return userSession.getLastSessionRefresh();
            }
        }
    }

}
