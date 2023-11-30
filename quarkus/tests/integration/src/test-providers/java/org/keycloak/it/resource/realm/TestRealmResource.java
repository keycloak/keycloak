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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class TestRealmResource implements RealmResourceProvider {
    protected static final Logger logger = Logger.getLogger(TestRealmResource.class);
    
    @Override
    public Object getResource() {
        return this;
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

    @Override
    public void close() {

    }
}
