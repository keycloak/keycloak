package org.keycloak.example;

import org.jboss.resteasy.annotations.cache.NoCache;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import java.security.Principal;

@Path("secret")
public class SecretResources {

    @Context
    HttpServletRequest request;

    @POST
    @Consumes("text/plain")
    @Produces("text/plain")
    @NoCache
    public String get(String text) {
        StringBuilder result = new StringBuilder();
        Principal userPrincipal = request.getUserPrincipal();
        if(userPrincipal != null){
            result.append("Hello ").append(userPrincipal.getName()).append("\r\n");
        }
        result.append("You said: ").append(text);

        return result.toString();
    }
}
