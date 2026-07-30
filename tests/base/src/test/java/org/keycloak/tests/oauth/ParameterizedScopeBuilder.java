package org.keycloak.tests.oauth;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testframework.realm.ClientScopeBuilder;

public class ParameterizedScopeBuilder {

    private final ClientScopeBuilder builder;

    private ParameterizedScopeBuilder(String name) {
        this.builder = ClientScopeBuilder.create()
                .name(name)
                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                .attribute(ClientScopeModel.IS_PARAMETERIZED_SCOPE, Boolean.TRUE.toString());
    }

    public static ParameterizedScopeBuilder create(String name) {
        return new ParameterizedScopeBuilder(name);
    }

    public ParameterizedScopeBuilder parameterizedScopeType(String type) {
        builder.attribute(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE, type);
        return this;
    }

    public ParameterizedScopeBuilder isRepeatableScope(boolean repeatable) {
        builder.attribute(ClientScopeModel.IS_REPEATABLE_SCOPE, Boolean.toString(repeatable));
        return this;
    }

    public ParameterizedScopeBuilder displayOnConsentScreen(boolean display) {
        builder.attribute(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, Boolean.toString(display));
        return this;
    }

    public ParameterizedScopeBuilder alwaysConsent(boolean alwaysConsent) {
        builder.attribute(ClientScopeModel.IS_ALWAYS_CONSENT, Boolean.toString(alwaysConsent));
        return this;
    }

    public ParameterizedScopeBuilder consentScreenText(String text) {
        builder.attribute(ClientScopeModel.CONSENT_SCREEN_TEXT, text);
        return this;
    }

    public ParameterizedScopeBuilder allowUserDataAccess(boolean allow) {
        builder.attribute(ClientScopeModel.ALLOW_USER_DATA_ACCESS, Boolean.toString(allow));
        return this;
    }

    public ParameterizedScopeBuilder regexp(String regexp) {
        builder.attribute(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP, regexp);
        return this;
    }

    public ClientScopeRepresentation build() {
        return builder.build();
    }
}
