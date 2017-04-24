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

package org.keycloak.protocol.oidc.endpoints;

import org.keycloak.common.util.UriUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.utils.WebOriginsUtils;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.P3PHelper;
import org.keycloak.utils.MediaType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginStatusIframeEndpoint {

    @Context
    private UriInfo uriInfo;

    @Context
    private KeycloakSession session;

    private RealmModel realm;

    public LoginStatusIframeEndpoint(RealmModel realm) {
        this.realm = realm;
    }

    @GET
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response getLoginStatusIframe(@QueryParam("client_id") String client_id,
                                         @QueryParam("origin") String origin) {
        InputStream resource = getClass().getClassLoader().getResourceAsStream("login-status-iframe.html");
        if (resource != null) {
            P3PHelper.addP3PHeader(session);
            return Response.ok(resource).cacheControl(CacheControlUtil.getDefaultCacheControl()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("init")
    public Response preCheck(@QueryParam("client_id") String clientId, @QueryParam("origin") String origin) {
        try {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.realms().getClientByClientId(clientId, realm);
            if (client != null) {
                Set<String> validWebOrigins = WebOriginsUtils.resolveValidWebOrigins(uriInfo, client);
                validWebOrigins.add(UriUtils.getOrigin(uriInfo.getRequestUri()));
                if (validWebOrigins.contains("*") || validWebOrigins.contains(origin)) {
                    return Response.noContent().build();
                }
            }
        } catch (Throwable t) {
        }
        return Response.status(Response.Status.FORBIDDEN).build();
    }

}
