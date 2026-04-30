package org.keycloak.protocol.oidc;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * <p>A {@link Spi} support pluggable validators of JWTAuthorizationGrant token
 * The pluggable validators are supposed to be provided for each JWT token type
 *
 * @author <a href="mailto:yutaka.obuchi.sd@hitachi.com">Yutaka Obuchi</a>
 */

public class JWTAuthorizationGrantValidatorSpi implements Spi {

    public static final String SPI_NAME = "jwt-authorization-grant-validator";

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return SPI_NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return JWTAuthorizationGrantValidator.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return JWTAuthorizationGrantValidatorFactory.class;
    }

}
