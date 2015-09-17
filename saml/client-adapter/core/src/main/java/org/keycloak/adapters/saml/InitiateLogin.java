package org.keycloak.adapters.saml;

import org.jboss.logging.Logger;
import org.keycloak.adapters.AuthChallenge;
import org.keycloak.adapters.HttpFacade;
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
    public boolean errorPage() {
        return true;
    }

    @Override
    public boolean challenge(HttpFacade httpFacade) {
        try {
            String issuerURL = deployment.getIssuer();
            String actionUrl = deployment.getSingleSignOnServiceUrl();
            String destinationUrl = actionUrl;
            String nameIDPolicyFormat = deployment.getNameIDPolicyFormat();

            if (nameIDPolicyFormat == null) {
                nameIDPolicyFormat =  JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get();
            }

            String protocolBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get();

            if (deployment.getResponseBinding() == SamlDeployment.Binding.POST) {
                protocolBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get();
            }

            SAML2AuthnRequestBuilder authnRequestBuilder = new SAML2AuthnRequestBuilder()
                    .assertionConsumerUrl(deployment.getAssertionConsumerServiceUrl())
                    .destination(destinationUrl)
                    .issuer(issuerURL)
                    .forceAuthn(deployment.isForceAuthentication())
                    .protocolBinding(protocolBinding)
                    .nameIdPolicy(SAML2NameIDPolicyBuilder.format(nameIDPolicyFormat));
            BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();

            if (deployment.isRequestsSigned()) {


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
            SamlDeployment.Binding samlBinding = deployment.getRequestBinding();
            SamlUtil.sendSaml(httpFacade, actionUrl, binding, document, samlBinding);
        } catch (Exception e) {
            throw new RuntimeException("Could not create authentication request.", e);
        }
        return true;
    }

}
