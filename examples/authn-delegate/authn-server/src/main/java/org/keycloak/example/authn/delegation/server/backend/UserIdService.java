package org.keycloak.example.authn.delegation.server.backend;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;


@Path("/userid")
public class UserIdService {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getUserId(@QueryParam("artifact") String artifact) {
    	return "userid=GETciekan9882ivndc";
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String getUserId() {
    	return "userid=POSTciekan9882ivndc";
    }

}
