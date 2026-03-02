package org.keycloak.protocol.oidc;

import java.util.List;

import org.keycloak.provider.Provider;

/**
 * Provider interface to support pluggable validators of JWTAuthorizationGrant token.
 * The pluggable validators are supposed to be provided for each JWT token type  
 *
 * @author <a href="mailto:yutaka.obuchi.sd@hitachi.com">Yutaka Obuchi</a>
 */

public interface JWTAuthorizationGrantValidator extends Provider, JWTAuthorizationGrantValidationContext{

    public void validateClient();
    
    public void validateIssuer();
    
    public void validateSubject();
    
    public boolean validateTokenActive(int allowedClockSkew, int maxExp, boolean reusePermitted); 
    
    public boolean validateSignatureAlgorithm(String expectedSignatureAlg);
    
    public boolean validateTokenAudience(List<String> expectedAudiences, boolean multipleAudienceAllowed);
    
}