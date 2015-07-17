package org.keycloak.events;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface Details {

    String EMAIL = "email";
    String PREVIOUS_EMAIL = "previous_email";
    String UPDATED_EMAIL = "updated_email";
    String CODE_ID = "code_id";
    String REDIRECT_URI = "redirect_uri";
    String RESPONSE_TYPE = "response_type";
    String AUTH_TYPE = "auth_type";
    String AUTH_METHOD = "auth_method";
    String IDENTITY_PROVIDER = "identity_provider";
    String IDENTITY_PROVIDER_USERNAME = "identity_provider_identity";
    String REGISTER_METHOD = "register_method";
    String USERNAME = "username";
    String REMEMBER_ME = "remember_me";
    String TOKEN_ID = "token_id";
    String REFRESH_TOKEN_ID = "refresh_token_id";
    String VALIDATE_ACCESS_TOKEN = "validate_access_token";
    String UPDATED_REFRESH_TOKEN_ID = "updated_refresh_token_id";
    String NODE_HOST = "node_host";
    String REASON = "reason";
    String REVOKED_CLIENT = "revoked_client";
    String CLIENT_SESSION_STATE = "client_session_state";
    String CLIENT_SESSION_HOST = "client_session_host";

    String CONSENT = "consent";
    String CONSENT_VALUE_NO_CONSENT_REQUIRED = "no_consent_required"; // No consent is required by client
    String CONSENT_VALUE_CONSENT_GRANTED = "consent_granted";         // Consent granted by user
    String CONSENT_VALUE_PERSISTED_CONSENT = "persistent_consent";    // Persistent consent used (was already granted by user before)
    String IMPERSONATOR_REALM = "impersonator_realm";
    String IMPERSONATOR = "impersonator";

}
