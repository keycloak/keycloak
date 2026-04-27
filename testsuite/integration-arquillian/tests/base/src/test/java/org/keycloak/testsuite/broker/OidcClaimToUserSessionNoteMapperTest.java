package org.keycloak.testsuite.broker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.oidc.mappers.ClaimToUserSessionNoteMapper;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class OidcClaimToUserSessionNoteMapperTest extends AbstractIdentityProviderMapperTest {

    private static final String CLAIM_NAME = "sessionNoteTest";
    private static final String CLAIM_VALUE = "foo";
    private static final String CONFIG_PROPERTY_CLAIMS = "claims";

    private static final String HARD_CODED_CLAIM_CONFIG_PROPERTY_CLAIM_VALUE = "claim.value";

    private ClientRepresentation consumerClientRep;
    private String providerClientUuid;
    private String providerHardcodedClaimMapperId;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Before
    public void setup() {
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        consumerClientRep = consumerRealm.clients().findByClientId("broker-app").get(0);

        setupIdentityProvider();
        // initialize the user with firstName and lastName, to avoid having to complete account data after login
        createUserInProviderRealm(Map.of(
                UserModel.FIRST_NAME, Collections.singletonList("FIRST NAME"),
                UserModel.LAST_NAME, Collections.singletonList("LAST NAME")));

        ProtocolMapperRepresentation consumerSessionNoteToClaimMapper = new ProtocolMapperRepresentation();
        consumerSessionNoteToClaimMapper.setName("Session Note To Claim");
        consumerSessionNoteToClaimMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        consumerSessionNoteToClaimMapper.setProtocolMapper(UserSessionNoteMapper.PROVIDER_ID);
        consumerSessionNoteToClaimMapper.setConfig(Map.of("user.session.note", CLAIM_NAME, "claim.name", CLAIM_NAME,
                "access.token.claim", "true"));
        CreatedResponseUtil.getCreatedId(consumerRealm.clients().get(consumerClientRep.getId()).getProtocolMappers()
                .createMapper(consumerSessionNoteToClaimMapper));

        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        ClientRepresentation providerClientRep = providerRealm.clients().findByClientId("brokerapp").get(0);
        providerClientUuid = providerClientRep.getId();

        ProtocolMapperRepresentation providerHardcodedClaimMapper = new ProtocolMapperRepresentation();
        providerHardcodedClaimMapper.setName("Hardcoded Claim");
        providerHardcodedClaimMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        providerHardcodedClaimMapper.setProtocolMapper(HardcodedClaim.PROVIDER_ID);
        providerHardcodedClaimMapper.setConfig(Map.of("claim.name", CLAIM_NAME,
                HARD_CODED_CLAIM_CONFIG_PROPERTY_CLAIM_VALUE, CLAIM_VALUE, "access.token.claim", "true"));
        providerHardcodedClaimMapperId = CreatedResponseUtil
                .getCreatedId(providerRealm.clients().get(providerClientRep.getId()).getProtocolMappers()
                        .createMapper(providerHardcodedClaimMapper));
    }

    @Test
    public void claimIsPropagatedOnFirstLoginOnlyWhenNameMatchesAndSyncModeIsImport() {
        createUserSessionNoteIdpMapper(IdentityProviderMapperSyncMode.IMPORT, CLAIM_VALUE);

        AccessToken accessToken = login();

        assertThat(accessToken.getOtherClaims().get(CLAIM_NAME), equalTo(CLAIM_VALUE));

        logout();

        AccessToken accessTokenSecondLogin = login();

        // claim should still have a value, because mapping is only applied on import
        assertThat(accessTokenSecondLogin.getOtherClaims().get(CLAIM_NAME), nullValue());
    }

    @Test
    public void claimIsPropagatedOnAllLoginsWhenNameMatchesAndSyncModeIsForce() {
        IdentityProviderMapperRepresentation userSessionNoteIdpMapper =
                createUserSessionNoteIdpMapper(IdentityProviderMapperSyncMode.FORCE, CLAIM_VALUE);

        AccessToken accessTokenFirstLogin = login();

        assertThat(accessTokenFirstLogin.getOtherClaims().get(CLAIM_NAME), equalTo(CLAIM_VALUE));

        logout();

        String updatedClaimValue = "updated-claim-value";
        updateProviderHardcodedClaimMapper(updatedClaimValue);
        updateUserSessionNoteIdpMapper(userSessionNoteIdpMapper, updatedClaimValue);

        AccessToken accessTokenSecondLogin = login();

        assertThat(accessTokenSecondLogin.getOtherClaims().get(CLAIM_NAME), equalTo(updatedClaimValue));
    }

    @Test
    public void claimIsNotPropagatedWhenNameDoesNotMatch() {
        createUserSessionNoteIdpMapper(IdentityProviderMapperSyncMode.IMPORT, "something-unexpected-1", "something-unexpected-2");

        AccessToken accessToken = login();

        assertThat(accessToken.getOtherClaims().get(CLAIM_NAME), nullValue());
    }

    private void logout() {
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
    }

    private AccessToken login() {
        oauth.realm(bc.consumerRealmName())
                .client("broker-app", consumerClientRep.getSecret())
                .redirectUri(getAuthServerRoot() + "realms/" + bc.consumerRealmName() + "/app");
        AuthorizationEndpointResponse authzResponse = doLoginSocial(oauth, bc.getIDPAlias(), bc.getUserLogin(), bc.getUserPassword());

        String code = authzResponse.getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        return toAccessToken(response.getAccessToken());
    }

    private AccessToken toAccessToken(String encoded) {
        AccessToken accessToken;

        try {
            accessToken = new JWSInput(encoded).readJsonContent(AccessToken.class);
        } catch (JWSInputException cause) {
            throw new RuntimeException("Failed to deserialize token", cause);
        }
        return accessToken;
    }

    private IdentityProviderMapperRepresentation createUserSessionNoteIdpMapper(IdentityProviderMapperSyncMode syncMode,
            String... matchingValue) {
        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("User Session Note Idp Mapper");
        mapper.setIdentityProviderMapper(ClaimToUserSessionNoteMapper.PROVIDER_ID);

        mapper.setConfig(ImmutableMap.<String, String> builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(CONFIG_PROPERTY_CLAIMS, createClaimsConfig(matchingValue))
                .build());

        return persistMapper(mapper);
    }

    private String createClaimsConfig(String... matchingValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (matchingValue != null) {
            for (String value : matchingValue) {
                sb.append("{\"key\":\"").append(CLAIM_NAME).append("\",\"value\":\"").append(value).append("\"},");
            }
            sb.setLength(sb.length() - 1);
        }
        sb.append("]");
        return sb.toString();
    }

    private void updateProviderHardcodedClaimMapper(String value) {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        ProtocolMappersResource clientProtocolMappersResource =
                providerRealm.clients().get(providerClientUuid).getProtocolMappers();
        ProtocolMapperRepresentation mapper =
                clientProtocolMappersResource.getMapperById(providerHardcodedClaimMapperId);
        Map<String, String> existingConfig = mapper.getConfig();
        Map<String, String> newConfig = existingConfig == null ? new HashMap<>() : existingConfig;
        newConfig.put(HARD_CODED_CLAIM_CONFIG_PROPERTY_CLAIM_VALUE, value);
        mapper.setConfig(newConfig);

        clientProtocolMappersResource.update(mapper.getId(), mapper);
    }


    private void updateUserSessionNoteIdpMapper(IdentityProviderMapperRepresentation mapper, String matchingValue) {
        Map<String, String> existingConfig = mapper.getConfig();
        Map<String, String> newConfig = existingConfig == null ? new HashMap<>() : existingConfig;
        newConfig.put(CONFIG_PROPERTY_CLAIMS, createClaimsConfig(matchingValue));
        mapper.setConfig(newConfig);

        IdentityProviderResource idpResource = realm.identityProviders().get(bc.getIDPAlias());
        idpResource.update(mapper.getId(), mapper);
    }

    private IdentityProviderMapperRepresentation persistMapper(IdentityProviderMapperRepresentation idpMapper) {
        String idpAlias = bc.getIDPAlias();
        IdentityProviderResource idpResource = realm.identityProviders().get(idpAlias);
        idpMapper.setIdentityProviderAlias(idpAlias);

        String createdId = CreatedResponseUtil.getCreatedId(idpResource.addMapper(idpMapper));
        return idpResource.getMapperById(createdId);
    }
}
