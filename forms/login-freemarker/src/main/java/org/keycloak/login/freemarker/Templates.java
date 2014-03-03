package org.keycloak.login.freemarker;

import org.keycloak.login.LoginFormsPages;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Templates {

    public static String getTemplate(LoginFormsPages page) {
        switch (page) {
            case LOGIN:
                return "login.ftl";
            case LOGIN_TOTP:
                return "login-totp.ftl";
            case LOGIN_CONFIG_TOTP:
                return "login-config-totp.ftl";
            case LOGIN_VERIFY_EMAIL:
                return "login-verify-email.ftl";
            case OAUTH_GRANT:
                return "login-oauth-grant.ftl";
            case LOGIN_RESET_PASSWORD:
                return "login-reset-password.ftl";
            case LOGIN_UPDATE_PASSWORD:
                return "login-update-password.ftl";
            case REGISTER:
                return "register.ftl";
            case ERROR:
                return "error.ftl";
            case LOGIN_UPDATE_PROFILE:
                return "login-update-profile.ftl";
            case CODE:
                return "code.ftl";
            default:
                throw new IllegalArgumentException();
        }
    }

}
