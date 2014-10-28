/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.example.multitenant.business.backend.boundary;

import org.keycloak.example.multitenant.business.backend.entity.MetricsResponse;
import java.security.Principal;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.keycloak.KeycloakPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Juraci Paixão Kröhling <juraci at kroehling.de>
 */
@Stateless
@Path("/metrics")
@RolesAllowed({"agent"})
public class MetricsService {

    @Resource
    SessionContext sessionContext;

    Logger logger = LoggerFactory.getLogger(MetricsService.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public MetricsResponse retrieve() {
        Principal principal = sessionContext.getCallerPrincipal();
        KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) principal;
        String realm = keycloakPrincipal.getKeycloakSecurityContext().getToken().getIssuer();
        logger.warn("User for this call is: " + principal.getName());
        logger.warn("User belongs to this realm: " + realm);

        return new MetricsResponse("Success");
    }

}
