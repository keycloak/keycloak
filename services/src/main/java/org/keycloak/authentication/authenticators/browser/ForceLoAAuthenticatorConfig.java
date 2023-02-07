package org.keycloak.authentication.authenticators.browser;

import org.keycloak.models.AuthenticatorConfigModel;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Strings.emptyToNull;
import static org.keycloak.models.Constants.NO_LOA;

final class ForceLoAAuthenticatorConfig {

    static final String MIN_LOA = "minLoA";

    private final AuthenticatorConfigModel authenticatorConfigModel;

    ForceLoAAuthenticatorConfig(AuthenticatorConfigModel configModel) {
        this.authenticatorConfigModel = configModel;
    }

    int levelOfAuthentication() {
        return getConfigMap()
            .map(config -> config.getOrDefault(MIN_LOA, String.valueOf(NO_LOA)))
            .map(str -> Optional.ofNullable(emptyToNull(str)).orElse(String.valueOf(NO_LOA)))
            .map(Integer::parseInt)
            .orElse(NO_LOA);
    }

    private Optional<Map<String, String>> getConfigMap() {
        return Optional.ofNullable(authenticatorConfigModel)
            .map(AuthenticatorConfigModel::getConfig);
    }
}
