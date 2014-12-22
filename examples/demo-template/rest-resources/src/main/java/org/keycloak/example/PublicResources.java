package org.keycloak.example;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Path("public")
public class PublicResources {

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
