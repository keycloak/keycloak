package org.keycloak.config;

public class BootstrapAdminOptions {
    
    public static final String DEFAULT_TEMP_ADMIN_USERNAME = "temp-admin";
    public static final String DEFAULT_TEMP_ADMIN_SERVICE = DEFAULT_TEMP_ADMIN_USERNAME;
    public static final int DEFAULT_TEMP_ADMIN_EXPIRATION = 120;
    public static final boolean DEFAULT_TEMP_ADMIN_IS_TEMPORARY = true;
    private static final String USED_ONLY_WHEN = " Used only when the master realm is created.";
    private static final String NON_CLI = " Use a non-CLI configuration option for this option if possible.";

    // Since the Configuration object is not available in the WelcomeResource, we'll just pass it through a static
    // handler. It's definitely a suboptimal approach but the alternatives are to either put it in the Profile
    // (which doesn't feel right) or RealmModel (which seems like too much).
    // There have been some other attempts to handle this better (https://github.com/keycloak/keycloak/pull/39123)
    // but they didn't get too much traction.
    private static boolean isTemporaryRuntimeAdmin = true;

    public static final Option<String> PASSWORD = new OptionBuilder<>("bootstrap-admin-password", String.class)
            .category(OptionCategory.BOOTSTRAP_ADMIN)
            .description("Bootstrap admin password." + USED_ONLY_WHEN + NON_CLI)
            .build();

    public static final Option<String> USERNAME = new OptionBuilder<>("bootstrap-admin-username", String.class)
            .category(OptionCategory.BOOTSTRAP_ADMIN)
            .description("Bootstrap admin username." + USED_ONLY_WHEN)
            .defaultValue(DEFAULT_TEMP_ADMIN_USERNAME)
            .build();

    public static final Option<Integer> EXPIRATION = new OptionBuilder<>("bootstrap-admin-expiration", Integer.class)
            .category(OptionCategory.BOOTSTRAP_ADMIN)
            .description("Time in minutes for the bootstrap admin user to expire." + USED_ONLY_WHEN)
            .hidden()
            .build();

    public static final Option<String> CLIENT_ID = new OptionBuilder<>("bootstrap-admin-client-id", String.class)
            .category(OptionCategory.BOOTSTRAP_ADMIN)
            .description("Client id for the bootstrap admin service account." + USED_ONLY_WHEN)
            .defaultValue(DEFAULT_TEMP_ADMIN_SERVICE)
            .build();

    public static final Option<String> CLIENT_SECRET = new OptionBuilder<>("bootstrap-admin-client-secret", String.class)
            .category(OptionCategory.BOOTSTRAP_ADMIN)
            .description("Client secret for the bootstrap admin service account." + USED_ONLY_WHEN + NON_CLI)
            .build();

    public static final Option<Boolean> IS_TEMPORARY = new OptionBuilder<>("bootstrap-admin-temporary", Boolean.class)
            .category(OptionCategory.BOOTSTRAP_ADMIN)
            .description("Indicates whether the admin user or service account is temporary." + USED_ONLY_WHEN)
            .defaultValue(DEFAULT_TEMP_ADMIN_IS_TEMPORARY)
            .build();

    public static synchronized void setTemporaryRuntimeAdmin(boolean temporary) {
        isTemporaryRuntimeAdmin = temporary;
    }

    public static synchronized boolean isTemporaryRuntimeAdmin() {
        return isTemporaryRuntimeAdmin;
    }


}
