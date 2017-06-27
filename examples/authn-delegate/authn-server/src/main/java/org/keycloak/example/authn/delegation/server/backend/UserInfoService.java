package org.keycloak.example.authn.delegation.server.backend;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.keycloak.example.authn.delegation.server.entity.UserResource;

import javax.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/userinfo")
public class UserInfoService {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response viewGet(@Context HttpServletRequest request) {
        Map<String, List<String>> attributes = new HashMap<String, List<String>>();
        attributes.put("firstName", Arrays.asList("Jane"));
        attributes.put("lastName", Arrays.asList("Doe"));        
        attributes.put("email", Arrays.asList("janedoe@example.com"));
        attributes.put("scopes", Arrays.asList("read_account", "write_account"));  
        attributes.put("sex", Arrays.asList("Female"));
        attributes.put("age", Arrays.asList("26"));
        attributes.put("customerRank", Arrays.asList("Gold"));
        // attributes for delegated authentication
        attributes.put("delegated_authenticator", Arrays.asList("http://localhost:8280/authn-delegetion-server/authenticate"));
        attributes.put("delegated_acr", Arrays.asList("3"));
        attributes.put("delegated_amr", Arrays.asList("TwoFactorBiometricsPassword"));
		UserResource res = new UserResource();
		res.setUsername("janedoe@example.com");
		res.setAttributes(attributes);
        return Response.ok(res).build();
    }
	
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response viewPost(@Context HttpServletRequest request) {
        Map<String, List<String>> attributes = new HashMap<String, List<String>>();
        attributes.put("firstName", Arrays.asList("Jane"));
        attributes.put("lastName", Arrays.asList("Doe"));        
        attributes.put("email", Arrays.asList("janedoe@example.com"));
        attributes.put("scopes", Arrays.asList("read_account", "write_account"));  
        attributes.put("sex", Arrays.asList("Female"));
        attributes.put("age", Arrays.asList("26"));
        attributes.put("customerRank", Arrays.asList("Gold"));
        // attributes for delegated authentication
        attributes.put("delegated_authenticator", Arrays.asList("http://localhost:8280/authn-delegetion-server/authenticate"));
        attributes.put("delegated_acr", Arrays.asList("3"));
        attributes.put("delegated_amr", Arrays.asList("TwoFactorBiometricsPassword"));
		UserResource res = new UserResource();
		res.setUsername("janedoe@example.com");
		res.setAttributes(attributes);
        return Response.ok(res).build();
    }
}
