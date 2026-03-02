package org.keycloak.protocol.oidc;

import org.keycloak.provider.ProviderFactory;

/**
 * Provider interface to support pluggable validators of JWTAuthorizationGrant token.
 * The pluggable validators are supposed to be provided for each JWT token type.
 *
 * @author <a href="mailto:yutaka.obuchi.sd@hitachi.com">Yutaka Obuchi</a>
 */

public interface JWTAuthorizationGrantValidatorFactory extends ProviderFactory<JWTAuthorizationGrantValidator> {

    /**
     * @return usually like 3-letters shortcut of specific grants. It can be useful for example in the tokens when the amount of characters should be limited and hence using full grant name
     * is not ideal. Shortcut should be unique across grants.
     */
    String getShortcut();
    
}
