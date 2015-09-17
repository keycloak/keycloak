package org.keycloak.adapters.saml;

import org.keycloak.enums.SslRequired;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface SamlDeployment {
    enum Binding {
        POST,
        REDIRECT
    }

    public boolean isConfigured();
    SslRequired getSslRequired();
    String getSingleSignOnServiceUrl();
    String getSingleLogoutServiceUrl();
    String getIssuer();
    String getNameIDPolicyFormat();
    String getAssertionConsumerServiceUrl();
    Binding getRequestBinding();
    Binding getResponseBinding();
    KeyPair getSigningKeyPair();
    String getSignatureCanonicalizationMethod();
    boolean isForceAuthentication();
    boolean isRequestsSigned();

    boolean isValidateSignatures();
    PublicKey getSignatureValidationKey();
    PrivateKey getAssertionDecryptionKey();

    Set<String> getRoleAttributeNames();
    Set<String> getRoleAttributeFriendlyNames();

    enum PrincipalNamePolicy {
        FROM_NAME_ID,
        FROM_ATTRIBUTE_NAME,
        FROM_FRIENDLY_ATTRIBUTE_NAME
    }
    PrincipalNamePolicy getPrincipalNamePolicy();
    String getPrincipalAttributeName();


}
