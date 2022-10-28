package org.keycloak.common;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum Feature {
    AUTHORIZATION("Authorization Service", Type.DEFAULT),

    ACCOUNT_API("Account Management REST API", Type.DEFAULT),
    ACCOUNT2("New Account Management Console", Type.DEFAULT, Feature.ACCOUNT_API),

    ADMIN_FINE_GRAINED_AUTHZ("Fine-Grained Admin Permissions", Type.PREVIEW),

    ADMIN_API("Admin API", Type.DEFAULT),

    @Deprecated
    ADMIN("Legacy Admin Console", Type.DEPRECATED),

    ADMIN2("New Admin Console", Type.DEFAULT, Feature.ADMIN_API),

    DOCKER("Docker Registry protocol", Type.DISABLED_BY_DEFAULT),

    IMPERSONATION("Ability for admins to impersonate users", Type.DEFAULT),

    OPENSHIFT_INTEGRATION("Extension to enable securing OpenShift", Type.PREVIEW),

    SCRIPTS("Write custom authenticators using JavaScript", Type.PREVIEW),

    TOKEN_EXCHANGE("Token Exchange Service", Type.PREVIEW),

    WEB_AUTHN("W3C Web Authentication (WebAuthn)", Type.DEFAULT),

    CLIENT_POLICIES("Client configuration policies", Type.DEFAULT),

    CIBA("OpenID Connect Client Initiated Backchannel Authentication (CIBA)", Type.DEFAULT),

    MAP_STORAGE("New store", Type.EXPERIMENTAL),

    PAR("OAuth 2.0 Pushed Authorization Requests (PAR)", Type.DEFAULT),

    DECLARATIVE_USER_PROFILE("Configure user profiles using a declarative style", Type.PREVIEW),

    DYNAMIC_SCOPES("Dynamic OAuth 2.0 scopes", Type.EXPERIMENTAL),

    CLIENT_SECRET_ROTATION("Client Secret Rotation", Type.PREVIEW),

    STEP_UP_AUTHENTICATION("Step-up Authentication", Type.DEFAULT),

    RECOVERY_CODES("Recovery codes", Type.PREVIEW),

    UPDATE_EMAIL("Update Email Action", Type.PREVIEW),

    JS_ADAPTER("Host keycloak.js and keycloak-authz.js through the Keycloak sever", Type.DEFAULT);

    private final Type type;
    private String label;

    private Set<Feature> dependencies;
    Feature(String label, Type type) {
        this.label = label;
        this.type = type;
    }

    Feature(String label, Type type, Feature... dependencies) {
        this.label = label;
        this.type = type;
        this.dependencies = Arrays.stream(dependencies).collect(Collectors.toSet());
    }

    public String getKey() {
        return name().toLowerCase().replaceAll("_", "-");
    }

    public String getLabel() {
        return label;
    }

    public Type getType() {
        return type;
    }

    public Set<Feature> getDependencies() {
        return dependencies;
    }

    public enum Type {
        DEFAULT,
        DISABLED_BY_DEFAULT,
        PREVIEW,
        EXPERIMENTAL,
        DEPRECATED;
    }
}
