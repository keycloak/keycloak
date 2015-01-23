package org.keycloak.services.cors;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.cors.exceptions.CORSOriginDeniedException;
import org.keycloak.services.cors.exceptions.InvalidCORSRequestException;
import org.keycloak.services.cors.exceptions.UnsupportedHTTPHeaderException;
import org.keycloak.services.cors.exceptions.UnsupportedHTTPMethodException;
import org.keycloak.util.CollectionUtil;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
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
    private Set<String> supportedMethods;
    private Set<String> supportedHeaders;
    private Set<String> exposedHeaders;

    private boolean preflight;
    private boolean allowAnyOrigin;
    private boolean supportAnyHeader;
    private boolean supportsCredentials;

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

    public Cors allowAnyOrigin() {
        allowAnyOrigin = true;
        return this;
    }

    public Cors supportAnyHeader() {
        supportAnyHeader = true;
        return this;
    }

    public Cors supportsCredentials() {
        supportsCredentials = true;
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
        this.supportedMethods = new HashSet<String>(Arrays.asList(allowedMethods));
        return this;
    }

    public Cors supportedHeaders(String... allowedHeaders) {
        this.supportedHeaders = new HashSet<String>(Arrays.asList(allowedHeaders));
        return this;
    }

    public Cors exposedHeaders(String... exposedHeaders) {
        this.exposedHeaders = new HashSet<String>(Arrays.asList(exposedHeaders));
        return this;
    }

    public Response build() throws CORSOriginDeniedException, UnsupportedHTTPMethodException, InvalidCORSRequestException,
            UnsupportedHTTPHeaderException {
        if (preflight) {
            return buildPreflightCorsResponse();
        } else {
            return buildActualCorsResponse();
        }
    }

    private Response buildActualCorsResponse() throws CORSOriginDeniedException, UnsupportedHTTPMethodException {

        final String requestOrigin = request.getHttpHeaders().getRequestHeaders().getFirst(ORIGIN_HEADER);

        if (requestOrigin == null) {
            logger.debug("CORS origin denied");
            throw new CORSOriginDeniedException("CORS origin denied", requestOrigin);
        }

        if (allowedOrigins == null
                || (!allowedOrigins.contains(requestOrigin) && !allowedOrigins.contains(ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD) && !allowAnyOrigin)) {
            logger.debug("CORS origin denied");
            throw new CORSOriginDeniedException("CORS origin denied", requestOrigin);
        }

        final String method = request.getHttpMethod().toUpperCase();
        if (!supportedMethods.contains(method)) {
            logger.debug("Unsupported HTTP method");
            throw new UnsupportedHTTPMethodException("Unsupported HTTP method", method);
        }

        if (supportsCredentials) {
            builder.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            builder.header(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
        } else {
            if (allowAnyOrigin) {
                builder.header(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD);
            } else {
                builder.header(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
            }
        }

        if (exposedHeaders != null) {
            builder.header(ACCESS_CONTROL_EXPOSE_HEADERS, CollectionUtil.join(exposedHeaders));
        }

        return builder.build();
    }

    private Response buildPreflightCorsResponse() throws CORSOriginDeniedException, InvalidCORSRequestException,
            UnsupportedHTTPHeaderException, UnsupportedHTTPMethodException {

        final String requestOrigin = request.getHttpHeaders().getRequestHeaders().getFirst(ORIGIN_HEADER);

        if (requestOrigin == null) {
            logger.debug("CORS origin denied");
            throw new CORSOriginDeniedException("CORS origin denied", requestOrigin);
        }

        if (allowedOrigins == null
                || (!allowedOrigins.contains(requestOrigin) && !allowedOrigins.contains(ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD) && !allowAnyOrigin)) {
            logger.debug("CORS origin denied");
            throw new CORSOriginDeniedException("CORS origin denied", requestOrigin);
        }

        final String requestMethodHeader = request.getHttpHeaders().getRequestHeaders().getFirst(ACCESS_CONTROL_REQUEST_METHOD);
        if (requestMethodHeader == null) {
            logger.debug("Invalid preflight CORS request: Missing Access-Control-Request-Method header");
            throw new InvalidCORSRequestException(
                    "Invalid preflight CORS request: Missing Access-Control-Request-Method header");
        }

        String requestedMethod = requestMethodHeader.toUpperCase();

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
                throw new InvalidCORSRequestException("Invalid preflight CORS request: Bad request header value");
            }
        }

        if (!supportedMethods.contains(requestedMethod)) {
            logger.debug("Unsupported HTTP method");
            throw new UnsupportedHTTPMethodException("Unsupported HTTP method", requestedMethod);
        }

        // Author request headers check
        if (!supportAnyHeader) {
            for (String requestHeader : requestHeaders) {
                if (!supportedHeaders.contains(requestHeader)) {
                    logger.debug("Unsupported HTTP request header");
                    throw new UnsupportedHTTPHeaderException("Unsupported HTTP request header", requestHeader);
                }
            }
        }

        if (supportsCredentials) {
            builder.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            builder.header(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
        } else {
            if (allowAnyOrigin) {
                builder.header(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD);
            } else {
                builder.header(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
            }
        }

        builder.header(ACCESS_CONTROL_MAX_AGE, DEFAULT_MAX_AGE);

        if (supportedMethods != null) {
            builder.header(ACCESS_CONTROL_ALLOW_METHODS, CollectionUtil.join(supportedMethods));
        } else {
            builder.header(ACCESS_CONTROL_ALLOW_METHODS, DEFAULT_ALLOW_METHODS);
        }

        if (supportAnyHeader && rawRequestHeadersString != null) {
            builder.header(ACCESS_CONTROL_ALLOW_HEADERS, rawRequestHeadersString);
        } else if (supportedHeaders != null && !supportedHeaders.isEmpty()) {
            builder.header(ACCESS_CONTROL_ALLOW_HEADERS, CollectionUtil.join(supportedHeaders));
        }

        return builder.build();
    }

    public void build(HttpResponse response) throws CORSOriginDeniedException, UnsupportedHTTPMethodException,
            InvalidCORSRequestException, UnsupportedHTTPHeaderException {
        if (preflight) {
            buildPreflightCorsResponse(response);
        } else {
            buildActualCorsResponse(response);
        }
    }

    private void buildActualCorsResponse(HttpResponse response) throws CORSOriginDeniedException,
            UnsupportedHTTPMethodException {

        final String requestOrigin = request.getHttpHeaders().getRequestHeaders().getFirst(ORIGIN_HEADER);

        if (requestOrigin == null) {
            logger.debug("CORS origin denied");
            throw new CORSOriginDeniedException("CORS origin denied", requestOrigin);
        }

        if (allowedOrigins == null
                || (!allowedOrigins.contains(requestOrigin) && !allowedOrigins.contains(ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD) && !allowAnyOrigin)) {
            logger.debug("CORS origin denied");
            throw new CORSOriginDeniedException("CORS origin denied", requestOrigin);
        }

        final String method = request.getHttpMethod().toUpperCase();
        if (!supportedMethods.contains(method)) {
            logger.debug("Unsupported HTTP method");
            throw new UnsupportedHTTPMethodException("Unsupported HTTP method", method);
        }

        if (supportsCredentials) {
            response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
        } else {
            if (allowAnyOrigin) {
                response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD);
            } else {
                response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
            }
        }

        if (exposedHeaders != null) {
            response.getOutputHeaders().add(ACCESS_CONTROL_EXPOSE_HEADERS, CollectionUtil.join(exposedHeaders));
        }
    }

    private void buildPreflightCorsResponse(HttpResponse response) throws CORSOriginDeniedException,
            InvalidCORSRequestException, UnsupportedHTTPHeaderException, UnsupportedHTTPMethodException {

        final String requestOrigin = request.getHttpHeaders().getRequestHeaders().getFirst(ORIGIN_HEADER);

        if (requestOrigin == null) {
            logger.debug("CORS origin denied");
            throw new CORSOriginDeniedException("CORS origin denied", requestOrigin);
        }

        if (allowedOrigins == null
                || (!allowedOrigins.contains(requestOrigin) && !allowedOrigins.contains(ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD) && !allowAnyOrigin)) {
            logger.debug("CORS origin denied");
            throw new CORSOriginDeniedException("CORS origin denied", requestOrigin);
        }

        final String requestMethodHeader = request.getHttpHeaders().getRequestHeaders().getFirst(ACCESS_CONTROL_REQUEST_METHOD);
        if (requestMethodHeader == null) {
            logger.debug("Invalid preflight CORS request: Missing Access-Control-Request-Method header");
            throw new InvalidCORSRequestException(
                    "Invalid preflight CORS request: Missing Access-Control-Request-Method header");
        }

        String requestedMethod = requestMethodHeader.toUpperCase();

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
                throw new InvalidCORSRequestException("Invalid preflight CORS request: Bad request header value");
            }
        }

        if (!supportedMethods.contains(requestedMethod)) {
            logger.debug("Unsupported HTTP method");
            throw new UnsupportedHTTPMethodException("Unsupported HTTP method", requestedMethod);
        }

        // Author request headers check
        if (!supportAnyHeader) {
            for (String requestHeader : requestHeaders) {
                if (!supportedHeaders.contains(requestHeader)) {
                    logger.debug("Unsupported HTTP request header");
                    throw new UnsupportedHTTPHeaderException("Unsupported HTTP request header", requestHeader);
                }
            }
        }

        if (supportsCredentials) {
            response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
        } else {
            if (allowAnyOrigin) {
                response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD);
            } else {
                response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
            }
        }

        response.getOutputHeaders().add(ACCESS_CONTROL_MAX_AGE, DEFAULT_MAX_AGE);

        if (supportedMethods != null) {
            response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_METHODS, CollectionUtil.join(supportedMethods));
        } else {
            response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_METHODS, DEFAULT_ALLOW_METHODS);
        }

        if (supportAnyHeader && rawRequestHeadersString != null) {
            response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, rawRequestHeadersString);
        } else if (supportedHeaders != null && !supportedHeaders.isEmpty()) {
            response.getOutputHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, CollectionUtil.join(supportedHeaders));
        }
    }

}
