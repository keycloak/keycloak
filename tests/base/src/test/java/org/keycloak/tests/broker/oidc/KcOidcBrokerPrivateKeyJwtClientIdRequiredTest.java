package org.keycloak.tests.broker.oidc;

import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.AuthenticationExecutionBuilder;
import org.keycloak.testframework.realm.AuthenticationFlowBuilder;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.broker.oidc.ClientIdRequiredJWTClientAuthenticator;

import org.junit.jupiter.api.BeforeEach;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class KcOidcBrokerPrivateKeyJwtClientIdRequiredTest extends AbstractKcOidcBrokerTest {

    @BeforeEach
    void configureClientIdRequiredJwt() {
        AuthenticationFlowRepresentation clientFlow = AuthenticationFlowBuilder.create()
                .alias("new-client-flow")
                .description("Base authentication for clients")
                .providerId(AuthenticationFlow.CLIENT_FLOW)
                .topLevel(true)
                .builtIn(false)
                .build();
        providerRealm.admin().flows().createFlow(clientFlow);

        var realm = providerRealm.admin().toRepresentation();
        realm.setClientAuthenticationFlow(clientFlow.getAlias());
        providerRealm.admin().update(realm);

        clientFlow = providerRealm.admin().flows().getFlows().stream()
                .filter(f -> "new-client-flow".equals(f.getAlias()))
                .findFirst().orElseThrow();

        AuthenticationExecutionRepresentation execution = AuthenticationExecutionBuilder.create()
                .parentFlow(clientFlow.getId())
                .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString())
                .authenticator(ClientIdRequiredJWTClientAuthenticator.PROVIDER_ID)
                .priority(10)
                .authenticatorFlow(false)
                .build();
        providerRealm.admin().flows().addExecution(execution);

        KeyMetadataRepresentation rsaKey = consumerRealm.admin().keys().getKeyMetadata().getKeys().stream()
                .filter(k -> k.getPublicKey() != null && KeyUse.SIG.equals(k.getUse())
                        && Algorithm.RS256.equals(k.getAlgorithm()))
                .findFirst().orElseThrow();

        ClientRepresentation client = providerRealm.admin().clients().findByClientId(CLIENT_ID).get(0);
        client.setClientAuthenticatorType(ClientIdRequiredJWTClientAuthenticator.PROVIDER_ID);
        client.getAttributes().put(JWTClientAuthenticator.CERTIFICATE_ATTR, rsaKey.getCertificate());
        providerRealm.admin().clients().get(client.getId()).update(client);

        IdentityProviderRepresentation idp = consumerRealm.admin()
                .identityProviders().get(getIdpAlias()).toRepresentation();
        idp.getConfig().put("clientSecret", null);
        idp.getConfig().put("clientAuthMethod", OIDCLoginProtocol.PRIVATE_KEY_JWT);
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }
}
