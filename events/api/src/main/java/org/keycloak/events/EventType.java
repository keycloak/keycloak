package org.keycloak.events;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public enum EventType {

    LOGIN(true),
    LOGIN_ERROR(true),
    REGISTER(true),
    REGISTER_ERROR(true),
    LOGOUT(true),
    LOGOUT_ERROR(true),

    CODE_TO_TOKEN(true),
    CODE_TO_TOKEN_ERROR(true),

    REFRESH_TOKEN(false),
    REFRESH_TOKEN_ERROR(false),
    VALIDATE_ACCESS_TOKEN(false),
    VALIDATE_ACCESS_TOKEN_ERROR(false),

    FEDERATED_IDENTITY_LINK(true),
    FEDERATED_IDENTITY_LINK_ERROR(true),
    REMOVE_FEDERATED_IDENTITY(true),
    REMOVE_FEDERATED_IDENTITY_ERROR(true),

    UPDATE_EMAIL(true),
    UPDATE_EMAIL_ERROR(true),
    UPDATE_PROFILE(true),
    UPDATE_PROFILE_ERROR(true),
    UPDATE_PASSWORD(true),
    UPDATE_PASSWORD_ERROR(true),
    UPDATE_TOTP(true),
    UPDATE_TOTP_ERROR(true),
    VERIFY_EMAIL(true),
    VERIFY_EMAIL_ERROR(true),

    REMOVE_TOTP(true),
    REMOVE_TOTP_ERROR(true),

    SEND_VERIFY_EMAIL(true),
    SEND_VERIFY_EMAIL_ERROR(true),
    SEND_RESET_PASSWORD(true),
    SEND_RESET_PASSWORD_ERROR(true),
    RESET_PASSWORD(true),
    RESET_PASSWORD_ERROR(true),

    INVALID_SIGNATURE_ERROR(false),
    REGISTER_NODE(false),
    UNREGISTER_NODE(false),

    USER_INFO_REQUEST(false),
    USER_INFO_REQUEST_ERROR(false),

    IDENTITY_PROVIDER_LOGIN(false),
    IDENTITY_PROVIDER_LOGIN_ERROR(false),
    IDENTITY_PROVIDER_RESPONSE(false),
    IDENTITY_PROVIDER_RESPONSE_ERROR(false),
    IDENTITY_PROVIDER_RETRIEVE_TOKEN(false),
    IDENTITY_PROVIDER_RETRIEVE_TOKEN_ERROR(false),
    IDENTITY_PROVIDER_ACCCOUNT_LINKING(false),
    IDENTITY_PROVIDER_ACCCOUNT_LINKING_ERROR(false),
    
    VIEW_REALM(false),
    CREATE_REALM(false),
    UPDATE_REALM(false),
    DELETE_REALM(false),

    VIEW_APPLICATIONS(false),
    VIEW_APPLICATION(false),
    CREATE_APPLICATION(false),
    UPDATE_APPLICATION(false),
    DELETE_APPLICATION(false),
    
    VIEW_APPLICATION_USER_SESSIONS(false),
    LOGOUT_APPLICATION_USERS(false),
    LOGOUT_USER(false),

    REGISTER_APPLICATION_CLUSTER_NODE(false),
    UNREGISTER_APPLICATION_CLUSTER_NODE(false),
    
    VIEW_CLIENT_CERTIFICATE(false),
    UPDATE_CLIENT_CERTIFICATE(false),

    VIEW_IDENTITY_PROVIDERS(false),
    VIEW_IDENTITY_PROVIDER(false),
    CREATE_IDENTITY_PROVIDER(false),
    UPDATE_IDENTITY_PROVIDER(false),
    DELETE_IDENTITY_PROVIDER(false),

    VIEW_OAUTH_CLIENTS(false),
    VIEW_OAUTH_CLIENT(false),
    CREATE_OAUTH_CLIENT(false),
    UPDATE_OAUTH_CLIENT(false),
    DELETE_OAUTH_CLIENT(false),

    VIEW_ROLES(false),
    VIEW_ROLE(false),
    CREATE_ROLE(false),
    UPDATE_ROLE(false),
    DELETE_ROLE(false),

    VIEW_USERS(false),
    VIEW_USER(false),
    CREATE_USER(false),
    UPDATE_USER(false),
    DELETE_USER(false),

    VIEW_USER_SESSIONS(false),
    LOGOUT_USER_SESSIONS(false),

    VIEW_FEDERATION_PROVIDERS(false),
    VIEW_FEDERATION_PROVIDER(false),
    CREATE_FEDERATION_PROVIDER(false),
    UPDATE_FEDERATION_PROVIDER(false),
    DELETE_FEDERATION_PROVIDER(false);

    private boolean saveByDefault;

    EventType(boolean saveByDefault) {
        this.saveByDefault = saveByDefault;
    }

    public boolean isSaveByDefault() {
        return saveByDefault;
    }

}
