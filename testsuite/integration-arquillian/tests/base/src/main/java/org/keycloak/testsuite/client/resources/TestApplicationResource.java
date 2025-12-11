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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.keycloak.representations.LogoutToken;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.TestAvailabilityAction;
import org.keycloak.utils.MediaType;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/realms/master/app")
public interface TestApplicationResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/poll-admin-logout")
    LogoutAction getAdminLogoutAction();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/poll-backchannel-logout")
    LogoutToken getBackChannelLogoutToken();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/poll-backchannel-raw-logout")
    String getBackChannelRawLogoutToken();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/poll-frontchannel-logout")
    LogoutToken getFrontChannelLogoutToken();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/poll-admin-not-before")
    PushNotBeforeAction getAdminPushNotBefore();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/poll-test-available")
    TestAvailabilityAction getTestAvailable();

    @POST
    @Path("/clear-admin-actions")
    void clearAdminActions();

    @GET
    @Produces(MediaType.TEXT_HTML_UTF_8)
    @Path("/get-account-profile")
    String getAccountProfile(@QueryParam("token") String token, @QueryParam("account-uri") String accountUri);

    @Path("/oidc-client-endpoints")
    TestOIDCEndpointsApplicationResource oidcClientEndpoints();
}
