package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.mappers.ClaimToRoleMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OidcUserInfoClaimToRoleMapperTest extends AbstractRoleMapperTest {

    protected static final String ATTRIBUTE_TO_MAP_USER_INFO = "user-attribute-info";
    private static final String USER_INFO_CLAIM = ATTRIBUTE_TO_MAP_USER_INFO;
    private static final String USER_INFO_CLAIM_VALUE = "value 1";
    private String claimOnSecondLogin = "";


    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfigurationUserInfoOnlyMappers();
    }

    @Test
    public void singleClaimValueInUserInfoMatches() {
        createClaimToRoleMapper(USER_INFO_CLAIM_VALUE);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(USER_INFO_CLAIM, ImmutableList.<String>builder().add(USER_INFO_CLAIM_VALUE).build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasBeenAssignedInConsumerRealmTo(user);
    }

    @Test
    public void noRoleAddedIfUserInfoDisabledAndOnlyClaimIsInUserInfo() {
        createClaimToRoleMapperWithUserInfoDisabledInIdP(USER_INFO_CLAIM_VALUE);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(USER_INFO_CLAIM, ImmutableList.<String>builder().add(USER_INFO_CLAIM_VALUE).build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasNotBeenAssignedInConsumerRealmTo(user);
    }

    private void createClaimToRoleMapper(String claimValue) {
        IdentityProviderRepresentation idp = setupIdentityProvider();
        createClaimToRoleMapper(idp, claimValue, IdentityProviderMapperSyncMode.IMPORT);
    }

    private void createClaimToRoleMapperWithUserInfoDisabledInIdP(String claimValue) {
        IdentityProviderRepresentation idp = setupIdentityProviderDisableUserInfo();
        createClaimToRoleMapper(idp, claimValue, IdentityProviderMapperSyncMode.IMPORT);
    }

    @Override
    protected void createMapperInIdp(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode) {
        createClaimToRoleMapper(idp, USER_INFO_CLAIM_VALUE, syncMode);
    }

    @Override
    protected void updateUser() {
        UserRepresentation user = findUser(bc.providerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        ImmutableMap<String, List<String>> mismatchingAttributes = ImmutableMap.<String, List<String>>builder()
            .put(USER_INFO_CLAIM, ImmutableList.<String>builder().add(claimOnSecondLogin).build())
            .build();
        user.setAttributes(mismatchingAttributes);
        adminClient.realm(bc.providerRealmName()).users().get(user.getId()).update(user);
    }

    private void createClaimToRoleMapper(IdentityProviderRepresentation idp, String claimValue, IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation claimToRoleMapper = new IdentityProviderMapperRepresentation();
        claimToRoleMapper.setName("userinfo-claim-to-role-mapper");
        claimToRoleMapper.setIdentityProviderMapper(ClaimToRoleMapper.PROVIDER_ID);
        claimToRoleMapper.setConfig(ImmutableMap.<String, String>builder()
            .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
            .put(ClaimToRoleMapper.CLAIM, OidcUserInfoClaimToRoleMapperTest.USER_INFO_CLAIM)
            .put(ClaimToRoleMapper.CLAIM_VALUE, claimValue)
            .put(ConfigConstants.ROLE, CLIENT_ROLE_MAPPER_REPRESENTATION)
            .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        claimToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(claimToRoleMapper).close();
    }

    private class KcOidcBrokerConfigurationUserInfoOnlyMappers extends KcOidcBrokerConfiguration {

        @Override
        public List<ClientRepresentation> createProviderClients() {
            List<ClientRepresentation> clientsRepList = super.createProviderClients();
            log.info("Update provider clients to disable attributes in Access & ID token");

            ProtocolMapperRepresentation userAttrMapper = new ProtocolMapperRepresentation();
            userAttrMapper.setName("attribute - name");
            userAttrMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            userAttrMapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);

            Map<String, String> userAttrMapperConfig = userAttrMapper.getConfig();
            userAttrMapperConfig.put(ProtocolMapperUtils.USER_ATTRIBUTE, ATTRIBUTE_TO_MAP_USER_INFO);
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, ATTRIBUTE_TO_MAP_USER_INFO);
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.JSON_TYPE, ProviderConfigProperty.STRING_TYPE);
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "false");
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "false");
            userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");

            for (ClientRepresentation client: clientsRepList) {
                client.setProtocolMappers(Arrays.asList(userAttrMapper));
            }

            return clientsRepList;

        }

        @Override
        protected void applyDefaultConfiguration(final Map<String, String> config, IdentityProviderSyncMode syncMode) {
            super.applyDefaultConfiguration(config, syncMode);
            config.put("disableUserInfo", "false");
        }
    }

}
