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

import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.common.enums.HostnameVerificationPolicy;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.utils.MediaType;

import org.infinispan.commons.time.TimeService;
import org.jboss.resteasy.reactive.NoCache;

/**
 * For an implementation see TestingResourceProvider
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */

@Path("/testing")
@Consumes(MediaType.APPLICATION_JSON)
public interface TestingResource {

    @POST
    @Path("/poll-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    EventRepresentation pollEvent();

    @POST
    @Path("/poll-admin-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    AdminEventRepresentation pollAdminEvent();

    @POST
    @Path("/clear-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    void clearEventQueue();

    @POST
    @Path("/clear-admin-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    void clearAdminEventQueue();

    /**
     * Will set Inifispan's {@link TimeService} that is aware of Keycloak time shifts to the infinispan {@code CacheManager} before the test.
     * This will allow infinispan expiration to be aware of Keycloak {@link org.keycloak.common.util.Time#setOffset}
     */
    @POST
    @Path("/set-testing-infinispan-time-service")
    @Produces(MediaType.APPLICATION_JSON)
    void setTestingInfinispanTimeService();

    @POST
    @Path("/revert-testing-infinispan-time-service")
    @Produces(MediaType.APPLICATION_JSON)
    void revertTestingInfinispanTimeService();

    @GET
    @Path("/test-amphibian-component")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Map<String, Object>> getTestAmphibianComponentDetails();

    @PUT
    @Path("/set-krb5-conf-file")
    @Consumes(MediaType.APPLICATION_JSON)
    void setKrb5ConfFile(@QueryParam("krb5-conf-file") String krb5ConfFile);

    @POST
    @Path("/run-on-server")
    @Consumes(MediaType.TEXT_PLAIN_UTF_8)
    @Produces(MediaType.TEXT_PLAIN_UTF_8)
    String runOnServer(String runOnServer);

    @POST
    @Path("/run-on-server")
    @Consumes(MediaType.TEXT_PLAIN_UTF_8)
    @Produces(MediaType.TEXT_PLAIN_UTF_8)
    Response runOnServerWithResponse(String runOnServer);

    @POST
    @Path("/run-model-test-on-server")
    @Consumes(MediaType.TEXT_PLAIN_UTF_8)
    @Produces(MediaType.TEXT_PLAIN_UTF_8)
    String runModelTestOnServer(@QueryParam("testClassName") String testClassName,
                                @QueryParam("testMethodName") String testMethodName);

    @GET
    @Path("/list-disabled-features")
    @Produces(MediaType.APPLICATION_JSON)
    Set<Profile.Feature> listDisabledFeatures();

    @POST
    @Path("/enable-feature/{feature}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Set<Profile.Feature> enableFeature(@PathParam("feature") String feature);

    @POST
    @Path("/disable-feature/{feature}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Set<Profile.Feature> disableFeature(@PathParam("feature") String feature);

    /**
     * Resets the given feature to it's default state.
     *
     * @param feature
     */
    @POST
    @Path("/reset-feature/{feature}")
    @Consumes(MediaType.APPLICATION_JSON)
    void resetFeature(@PathParam("feature") String feature);

    /**
     * This method is here just to have all endpoints from TestingResourceProvider available here.
     * <p>
     * But usually it is requested to call this endpoint through WebDriver. See URLUtils.sendPOSTWithWebDriver for more details
     */
    @GET
    @Path("/simulate-post-request")
    @Produces(MediaType.TEXT_HTML_UTF_8)
    Response simulatePostRequest(@QueryParam("postRequestUrl") String postRequestUrl,
                                 @QueryParam("encodedFormParameters") String encodedFormParameters);

    /**
     * Temporarily disables truststore SPI from the file. Useful for example to test some error scenarios, which require truststore SPI to be unset (or set incorrectly)
     */
    @GET
    @Path("/disable-truststore-spi")
    @NoCache
    void disableTruststoreSpi();

    /**
     * Temporarily changes the truststore SPI with another hostname verification policy. Call reenableTruststoreSpi to revert.
     *
     * @param hostnamePolicy The hostname verification policy to set
     */
    @GET
    @Path("/modify-truststore-spi-hostname-policy")
    @NoCache
    public void modifyTruststoreSpiHostnamePolicy(@QueryParam("hostnamePolicy") final HostnameVerificationPolicy hostnamePolicy);

    /**
     * Re-enable truststore SPI after it was temporarily disabled by {@link #disableTruststoreSpi()}
     */
    @GET
    @Path("/reenable-truststore-spi")
    @NoCache
    void reenableTruststoreSpi();

    @GET
    @Path("/no-cache-annotated-endpoint")
    Response getNoCacheAnnotatedEndpointResponse(@QueryParam("programmatic_max_age_value") Long programmaticMaxAgeValue);
}
