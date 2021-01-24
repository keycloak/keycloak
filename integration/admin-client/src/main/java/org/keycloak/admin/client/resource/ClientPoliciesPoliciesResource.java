package org.keycloak.admin.client.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.cache.NoCache;

public interface ClientPoliciesPoliciesResource {

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    String getPolicies();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    Response updatePolicies(final String json);
}

