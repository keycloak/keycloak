package org.keycloak.authentication.authenticators.browser;

import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;

final class MfaEnrollmentConfig {

    private static final String STRING_PROPERTY_DEFAULT = "";

    static final String REQUIRED_ACTIONS = "requiredActions";
    static final String CREDENTIAL_TYPES = "credentialTypes";

    private final AuthenticatorConfigModel authenticatorConfigModel;

    MfaEnrollmentConfig(AuthenticatorConfigModel authenticatorConfigModel) {
        this.authenticatorConfigModel = authenticatorConfigModel;
    }

    List<String> getRequiredActions() {
        return getList(REQUIRED_ACTIONS);
    }

    public List<String> getCredentialTypes() {
        return getList(CREDENTIAL_TYPES);
    }

    private List<String> getList(String propertyName) {
        List<String> propertyValues = new ArrayList<>(getConfigMap()
                .map(config -> Arrays.asList(Constants.CFG_DELIMITER_PATTERN.split(
                        Optional.ofNullable(
                                config.getOrDefault(propertyName, STRING_PROPERTY_DEFAULT)
                        ).orElse(STRING_PROPERTY_DEFAULT).trim())))
                .orElse(emptyList()));
        propertyValues.remove(STRING_PROPERTY_DEFAULT);
        return propertyValues;
    }

    private Optional<Map<String, String>> getConfigMap() {
        return Optional.ofNullable(authenticatorConfigModel)
                .map(AuthenticatorConfigModel::getConfig);
    }

}
