package org.keycloak.protocol.oidc.resourceindicators;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;

/**
 * Converts multiple values of the {@code resource} parameter to and from a single string.
 *
 * <p>The values are delimited by a space, which can not be part of a valid resource indicator. A single value is
 * therefore represented by the value itself.
 */
public final class ResourceIndicators {

    private static final String DELIMITER = " ";

    private ResourceIndicators() {
    }

    /**
     * @param paramName the name of a request parameter
     * @return {@code true} if the parameter is the {@code resource} parameter and the Resource Indicators feature
     * processes it. As it restricts the audience of the tokens, a request must be rejected instead of ignoring it.
     */
    public static boolean isProcessedParameter(String paramName) {
        return OAuth2Constants.RESOURCE.equals(paramName) && Profile.isFeatureEnabled(Profile.Feature.RESOURCE_INDICATORS);
    }

    /**
     * @param resources the values of the {@code resource} parameter, may be {@code null}
     * @return the values joined to a single string, {@code null} if there are no values
     */
    public static String join(Collection<String> resources) {
        if (resources == null || resources.isEmpty()) {
            return null;
        }
        // a value containing the delimiter can not be a valid resource indicator. It is replaced by an empty value,
        // which is rejected when validating the values, so that it is not handled as multiple values.
        return resources.stream()
                .map(resource -> resource == null || resource.contains(DELIMITER) ? "" : resource)
                .collect(Collectors.joining(DELIMITER));
    }

    /**
     * @param resources the string created by {@link #join(Collection)}, may be {@code null}
     * @return {@code true} if the parameter was sent with more than one value, also if all the values are the same
     */
    public static boolean isMultivalued(String resources) {
        return resources != null && resources.contains(DELIMITER);
    }

    /**
     * @param resources the string created by {@link #join(Collection)}, may be {@code null}
     * @return the distinct values in their original order, {@code null} if the given string is {@code null}
     */
    public static Set<String> parse(String resources) {
        if (resources == null) {
            return null;
        }
        // empty values are kept so that they are rejected when validating the values
        return new LinkedHashSet<>(Arrays.asList(resources.split(DELIMITER, -1)));
    }

}
