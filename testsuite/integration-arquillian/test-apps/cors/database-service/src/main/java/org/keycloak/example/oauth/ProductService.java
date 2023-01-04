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

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("products")
public class ProductService {

    @Context
    private HttpServletResponse response;

    @GET
    @Produces("application/json")
    @NoCache
    public List<String> getProducts() {
        ArrayList<String> rtn = new ArrayList<String>();
        rtn.add("iphone");
        rtn.add("ipad");
        rtn.add("ipod");

        response.addHeader("X-Custom1", "some-value");
        response.addHeader("WWW-Authenticate", "some-value");
        return rtn;
    }
}
