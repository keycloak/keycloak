package org.keycloak.admin.client.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.cache.NoCache;

public interface ClientPoliciesProfilesResource {

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    String getProfiles();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    Response updateProfiles(final String json);
}
