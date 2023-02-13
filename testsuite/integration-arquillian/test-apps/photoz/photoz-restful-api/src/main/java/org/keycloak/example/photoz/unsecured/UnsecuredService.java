/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.example.photoz.unsecured;


import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.GET;
import org.jboss.logging.Logger;
import org.keycloak.example.photoz.CustomDatabase;

/**
 * Service used to ensure there is clean DB before test 
 * 
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
@Path("/unsecured/clean")
public class UnsecuredService {

    private final Logger log = Logger.getLogger(UnsecuredService.class);

    private CustomDatabase customDatabase = CustomDatabase.create();

    @GET
    @Produces("application/json")
    public Response cleanAll() {
        int deleted = customDatabase.cleanAll();
        
        if (deleted != 0) {
            log.warnf("Database was not empty. Deleted Albums + Photos {0}", deleted);
        } else {
            log.debug("Database was clean before test");
        }
        return Response.ok().build();
    }
}
