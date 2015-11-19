package org.keycloak.adapters.saml;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.saml.SignatureAlgorithm;

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
        REDIRECT;

        public static Binding parseBinding(String val) {
            if (val == null) return POST;
            return Binding.valueOf(val);
        }
    }

    public interface IDP {
        String getEntityID();

        SingleSignOnService getSingleSignOnService();
        SingleLogoutService getSingleLogoutService();
        PublicKey getSignatureValidationKey();

        public interface SingleSignOnService {
            boolean signRequest();
            boolean validateResponseSignature();
            Binding getRequestBinding();
            Binding getResponseBinding();
            String getRequestBindingUrl();
        }
        public interface SingleLogoutService {
            boolean validateRequestSignature();
            boolean validateResponseSignature();
            boolean signRequest();
            boolean signResponse();
            Binding getRequestBinding();
            Binding getResponseBinding();
            String getRequestBindingUrl();
            String getResponseBindingUrl();
        }
    }

    public IDP getIDP();

    public boolean isConfigured();
    SslRequired getSslRequired();
    String getEntityID();
    String getNameIDPolicyFormat();
    boolean isForceAuthentication();
    boolean isIsPassive();
    PrivateKey getDecryptionKey();
    KeyPair getSigningKeyPair();
    String getSignatureCanonicalizationMethod();
    SignatureAlgorithm getSignatureAlgorithm();
    String getAssertionConsumerServiceUrl();
    String getLogoutPage();

    Set<String> getRoleAttributeNames();

    enum PrincipalNamePolicy {
        FROM_NAME_ID,
        FROM_ATTRIBUTE
    }
    PrincipalNamePolicy getPrincipalNamePolicy();
    String getPrincipalAttributeName();


}
