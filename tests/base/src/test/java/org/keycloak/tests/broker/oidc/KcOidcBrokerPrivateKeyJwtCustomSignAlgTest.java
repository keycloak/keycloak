package org.keycloak.testsuite.broker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.keys.GeneratedEcdsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.util.TokenSignatureUtil;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_PROVIDER_ID;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.testsuite.broker.BrokerTestTools.createIdentityProvider;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

public class KcOidcBrokerPrivateKeyJwtCustomSignAlgTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfigurationWithJWTAuthentication();
    }

    private class KcOidcBrokerConfigurationWithJWTAuthentication extends KcOidcBrokerConfiguration {

        String signAlg = Algorithm.ES256;

        @Override
        public List<ClientRepresentation> createProviderClients() {
            List<ClientRepresentation> clientsRepList = super.createProviderClients();
            log.info("Update provider clients to accept JWT authentication");
            for (ClientRepresentation client: clientsRepList) {
                client.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
                if (client.getAttributes() == null) {
                    client.setAttributes(new HashMap<String, String>());
                }
                client.getAttributes().put(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, signAlg);
                client.getAttributes().put(OIDCConfigAttributes.USE_JWKS_URL, "true");
                client.getAttributes().put(OIDCConfigAttributes.JWKS_URL, getConsumerRoot() +
                    "/auth/realms/" + REALM_CONS_NAME + "/protocol/openid-connect/certs");
            }
            return clientsRepList;
        }

        @Override
        public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
            generateEcdsaKeyProvider("valid", signAlg, REALM_CONS_NAME, adminClient);
            IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);
            Map<String, String> config = idp.getConfig();
            applyDefaultConfiguration(config, syncMode);
            config.put("clientSecret", null);
            config.put("clientAuthMethod", OIDCLoginProtocol.PRIVATE_KEY_JWT);
            config.put("clientAssertionSigningAlg", signAlg);
            return idp;
        }

        private void generateEcdsaKeyProvider(String name, String alg, String realmName, Keycloak adminClient) {
            ComponentRepresentation rep = createRep(name, 
                    adminClient.realm(realmName).toRepresentation().getId(), GeneratedEcdsaKeyProviderFactory.ID);
            long priority = System.currentTimeMillis();
            rep.getConfig().putSingle("priority", Long.toString(priority));
            rep.getConfig().putSingle("active", "true");
            rep.getConfig().putSingle("enabled", "true");
            rep.getConfig().putSingle("ecdsaEllipticCurveKey",
                    TokenSignatureUtil.convertAlgorithmToECDomainParamNistRep(alg));
            Response response = adminClient.realm(realmName).components().add(rep);
            response.close();
        }

        protected ComponentRepresentation createRep(String name, String realmId, String providerId) {
            ComponentRepresentation rep = new ComponentRepresentation();
            rep.setName(name);
            rep.setParentId(realmId);
            rep.setProviderId(providerId);
            rep.setProviderType(KeyProvider.class.getName());
            rep.setConfig(new MultivaluedHashMap<>());
            return rep;
        }
    }
}