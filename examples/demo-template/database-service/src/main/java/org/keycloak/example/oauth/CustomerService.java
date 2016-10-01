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

package org.keycloak.example.oauth;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("customers")
public class CustomerService {

    @Context
    private HttpRequest httpRequest;

    @GET
    @Produces("application/json")
    @NoCache
    public List<String> getCustomers() {
        // Just to show how to user info from access token in REST endpoint
        KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class.getName());
        AccessToken accessToken = securityContext.getToken();
        System.out.println(String.format("User '%s' with email '%s' made request to CustomerService REST endpoint", accessToken.getPreferredUsername(), accessToken.getEmail()));

        ArrayList<String> rtn = new ArrayList<String>();
        rtn.add("Bill Burke");
        rtn.add("Stian Thorgersen");
        rtn.add("Stan Silvert");
        rtn.add("Gabriel Cardoso");
        rtn.add("Viliam Rockai");
        rtn.add("Marek Posolda");
        rtn.add("Boleslaw Dawidowicz");
        return rtn;
    }
}
