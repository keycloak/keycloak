package org.keycloak.example.authn.delegation.server.frontend;

import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.keycloak.example.authn.delegation.server.entity.KeycloakCredential;
import org.keycloak.example.authn.delegation.server.util.Transaction;

@Path("/getback")
@Transaction
public class GetBackService {
	
	private final String GETBACK_REALM = "authn-delegation";
	private final String GETBACK_BASE_URI = "http://localhost:8080/auth/realms/" + GETBACK_REALM + "/login-actions/authenticate";
	
    @Inject
    private EntityManager entityManager;

    @Context
    private HttpServletRequest request;
    
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response getBack(
    		@Context HttpServletRequest request,
    		@Context UriInfo uriInfo,
    		MultivaluedMap<String, String> params) {
    	String mark = params.getFirst("mark");
    	System.out.println("getback : mark = " + mark);  	

    	KeycloakCredential kc = null;
    	try {
    		 kc = this.entityManager.find(KeycloakCredential.class, mark);
    	} catch (Exception e) {
    		System.out.println("getback error msg = " + e.getMessage());
    	}
    	if (kc == null) {
    		System.out.println("getback error kc is null.");
    	}
    	String code = kc.getCode();
    	String execution = kc.getExecution();
    	
    	StringBuilder builder = new StringBuilder(GETBACK_BASE_URI);
    	builder.append("/?code=").append(code).append("&execution=").append(execution);
    	String getBackUri = builder.toString();
    	this.entityManager.remove(kc);
    	
    	return Response.ok(getFormPostBody(getBackUri, "983wn4r498hfsadugv90r3woi")).build();
    }
    
	private String getFormPostBody(String getBackUri, String artifact) {

		StringBuilder builder = new StringBuilder();

        builder.append("<HTML>");
        builder.append("<HEAD>");
        builder.append("<TITLE>Submit This Form</TITLE>");
        builder.append("</HEAD>");
        builder.append("<BODY Onload=\"javascript:document.forms[0].submit()\">");
        builder.append("<FORM METHOD=\"POST\" ACTION=\"" + getBackUri + "\">");
        builder.append("<INPUT name=\"artifact\" TYPE=\"HIDDEN\" VALUE=\"" + artifact + "\" />");        
        builder.append("<NOSCRIPT>")
        .append("<P>JavaScript is disabled. We strongly recommend to enable it. Click the button below to continue.</P>")
        .append("<INPUT TYPE=\"SUBMIT\" VALUE=\"CONTINUE\" />")
        .append("</NOSCRIPT>");
        builder.append("</FORM></BODY></HTML>");
		return builder.toString();
	}
}
