/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.it.resource.realm;

import org.infinispan.Cache;
import org.infinispan.commons.configuration.io.ConfigurationWriter;
import org.infinispan.commons.io.StringBuilderWriter;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class TestRealmResource implements RealmResourceProvider {
    protected static final Logger logger = Logger.getLogger(TestRealmResource.class);

    final InfinispanConnectionProvider infinispanConnectionProvider;
    final KeycloakSession session;

    public TestRealmResource(KeycloakSession session) {
        this.session = session;
        this.infinispanConnectionProvider = session.getProvider(InfinispanConnectionProvider.class);
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Path("trusted")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response trustedResponse() throws Exception {
        if (session.getContext().getHttpRequest().isProxyTrusted()) {
            return Response.ok("{}", MediaType.APPLICATION_JSON).build();
        }
        return Response.noContent().build();
    }

    @Path("slow")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response slowResource() throws Exception {
        final int sleep = 5000;
        logger.info("Sleeping for " + sleep + " millis");
        Thread.sleep(sleep);
        logger.info("Waking up...");
        return Response.noContent().build();
    }

    @Path("cache/{cache}/config")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response cacheConfig(@PathParam("cache") String cacheName) {
        Cache<?, ?> cache = infinispanConnectionProvider.getCache(cacheName, false);
        if (cache == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        StringBuilderWriter out = new StringBuilderWriter();
        try (ConfigurationWriter writer = ConfigurationWriter.to(out)
              .withType(org.infinispan.commons.dataconversion.MediaType.APPLICATION_JSON)
              .prettyPrint(true)
              .build()) {
            new ParserRegistry().serialize(writer, cacheName, cache.getCacheConfiguration());
        }
        return Response.ok(out.toString(), MediaType.APPLICATION_JSON).build();
    }

    @Override
    public void close() {

    }
}
