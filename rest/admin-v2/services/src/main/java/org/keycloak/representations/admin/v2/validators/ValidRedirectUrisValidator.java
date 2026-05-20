package org.keycloak.representations.admin.v2.validators;

import java.util.Set;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.validation.ValidRedirectUris;

/**
 * Validates redirect URIs according to Keycloak's redirect URI rules.
 * <p>
 * The validation is context-aware: when a root URL is set on the client,
 * relative paths are allowed. Without a root URL, only absolute URIs are valid.
 */
public class ValidRedirectUrisValidator implements ConstraintValidator<ValidRedirectUris, BaseClientRepresentation> {

    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://");

    @Override
    public boolean isValid(BaseClientRepresentation representation, ConstraintValidatorContext context) {
        Set<String> redirectUris = representation.getRedirectUris();
        if (redirectUris == null || redirectUris.isEmpty()) {
            return true;
        }

        boolean hasRootUrl = representation.getAppUrl() != null && !representation.getAppUrl().isBlank();
        boolean allValid = true;

        for (String uri : redirectUris) {
            String error = validateRedirectUri(uri, hasRootUrl);
            if (error != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(error)
                        .addPropertyNode("redirectUris")
                        .addConstraintViolation();
                allValid = false;
            }
        }

        return allValid;
    }

    /**
     * Validates a single redirect URI.
     *
     * @param uri the redirect URI to validate
     * @param hasRootUrl whether the client has a root URL configured
     * @return error message if invalid, null if valid
     */
    public static String validateRedirectUri(String uri, boolean hasRootUrl) {
        if (uri == null || uri.isBlank()) {
            return "Redirect URI cannot be empty";
        }

        String trimmedUri = uri.trim();

        // Special cases that are always valid
        if ("*".equals(trimmedUri) || "+".equals(trimmedUri) || "-".equals(trimmedUri)) {
            return null;
        }

        boolean hasScheme = SCHEME_PATTERN.matcher(trimmedUri).find();

        // Without root URL, only absolute URIs are allowed
        if (!hasRootUrl && !hasScheme) {
            return "Redirect URI must be an absolute URI (include scheme like https://) when Root URL is not set";
        }

        // Validate wildcard rules
        if (trimmedUri.contains("*")) {
            return validateWildcard(trimmedUri);
        }

        return null;
    }

    private static String validateWildcard(String uri) {
        // Wildcard must be at the very end
        if (!uri.endsWith("*")) {
            return "Wildcard (*) must be at the end of the URI";
        }

        // Only one wildcard allowed
        long wildcardCount = uri.chars().filter(ch -> ch == '*').count();
        if (wildcardCount > 1) {
            return "Only one wildcard (*) is allowed at the end of the URI";
        }

        // Wildcard must be preceded by "/" (valid patterns: "/*", "/path/*")
        int wildcardIndex = uri.lastIndexOf('*');
        if (wildcardIndex > 0 && uri.charAt(wildcardIndex - 1) != '/') {
            return "Wildcard (*) must be preceded by a slash (/)";
        }

        // No query parameters with wildcard
        if (uri.contains("?")) {
            return "Wildcard URIs cannot contain query parameters";
        }

        // No fragment with wildcard
        if (uri.contains("#")) {
            return "Wildcard URIs cannot contain fragments";
        }

        return null;
    }

    /**
     * Checks if a redirect URI is valid.
     *
     * @param uri the redirect URI to check
     * @param hasRootUrl whether the client has a root URL configured
     * @return true if valid, false otherwise
     */
    public static boolean isValidRedirectUri(String uri, boolean hasRootUrl) {
        return validateRedirectUri(uri, hasRootUrl) == null;
    }
}
