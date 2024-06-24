package org.keycloak.testsuite.broker;

import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

public final class KcSamlBrokerArtifactBindingTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }


    @Test
    public void testLogin() {
        // configure artifact binding to the broker
        IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();
        String baseSamlUrl = idpRep.getConfig().get(SAMLIdentityProviderConfig.ARTIFACT_RESOLUTION_SERVICE_URL);
        idpRep.getConfig().put(SAMLIdentityProviderConfig.ARTIFACT_RESOLUTION_SERVICE_URL, baseSamlUrl + "/resolve");
        idpRep.getConfig().put(SAMLIdentityProviderConfig.ARTIFACT_BINDING_RESPONSE, Boolean.TRUE.toString());
        identityProviderResource.update(idpRep);

        // configure artifact binding to the broker client
        RealmResource providerRealm = realmsResouce().realm(bc.providerRealmName());
        ClientRepresentation brokerClient = providerRealm.clients().findByClientId(bc.getIDPClientIdInProviderRealm()).get(0);
        brokerClient.getAttributes().put(SamlConfigAttributes.SAML_ARTIFACT_BINDING, Boolean.TRUE.toString());
        providerRealm.clients().get(brokerClient.getId()).update(brokerClient);

        // login using artifact binding
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("f", "l");
        appPage.assertCurrent();
    }
}
