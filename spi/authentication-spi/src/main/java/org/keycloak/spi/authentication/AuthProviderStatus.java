package org.keycloak.spi.authentication;

/**
 * Result of authentication by AuthenticationProvider
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public enum AuthProviderStatus {

    // Ignore means that AuthenticationProvider wasn't able to authenticate result, but it should postpone authentication to next provider (for example user didn't exists in realm)
    SUCCESS, FAILED, IGNORE
}
