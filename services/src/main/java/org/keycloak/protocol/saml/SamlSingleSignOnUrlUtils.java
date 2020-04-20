package org.keycloak.protocol.saml;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.Optional.ofNullable;

public class SamlSingleSignOnUrlUtils {

    private static final String LOGIN_HINT_PARAM = "login_hint";
    private static final Set<String> SAML_LOGIN_HINT_QUERY_PARAMETER_NAMES = new HashSet<>(Arrays.asList("login_hint", "username"));
    private static final String LOGIN_HINT_INVALID_CHARACTERS = "![ -~]";

    public static String createSingleSignOnServiceUrl(SAMLIdentityProviderConfig configuration, MultivaluedMap<String, String> urlQueryParameters) {
        if (configuration == null) {
            throw new IllegalArgumentException("An instance of SAMLIdentityProviderConfig must be provided");
        }

        final UriBuilder uriBuilder = UriBuilder.fromUri(configuration.getSingleSignOnServiceUrl());
        if (configuration.isLoginHint()) {
            ofNullable(urlQueryParameters)
                    .map(queryParameters -> queryParameters.getFirst(LOGIN_HINT_PARAM))
                    .map(String::trim)
                    .map(SamlSingleSignOnUrlUtils::sanitizeLoginHint)
                    .filter(hint -> !hint.isEmpty())
                    .ifPresent(hint -> SAML_LOGIN_HINT_QUERY_PARAMETER_NAMES.forEach(parameterName -> uriBuilder.replaceQueryParam(parameterName, hint)));
        }
        return uriBuilder.build().toString();
    }

    private static String sanitizeLoginHint(String loginHint) {
        return loginHint.replaceAll(LOGIN_HINT_INVALID_CHARACTERS, "")
                .replace("?", "");
    }
}
