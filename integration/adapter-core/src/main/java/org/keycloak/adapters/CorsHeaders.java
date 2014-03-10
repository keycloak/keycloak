package org.keycloak.adapters;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface CorsHeaders {
    String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
    String ORIGIN = "Origin";
    String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
}
