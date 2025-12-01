package org.keycloak.services.filters;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Priority;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

@Provider
@PreMatching
@Priority(10)
public class InvalidQueryParameterFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(InvalidQueryParameterFilter.class);

    private static final String NUL_CHARACTER = "\u0000";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final Map<String, List<String>> queryParams = requestContext.getUriInfo().getQueryParameters();

        for (final List<String> queryParamValues : queryParams.values()) {
            for (final String queryParamValue : queryParamValues) {
                if (containsAnyNULCharacter(queryParamValue)) {
                    LOGGER.debugf("Request with invalid query parameter value is blocked");
                    throw new BadRequestException("Blocking invalid query parameter value");
                }
            }
        }
    }

    /**
     * Unsafe character values we can safely assume is a bad request:
     * NUL	U+0000	Breaks encoding (esp. UTF-8)
     *
     * @param input the value to check if contains unsafe characters
     * @return true if the input contains at least one of the unsafe characters
     */
    private boolean containsAnyNULCharacter(String input) {
        if (input == null) {
            return false;
        }
        return input.contains(NUL_CHARACTER);
    }

}
