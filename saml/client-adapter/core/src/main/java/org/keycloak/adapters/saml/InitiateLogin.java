package org.keycloak.adapters.saml;

import org.jboss.logging.Logger;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2AuthnRequestBuilder;
import org.keycloak.saml.SAML2NameIDPolicyBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.w3c.dom.Document;

import java.security.KeyPair;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class InitiateLogin implements AuthChallenge {
    protected static Logger log = Logger.getLogger(InitiateLogin.class);

    protected SamlDeployment deployment;
    protected SamlSessionStore sessionStore;

    public InitiateLogin(SamlDeployment deployment, SamlSessionStore sessionStore) {
        this.deployment = deployment;
        this.sessionStore = sessionStore;
    }

    @Override
    public int getResponseCode() {
        return 0;
    }

    @Override
    public boolean challenge(HttpFacade httpFacade) {
        try {
            String issuerURL = deployment.getEntityID();
            String actionUrl = deployment.getIDP().getSingleSignOnService().getRequestBindingUrl();
            String destinationUrl = actionUrl;
            String nameIDPolicyFormat = deployment.getNameIDPolicyFormat();

            if (nameIDPolicyFormat == null) {
                nameIDPolicyFormat =  JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get();
            }



            SAML2AuthnRequestBuilder authnRequestBuilder = new SAML2AuthnRequestBuilder()
                    .destination(destinationUrl)
                    .issuer(issuerURL)
                    .forceAuthn(deployment.isForceAuthentication()).isPassive(deployment.isIsPassive())
                    .nameIdPolicy(SAML2NameIDPolicyBuilder.format(nameIDPolicyFormat));
            if (deployment.getIDP().getSingleSignOnService().getResponseBinding() != null) {
                String protocolBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get();
                if (deployment.getIDP().getSingleSignOnService().getResponseBinding() == SamlDeployment.Binding.POST) {
                    protocolBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get();
                }
                authnRequestBuilder.protocolBinding(protocolBinding);

            }
            if (deployment.getAssertionConsumerServiceUrl() != null) {
                authnRequestBuilder.assertionConsumerUrl(deployment.getAssertionConsumerServiceUrl());
            }
            BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();

            if (deployment.getIDP().getSingleSignOnService().signRequest()) {


                KeyPair keypair = deployment.getSigningKeyPair();
                if (keypair == null) {
                    throw new RuntimeException("Signing keys not configured");
                }
                if (deployment.getSignatureCanonicalizationMethod() != null) {
                    binding.canonicalizationMethod(deployment.getSignatureCanonicalizationMethod());
                }

                binding.signWith(keypair);
                binding.signDocument();
            }
            sessionStore.saveRequest();

            Document document = authnRequestBuilder.toDocument();
            SamlDeployment.Binding samlBinding = deployment.getIDP().getSingleSignOnService().getRequestBinding();
            SamlUtil.sendSaml(true, httpFacade, actionUrl, binding, document, samlBinding);
            sessionStore.setCurrentAction(SamlSessionStore.CurrentAction.LOGGING_IN);
        } catch (Exception e) {
            throw new RuntimeException("Could not create authentication request.", e);
        }
        return true;
    }

}
