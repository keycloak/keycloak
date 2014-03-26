package org.keycloak.spi.authentication;

/**
 * Result of authentication by AuthenticationProvider
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public enum AuthProviderStatus {

    SUCCESS, INVALID_CREDENTIALS, USER_NOT_FOUND

}
