package org.keycloak.services.resources;

import org.jboss.resteasy.spi.HttpRequest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Cors {

    private HttpRequest request;
    private ResponseBuilder response;
    private Set<String> allowedOrigins;

    public Cors(HttpRequest request, ResponseBuilder response) {
        this.request = request;
        this.response = response;
    }

    public static Cors add(HttpRequest request, ResponseBuilder response) {
        return new Cors(request, response);
    }

    public Cors allowedOrigins(Set<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
        return this;
    }

    public Response build() {
        String origin = request.getHttpHeaders().getHeaderString("Origin");
        if (origin == null || allowedOrigins == null || (!allowedOrigins.contains(origin))) {
            return response.build();
        }

        response.header("Access-Control-Allow-Origin", origin);
        return response.build();
    }

}
