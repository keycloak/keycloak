package org.keycloak.protocol.oidc.grants;

import java.util.List;

import org.keycloak.protocol.oidc.JWTAuthorizationGrantValidationContext ;


/**
 * Interface for the assertion validator of JWTAuthorizationGrant
 *
 * @author <a href="mailto:yutaka.obuchi.sd@hitachi.com">Yutaka Obuchi</a>
 */

public interface JWTAuthorizationGrantValidator extends JWTAuthorizationGrantValidationContext {

    void validateClient();
    
    void validateIssuer();
    
    void validateSubject();
    
    boolean validateTokenActive(int allowedClockSkew, int maxExp, boolean reusePermitted); 
    
    boolean validateSignatureAlgorithm(String expectedSignatureAlg);
    
    boolean validateTokenAudience(List<String> expectedAudiences, boolean multipleAudienceAllowed);
    
}