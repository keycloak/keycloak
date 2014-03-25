package org.keycloak.audit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface Events {

    String LOGIN = "login";
    String REGISTER = "register";
    String LOGOUT = "logout";
    String CODE_TO_TOKEN = "code_to_token";
    String REFRESH_TOKEN = "refresh_token";

    String SOCIAL_LINK = "social_link";
    String REMOVE_SOCIAL_LINK = "remove_social_link";

    String UPDATE_EMAIL = "update_email";
    String UPDATE_PROFILE = "update_profile";
    String UPDATE_PASSWORD = "update_password";
    String UPDATE_TOTP = "update_totp";

    String VERIFY_EMAIL = "verify_email";

    String REMOVE_TOTP = "remove_totp";

    String SEND_VERIFY_EMAIL = "send_verify_email";
    String SEND_RESET_PASSWORD = "send_reset_password";

}
