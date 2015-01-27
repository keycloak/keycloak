package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.CollectionUtil;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Cors {
    protected static final Logger logger = Logger.getLogger(Cors.class);

    public static final long DEFAULT_MAX_AGE = TimeUnit.HOURS.toSeconds(1);
    public static final String DEFAULT_ALLOW_METHODS = "GET, POST, HEAD, OPTIONS";
    public static final String DEFAULT_ALLOW_HEADERS = "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers";

    public static final String ORIGIN_HEADER = "Origin";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD = "*";

    private HttpRequest request;
    private ResponseBuilder builder;
    private Set<String> allowedOrigins;
    private Set<String> allowedMethods;
    private Set<String> supportedHeaders;
    private Set<String> exposedHeaders;

    private boolean preflight;
    private boolean auth;

    public Cors(HttpRequest request, ResponseBuilder response) {
        this.request = request;
        this.builder = response;
    }

    public Cors(HttpRequest request) {
        this.request = request;
    }

    public static Cors add(HttpRequest request, ResponseBuilder response) {
        return new Cors(request, response);
    }

    public static Cors add(HttpRequest request) {
        return new Cors(request);
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

    public Cors allowedOrigins(AccessToken token) {
        if (token != null) {
            allowedOrigins = token.getAllowedOrigins();
        }
        return this;
    }

    public Cors allowedOrigins(String... allowedOrigins) {
        if (allowedOrigins != null && allowedOrigins.length > 0) {
            this.allowedOrigins = new HashSet<String>(Arrays.asList(allowedOrigins));
        }
        return this;
    }

    public Cors allowedMethods(String... allowedMethods) {
        this.allowedMethods = new HashSet<String>(Arrays.asList(allowedMethods));
        return this;
    }

    public Cors supportedHeaders(String... supportedHeaders) {
        this.supportedHeaders = new HashSet<String>(Arrays.asList(supportedHeaders));
        return this;
    }

    public Cors exposedHeaders(String... exposedHeaders) {
        this.exposedHeaders = new HashSet<String>(Arrays.asList(exposedHeaders));
        return this;
    }

    public Response build() {
        final String origin = request.getHttpHeaders().getRequestHeaders().getFirst(ORIGIN_HEADER);
        if (origin == null) {
            logger.debug("CORS origin denied");
            throw new BadRequestException("CORS origin denied");
        }

        if (allowedOrigins == null
                || (!allowedOrigins.contains(origin) && !allowedOrigins.contains(ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD))) {
            logger.debug("CORS origin denied");
            throw new BadRequestException("CORS origin denied");
        }

        if (auth) {
            builder.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        builder.header(ACCESS_CONTROL_ALLOW_ORIGIN, origin);

        if (preflight) {

            // Parse requested method
            // Note: method checking must be done after header parsing, see CORS spec
            final String requestMethodHeader = request.getHttpHeaders().getRequestHeaders()
                    .getFirst(ACCESS_CONTROL_REQUEST_METHOD);
            if (requestMethodHeader == null) {
                logger.debug("Invalid preflight CORS request: Missing Access-Control-Request-Method header");
                throw new BadRequestException("Invalid preflight CORS request: Missing Access-Control-Request-Method header");
            }

            final String requestedMethod = requestMethodHeader.toUpperCase();

            // Parse the requested author (custom) headers
            final String rawRequestHeadersString = request.getHttpHeaders().getRequestHeaders()
                    .getFirst(ACCESS_CONTROL_REQUEST_HEADERS);
            final String[] requestHeaderValues = CorsUtil.parseMultipleHeaderValues(rawRequestHeadersString);
            final String[] requestHeaders = new String[requestHeaderValues.length];

            for (int i = 0; i < requestHeaders.length; i++) {
                try {
                    requestHeaders[i] = CorsUtil.formatCanonical(requestHeaderValues[i]);
                } catch (IllegalArgumentException e) {
                    // Invalid header name
                    logger.debug("Invalid preflight CORS request: Bad request header value");
                    throw new BadRequestException("Invalid preflight CORS request: Bad request header value");
                }
            }

            // Now, do method check
            if (!allowedMethods.contains(requestedMethod)) {
                logger.debug("Unsupported HTTP method " + requestedMethod);
                throw new BadRequestException("Unsupported HTTP method " + requestedMethod);
            }

            // Author request headers check
            for (String requestHeader : requestHeaders) {
                if (!supportedHeaders.contains(requestHeader)) {
                    logger.debug("Unsupported HTTP request header " + requestHeader);
                    throw new BadRequestException("Unsupported HTTP request header " + requestHeader);
                }
            }

            // Setting up headers for preflight request
            builder.header(ACCESS_CONTROL_MAX_AGE, DEFAULT_MAX_AGE);

            if (allowedMethods != null) {
                builder.header(ACCESS_CONTROL_ALLOW_METHODS, CollectionUtil.join(allowedMethods));
            } else {
                builder.header(ACCESS_CONTROL_ALLOW_METHODS, DEFAULT_ALLOW_METHODS);
            }

            if (rawRequestHeadersString != null) {
                builder.header(ACCESS_CONTROL_ALLOW_HEADERS, rawRequestHeadersString);
            } else if (supportedHeaders != null && !supportedHeaders.isEmpty()) {
                builder.header(ACCESS_CONTROL_ALLOW_HEADERS, CollectionUtil.join(supportedHeaders));
            }

        } else {
            // request is actual
            // do method check
            final String method = request.getHttpMethod().toUpperCase();
            if (!allowedMethods.contains(method)) {
                logger.debug("Unsupported HTTP request method " + method);
                throw new BadRequestException("Unsupported HTTP request method " + method);
            }

            // Setting up headers for actual request
            if (exposedHeaders != null) {
                builder.header(ACCESS_CONTROL_EXPOSE_HEADERS, CollectionUtil.join(exposedHeaders));
            }
        }

        return builder.build();
    }

    public void build(HttpResponse response) {
        final String origin = request.getHttpHeaders().getRequestHeaders().getFirst(ORIGIN_HEADER);
        if (origin == null) {
            logger.debug("CORS origin denied");
            throw new BadRequestException("CORS origin denied", builder.build());
        }

        if (allowedOrigins == null
                || (!allowedOrigins.contains(origin) && !allowedOrigins.contains(ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD))) {
            logger.debug("CORS origin denied");
            throw new BadRequestException("CORS origin denied", builder.build());
        }

        if (auth) {
            response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, origin);

        if (preflight) {
            // request is preflight
            // Parse requested method
            // Note: method checking must be done after header parsing, see CORS spec
            final String requestMethodHeader = request.getHttpHeaders().getRequestHeaders()
                    .getFirst(ACCESS_CONTROL_REQUEST_METHOD);
            if (requestMethodHeader == null) {
                logger.debug("Invalid preflight CORS request: Missing Access-Control-Request-Method header");
                throw new BadRequestException("Invalid preflight CORS request: Missing Access-Control-Request-Method header");
            }

            final String requestedMethod = requestMethodHeader.toUpperCase();

            // Parse the requested author (custom) headers
            final String rawRequestHeadersString = request.getHttpHeaders().getRequestHeaders()
                    .getFirst(ACCESS_CONTROL_REQUEST_HEADERS);
            final String[] requestHeaderValues = CorsUtil.parseMultipleHeaderValues(rawRequestHeadersString);
            final String[] requestHeaders = new String[requestHeaderValues.length];

            for (int i = 0; i < requestHeaders.length; i++) {
                try {
                    requestHeaders[i] = CorsUtil.formatCanonical(requestHeaderValues[i]);
                } catch (IllegalArgumentException e) {
                    // Invalid header name
                    logger.debug("Invalid preflight CORS request: Bad request header value");
                    throw new BadRequestException("Invalid preflight CORS request: Bad request header value");
                }
            }

            // Now, do method check
            if (!allowedMethods.contains(requestedMethod)) {
                logger.debug("Unsupported HTTP method " + requestedMethod);
                throw new BadRequestException("Unsupported HTTP method " + requestedMethod);
            }

            // Author request headers check
            for (String requestHeader : requestHeaders) {
                if (!supportedHeaders.contains(requestHeader)) {
                    logger.debug("Unsupported HTTP request header " + requestHeader);
                    throw new BadRequestException("Unsupported HTTP request header " + requestHeader);
                }
            }

            // Setting up headers for preflight request
            response.getOutputHeaders().add(ACCESS_CONTROL_MAX_AGE, DEFAULT_MAX_AGE);

            if (allowedMethods != null) {
                response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_METHODS, CollectionUtil.join(allowedMethods));
            } else {
                response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_METHODS, DEFAULT_ALLOW_METHODS);
            }

            if (rawRequestHeadersString != null) {
                response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, rawRequestHeadersString);
            } else if (supportedHeaders != null && !supportedHeaders.isEmpty()) {
                response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, CollectionUtil.join(supportedHeaders));
            }

        } else {
            // request is actual
            // do method check
            final String method = request.getHttpMethod().toUpperCase();
            if (!allowedMethods.contains(method)) {
                logger.debug("Unsupported HTTP request method " + method);
                throw new BadRequestException("Unsupported HTTP request method " + method);
            }

            // Setting up headers for actual request
            if (exposedHeaders != null) {
                response.getOutputHeaders().add(ACCESS_CONTROL_EXPOSE_HEADERS, CollectionUtil.join(exposedHeaders));
            }
        }
    }

}
