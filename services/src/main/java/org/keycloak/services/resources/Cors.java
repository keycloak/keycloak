package org.keycloak.services.resources;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Cors {

    public static final long DEFAULT_MAX_AGE = TimeUnit.HOURS.toSeconds(1);
    public static final String DEFAULT_ALLOW_METHODS = "GET, OPTIONS";

    public static final String ORIGIN = "Origin";

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    private HttpRequest request;
    private ResponseBuilder response;
    private Set<String> allowedOrigins;
    private String[] allowedMethods;

    private boolean preflight;
    private boolean auth;

    public Cors(HttpRequest request, ResponseBuilder response) {
        this.request = request;
        this.response = response;
    }

    public static Cors add(HttpRequest request, ResponseBuilder response) {
        return new Cors(request, response);
    }

    public Cors preflight() {
        preflight = true;
        return this;
    }

    public Cors auth() {
        auth = true;
        return this;
    }

    public Cors allowedOrigins(ClientModel client) {
        if (client != null) {
            allowedOrigins = client.getWebOrigins();
        }
        return this;
    }

    public Cors allowedMethods(String... allowedMethods) {
        this.allowedMethods = allowedMethods;
        return this;
    }

    public Response build() {
        String origin = request.getHttpHeaders().getRequestHeaders().getFirst(ORIGIN);
        if (origin == null) {
            return response.build();
        }

        if (!preflight && (allowedOrigins == null || !allowedOrigins.contains(origin))) {
            return response.build();
        }

        response.header(ACCESS_CONTROL_ALLOW_ORIGIN, origin);

        if (allowedMethods != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < allowedMethods.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(allowedMethods[i]);
            }
            response.header(ACCESS_CONTROL_ALLOW_METHODS, sb.toString());
        } else {
            response.header(ACCESS_CONTROL_ALLOW_METHODS, DEFAULT_ALLOW_METHODS);
        }

        response.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.toString(auth));
        if (auth) {
            response.header(ACCESS_CONTROL_ALLOW_HEADERS, "Authorization");
        }

        response.header(ACCESS_CONTROL_MAX_AGE, DEFAULT_MAX_AGE);

        return response.build();
    }

}
