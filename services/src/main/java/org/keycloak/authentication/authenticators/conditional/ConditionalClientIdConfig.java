package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;

final class ConditionalClientIdConfig {

    static final String CONDITIONAL_CLIENT_IDS = "clientIds";
    static final String CONF_NEGATE = "negate";

    private final AuthenticatorConfigModel authenticatorConfigModel;

    ConditionalClientIdConfig(AuthenticatorConfigModel configModel) {
        this.authenticatorConfigModel = configModel;
    }

    List<String> getClientIds() {
        List<String> clientIds = new ArrayList<>(getConfigMap()
            .map(config -> Arrays.asList(Constants.CFG_DELIMITER_PATTERN.split(
                Optional.ofNullable(
                    config.getOrDefault(ConditionalClientIdConfig.CONDITIONAL_CLIENT_IDS, "")
                ).orElse("").trim())))
            .orElse(emptyList()));
        clientIds.remove("");
        return clientIds;
    }

    boolean isNegateOutput() {
        return getConfigMap()
            .map(config -> Boolean.parseBoolean(
                config.getOrDefault(ConditionalClientIdConfig.CONF_NEGATE, Boolean.FALSE.toString())))
            .orElse(false);
    }

    private Optional<Map<String, String>> getConfigMap() {
        return Optional.ofNullable(authenticatorConfigModel)
            .map(AuthenticatorConfigModel::getConfig);
    }
}
