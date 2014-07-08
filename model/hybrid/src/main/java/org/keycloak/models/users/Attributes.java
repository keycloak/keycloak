package org.keycloak.models.users;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface Attributes {

    String EMAIL_VERIFIED = "keycloak.emailVerified";
    String TOTP_ENABLED = "keycloak.totpEnabled";
    String REQUIRED_ACTIONS = "keycloak.requiredActions";

}
