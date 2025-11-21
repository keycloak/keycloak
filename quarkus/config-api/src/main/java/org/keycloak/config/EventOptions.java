package org.keycloak.config;

import java.util.List;

public class EventOptions {

    public static final Option<Boolean> USER_EVENT_METRICS_ENABLED = new OptionBuilder<>("event-metrics-user-enabled", Boolean.class)
            .category(OptionCategory.EVENTS)
            .description("Create metrics based on user events.")
            .buildTime(true)
            .defaultValue(Boolean.FALSE)
            .build();

    public static final Option<List<String>> USER_EVENT_METRICS_TAGS = OptionBuilder.listOptionBuilder("event-metrics-user-tags", String.class)
            .category(OptionCategory.EVENTS)
            .description("Comma-separated list of tags to be collected for user event metrics. By default only 'realm' is enabled to avoid a high metrics cardinality.")
            .buildTime(false)
            .expectedValues(List.of("realm", "idp", "clientId"))
            .defaultValue(List.of("realm"))
            .build();

    public static final Option<List<String>> USER_EVENT_METRICS_EVENTS = OptionBuilder.listOptionBuilder("event-metrics-user-events", String.class)
            .category(OptionCategory.EVENTS)
            .description("Comma-separated list of events to be collected for user event metrics. This option can be used to reduce the number of metrics created as by default all user events create a metric.")
            .buildTime(false)
            .expectedValues(sortedListOfEvents())
            .deprecatedMetadata(DeprecatedMetadata.deprecateValues("Use `remove_credential` instead of `remove_totp`, and `update_credential` instead of `update_totp` and `update_password`.", "remove_totp", "update_totp", "update_password"))
            .build();

    private static List<String> sortedListOfEvents() {
        List<String> events = new java.util.ArrayList<>(List.of("register", "login", "code_to_token", "logout", "client_login", "refresh_token", "introspect_token", "federated_identity_link",
                "remove_federated_identity", "update_email", "update_profile",
                "verify_email", "verify_profile", "grant_consent", "update_consent", "revoke_grant", "send_verify_email", "send_reset_password", "send_identity_provider_link",
                "reset_password", "restart_authentication", "invalid_signature", "register_node", "unregister_node", "user_info_request", "identity_provider_link_account", "identity_provider_login",
                "identity_provider_first_login", "identity_provider_post_login", "identity_provider_response", "identity_provider_retrieve_token", "impersonate", "custom_required_action",
                "execute_actions", "execute_action_token", "client_info", "client_register", "client_update", "client_delete", "client_initiated_account_linking", "token_exchange",
                "oauth2_device_auth", "oauth2_device_verify_user_code", "oauth2_device_code_to_token", "authreqid_to_token", "permission_token", "delete_account", "pushed_authorization_request",
                "user_disabled_by_permanent_lockout", "user_disabled_by_temporary_lockout", "oauth2_extension_grant", "federated_identity_override_link", "update_credential", "remove_credential",
                "invite_org", "remove_totp", "update_totp", "update_password", "user_session_deleted"));
        events.sort(String::compareToIgnoreCase);
        return events;
    }

}
