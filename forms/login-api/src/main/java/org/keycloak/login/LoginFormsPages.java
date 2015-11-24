package org.keycloak.login;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public enum LoginFormsPages {

    LOGIN, LOGIN_TOTP, LOGIN_CONFIG_TOTP, LOGIN_VERIFY_EMAIL,
    LOGIN_IDP_LINK_CONFIRM, LOGIN_IDP_LINK_EMAIL,
    OAUTH_GRANT, LOGIN_RESET_PASSWORD, LOGIN_UPDATE_PASSWORD, REGISTER, INFO, ERROR, LOGIN_UPDATE_PROFILE, CODE;

}
