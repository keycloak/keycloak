package org.keycloak.testsuite.broker;

import org.keycloak.admin.client.resource.UserResource;
import com.google.common.collect.ImmutableMap;
import org.keycloak.broker.saml.mappers.AttributeToRoleMapper;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.Matchers.isSamlResponse;

/**
 * Final class as it's not intended to be overriden. Feel free to remove "final" if you really know what you are doing.
 */
public final class KcSamlBrokerTest extends AbstractAdvancedBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    @Override
    protected Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers() {
        IdentityProviderMapperRepresentation attrMapper1 = new IdentityProviderMapperRepresentation();
        attrMapper1.setName("manager-role-mapper");
        attrMapper1.setIdentityProviderMapper(AttributeToRoleMapper.PROVIDER_ID);
        attrMapper1.setConfig(ImmutableMap.<String,String>builder()
                .put(UserAttributeMapper.ATTRIBUTE_NAME, "Role")
                .put(ATTRIBUTE_VALUE, ROLE_MANAGER)
                .put("role", ROLE_MANAGER)
                .build());

        IdentityProviderMapperRepresentation attrMapper2 = new IdentityProviderMapperRepresentation();
        attrMapper2.setName("user-role-mapper");
        attrMapper2.setIdentityProviderMapper(AttributeToRoleMapper.PROVIDER_ID);
        attrMapper2.setConfig(ImmutableMap.<String,String>builder()
                .put(UserAttributeMapper.ATTRIBUTE_NAME, "Role")
                .put(ATTRIBUTE_VALUE, ROLE_USER)
                .put("role", ROLE_USER)
                .build());

        IdentityProviderMapperRepresentation attrMapper3 = new IdentityProviderMapperRepresentation();
        attrMapper3.setName("friendly-mapper");
        attrMapper3.setIdentityProviderMapper(AttributeToRoleMapper.PROVIDER_ID);
        attrMapper3.setConfig(ImmutableMap.<String,String>builder()
                .put(UserAttributeMapper.ATTRIBUTE_FRIENDLY_NAME, AbstractUserAttributeMapperTest.ATTRIBUTE_TO_MAP_FRIENDLY_NAME)
                .put(ATTRIBUTE_VALUE, ROLE_FRIENDLY_MANAGER)
                .put("role", ROLE_FRIENDLY_MANAGER)
                .build());

        IdentityProviderMapperRepresentation attrMapper4 = new IdentityProviderMapperRepresentation();
        attrMapper4.setName("user-role-dot-guide-mapper");
        attrMapper4.setIdentityProviderMapper(AttributeToRoleMapper.PROVIDER_ID);
        attrMapper4.setConfig(ImmutableMap.<String,String>builder()
                .put(UserAttributeMapper.ATTRIBUTE_NAME, "Role")
                .put(ATTRIBUTE_VALUE, ROLE_USER_DOT_GUIDE)
                .put("role", ROLE_USER_DOT_GUIDE)
                .build());

        return Arrays.asList(new IdentityProviderMapperRepresentation[] { attrMapper1, attrMapper2, attrMapper3, attrMapper4 });
    }

    // KEYCLOAK-3987
    @Test
    @Override
    public void grantNewRoleFromToken() {
        createRolesForRealm(bc.providerRealmName());
        createRolesForRealm(bc.consumerRealmName());

        createRoleMappersForConsumerRealm();

        RoleRepresentation managerRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_MANAGER).toRepresentation();
        RoleRepresentation friendlyManagerRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_FRIENDLY_MANAGER).toRepresentation();
        RoleRepresentation userRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_USER).toRepresentation();

        UserResource userResource = adminClient.realm(bc.providerRealmName()).users().get(userId);
        userResource.roles().realmLevel().add(Collections.singletonList(managerRole));

        logInAsUserInIDPForFirstTime();

        Set<String> currentRoles = userResource.roles().realmLevel().listAll().stream()
          .map(RoleRepresentation::getName)
          .collect(Collectors.toSet());

        assertThat(currentRoles, hasItems(ROLE_MANAGER));
        assertThat(currentRoles, not(hasItems(ROLE_USER, ROLE_FRIENDLY_MANAGER)));

        logoutFromRealm(bc.consumerRealmName());


        userResource.roles().realmLevel().add(Collections.singletonList(userRole));
        userResource.roles().realmLevel().add(Collections.singletonList(friendlyManagerRole));

        logInAsUserInIDP();

        currentRoles = userResource.roles().realmLevel().listAll().stream()
          .map(RoleRepresentation::getName)
          .collect(Collectors.toSet());
        assertThat(currentRoles, hasItems(ROLE_MANAGER, ROLE_USER, ROLE_FRIENDLY_MANAGER));

        logoutFromRealm(bc.consumerRealmName());


        userResource.roles().realmLevel().remove(Collections.singletonList(friendlyManagerRole));

        logInAsUserInIDP();

        currentRoles = userResource.roles().realmLevel().listAll().stream()
          .map(RoleRepresentation::getName)
          .collect(Collectors.toSet());
        assertThat(currentRoles, hasItems(ROLE_MANAGER, ROLE_USER));
        assertThat(currentRoles, not(hasItems(ROLE_FRIENDLY_MANAGER)));

        logoutFromRealm(bc.providerRealmName());
        logoutFromRealm(bc.consumerRealmName());
    }

    @Test
    public void roleWithDots() {
        createRolesForRealm(bc.providerRealmName());
        createRolesForRealm(bc.consumerRealmName());

        createRoleMappersForConsumerRealm();

        RoleRepresentation managerRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_MANAGER).toRepresentation();
        RoleRepresentation friendlyManagerRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_FRIENDLY_MANAGER).toRepresentation();
        RoleRepresentation userRole = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_USER).toRepresentation();
        RoleRepresentation userRoleDotGuide = adminClient.realm(bc.providerRealmName()).roles().get(ROLE_USER_DOT_GUIDE).toRepresentation();

        UserResource userResourceProv = adminClient.realm(bc.providerRealmName()).users().get(userId);
        userResourceProv.roles().realmLevel().add(Collections.singletonList(managerRole));

        logInAsUserInIDPForFirstTime();

        String consUserId = adminClient.realm(bc.consumerRealmName()).users().search(bc.getUserLogin()).iterator().next().getId();
        UserResource userResourceCons = adminClient.realm(bc.consumerRealmName()).users().get(consUserId);

        Set<String> currentRoles = userResourceCons.roles().realmLevel().listAll().stream()
          .map(RoleRepresentation::getName)
          .collect(Collectors.toSet());

        assertThat(currentRoles, hasItems(ROLE_MANAGER));
        assertThat(currentRoles, not(hasItems(ROLE_USER, ROLE_FRIENDLY_MANAGER, ROLE_USER_DOT_GUIDE)));

        logoutFromRealm(bc.consumerRealmName());


        UserRepresentation urp = userResourceProv.toRepresentation();
        urp.setAttributes(new HashMap<>());
        urp.getAttributes().put(AbstractUserAttributeMapperTest.ATTRIBUTE_TO_MAP_FRIENDLY_NAME, Collections.singletonList(ROLE_FRIENDLY_MANAGER));
        userResourceProv.update(urp);
        userResourceProv.roles().realmLevel().add(Collections.singletonList(userRole));
        userResourceProv.roles().realmLevel().add(Collections.singletonList(userRoleDotGuide));

        logInAsUserInIDP();

        currentRoles = userResourceCons.roles().realmLevel().listAll().stream()
          .map(RoleRepresentation::getName)
          .collect(Collectors.toSet());
        assertThat(currentRoles, hasItems(ROLE_MANAGER, ROLE_USER, ROLE_USER_DOT_GUIDE, ROLE_FRIENDLY_MANAGER));

        logoutFromRealm(bc.consumerRealmName());


        urp = userResourceProv.toRepresentation();
        urp.setAttributes(new HashMap<>());
        userResourceProv.update(urp);

        logInAsUserInIDP();

        currentRoles = userResourceCons.roles().realmLevel().listAll().stream()
          .map(RoleRepresentation::getName)
          .collect(Collectors.toSet());
        assertThat(currentRoles, hasItems(ROLE_MANAGER, ROLE_USER, ROLE_USER_DOT_GUIDE));
        assertThat(currentRoles, not(hasItems(ROLE_FRIENDLY_MANAGER)));

        logoutFromRealm(bc.providerRealmName());
        logoutFromRealm(bc.consumerRealmName());
    }

    // KEYCLOAK-6106
    @Test
    public void loginClientWithDotsInName() throws Exception {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST + ".dot/ted", AUTH_SERVER_SCHEME + "://localhost:" + AUTH_SERVER_PORT + "/sales-post/saml", null);

        Document doc = SAML2Request.convert(loginRep);

        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
          .authnRequest(getAuthServerSamlEndpoint(bc.consumerRealmName()), doc, Binding.POST).build()   // Request to consumer IdP
          .login().idp(bc.getIDPAlias()).build()

          .processSamlResponse(Binding.POST)    // AuthnRequest to producer IdP
            .targetAttributeSamlRequest()
            .build()

          .login().user(bc.getUserLogin(), bc.getUserPassword()).build()

          .processSamlResponse(Binding.POST)    // Response from producer IdP
            .build()

          // first-broker flow
          .updateProfile().firstName("a").lastName("b").email(bc.getUserEmail()).username(bc.getUserLogin()).build()
          .followOneRedirect()

          .getSamlResponse(Binding.POST);       // Response from consumer IdP

        Assert.assertThat(samlResponse, Matchers.notNullValue());
        Assert.assertThat(samlResponse.getSamlObject(), isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
    }

}
