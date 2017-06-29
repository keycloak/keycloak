package org.keycloak.example.authn.delegation.server.frontend;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Encoder;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
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

@Path("/authenticate")
@Transaction
public class AuthenticationService {
	
    @Inject
    private EntityManager entityManager;

    @Context
    private HttpServletRequest request;
	
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response createLoginForm(
    		@Context HttpServletRequest request,
    		@Context UriInfo uriInfo,
    		@QueryParam("code") String code,
    		@QueryParam("execution") String execution,
    		@QueryParam("client_id") String clientId) {
		KeycloakCredential kc = new KeycloakCredential();
    	String mark = getMark();
    	System.out.println("authenticate : mark = " + mark);
		kc.setCode(code);
		kc.setExecution(execution);
		kc.setMark(mark);
		this.entityManager.persist(kc);
    	
    	return Response.ok(getLoginForm(code, execution, clientId, "/authn-delegation-server/getback", mark)).build();
    }
 
    @POST
    @Produces(MediaType.TEXT_HTML)
    public Response create(
    		@Context HttpServletRequest request,
    		@Context UriInfo uriInfo,
    		MultivaluedMap<String, String> params) {
    	String code = params.getFirst("code");
    	String execution = params.getFirst("execution");
    	String clientId = params.getFirst("client_id");
    	String mark = getMark();

		KeycloakCredential kc = new KeycloakCredential();
		kc.setCode(code);
		kc.setExecution(execution);
		kc.setMark(mark);
		this.entityManager.persist(kc);
    	
    	return Response.ok(getLoginForm(code, execution, clientId, "/authn-delegation-server/getback", mark)).build();
    }
    
    private static byte[] generateSecret() {
        return generateSecret(32);
    }

    private static byte[] generateSecret(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return buf;
    }
    
    private static String getMark() {
    	/*
    	Encoder enc = Base64.getUrlEncoder();
    	String mark = enc.encodeToString(generateSecret());
    	return mark;
    	*/
    	return "pseudoMARK777";
    }
    
	private String getLoginForm(String code, String execution, String clientId, String actionUrl, String mark) {
		
		StringBuilder builder = new StringBuilder();
		
        builder.append("<html>")
        .append("<head>")
        .append("<meta charset=\"UTF-8\" />")
        .append("<title>Delegated Authentication</title>")
        .append("</head>");

		builder.append("<body>")
		.append("<h1>Meta Info</h1>")
    	.append("<p><span>In HTTP POST Binding</span></p>")
    	.append("<p><span>code = " + code + "</span></p>")
    	.append("<p><span>execution = " + execution + "</span></p>")
        .append("<p><span>client_id = " + clientId + "</span></p>");

        builder.append("<div>")
        .append("<form action=\"" + actionUrl + "\" method=\"post\">")
        .append("<h2>Exteral Authentication Server Login</h2>")
        .append("<label for=\"username\">User Account</label>")
        .append("<input name=\"username\" id=\"username\" value=\"\" />")
        .append("<label for=\"password\">Password</label>")
        .append("<input name=\"password\" id=\"password\" value=\"\" />")
        .append("<input type=\"hidden\" name=\"mark\" id=\"mark\" value=\"" + mark + "\" />")
        .append("<button name=\"login\">Log In</button>")
        .append("</form>")
        .append("</div>");		
			
		builder.append("</body>")
        .append("</html>");			
			
		return builder.toString();
	}
}
