package org.keycloak.testsuite.broker;

import org.keycloak.admin.client.resource.UserResource;
import com.google.common.collect.ImmutableMap;
import org.keycloak.broker.saml.mappers.AttributeToRoleMapper;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.StatementAbstractType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.getAuthServerContextRoot;
import static org.keycloak.testsuite.broker.AbstractBrokerTest.ROLE_MANAGER;
import static org.keycloak.testsuite.broker.AbstractBrokerTest.ROLE_USER;
import static org.keycloak.testsuite.saml.RoleMapperTest.ROLE_ATTRIBUTE_NAME;
import static org.keycloak.testsuite.util.Matchers.isSamlResponse;
import static org.keycloak.testsuite.util.SamlStreams.assertionsUnencrypted;
import static org.keycloak.testsuite.util.SamlStreams.attributeStatements;
import static org.keycloak.testsuite.util.SamlStreams.attributesUnecrypted;

/**
 * Final class as it's not intended to be overriden. Feel free to remove "final" if you really know what you are doing.
 */
public final class KcSamlBrokerTest extends AbstractAdvancedBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    private static final String EMPTY_ATTRIBUTE_NAME = "empty.attribute.name";

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

        IdentityProviderMapperRepresentation attrMapper5 = new IdentityProviderMapperRepresentation();
        attrMapper5.setName("empty-attribute-to-role-mapper");
        attrMapper5.setIdentityProviderMapper(AttributeToRoleMapper.PROVIDER_ID);
        attrMapper5.setConfig(ImmutableMap.<String,String>builder()
                .put(UserAttributeMapper.ATTRIBUTE_NAME, EMPTY_ATTRIBUTE_NAME)
                .put(ATTRIBUTE_VALUE, "")
                .put("role", EMPTY_ATTRIBUTE_ROLE)
                .build());

        return Arrays.asList(new IdentityProviderMapperRepresentation[] { attrMapper1, attrMapper2, attrMapper3, attrMapper4, attrMapper5 });
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
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST + ".dot/ted", getAuthServerContextRoot() + "/sales-post/saml", null);

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

    @Test
    public void emptyAttributeToRoleMapperTest() throws ParsingException, ConfigurationException, ProcessingException {
        createRolesForRealm(bc.consumerRealmName());
        createRoleMappersForConsumerRealm();

        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST + ".dot/ted", getAuthServerContextRoot() + "/sales-post/saml", null);

        Document doc = SAML2Request.convert(loginRep);

        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(bc.consumerRealmName()), doc, Binding.POST).build()   // Request to consumer IdP
                .login().idp(bc.getIDPAlias()).build()

                .processSamlResponse(Binding.POST)    // AuthnRequest to producer IdP
                .targetAttributeSamlRequest()
                .build()

                .login().user(bc.getUserLogin(), bc.getUserPassword()).build()

                .processSamlResponse(Binding.POST)    // Response from producer IdP
                    .transformObject(ob -> {
                        assertThat(ob, org.keycloak.testsuite.util.Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                        ResponseType resp = (ResponseType) ob;

                        Set<StatementAbstractType> statements = resp.getAssertions().get(0).getAssertion().getStatements();

                        AttributeStatementType attributeType = (AttributeStatementType) statements.stream()
                                .filter(statement -> statement instanceof AttributeStatementType)
                                .findFirst().orElse(new AttributeStatementType());

                        AttributeType attr = new AttributeType(EMPTY_ATTRIBUTE_NAME);
                        attr.addAttributeValue(null);

                        attributeType.addAttribute(new AttributeStatementType.ASTChoiceType(attr));
                        resp.getAssertions().get(0).getAssertion().addStatement(attributeType);

                        return ob;
                    })
                .build()

                // first-broker flow
                .updateProfile().firstName("a").lastName("b").email(bc.getUserEmail()).username(bc.getUserLogin()).build()
                .followOneRedirect()

                .getSamlResponse(Binding.POST);       // Response from consumer IdP

        Assert.assertThat(samlResponse, Matchers.notNullValue());
        Assert.assertThat(samlResponse.getSamlObject(), isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));

        Stream<AssertionType> assertionTypeStream = assertionsUnencrypted(samlResponse.getSamlObject());
        Stream<AttributeType> attributeStatementTypeStream = attributesUnecrypted(attributeStatements(assertionTypeStream));
        Set<String> attributeValues = attributeStatementTypeStream
                .filter(a -> a.getName().equals(ROLE_ATTRIBUTE_NAME))
                .flatMap(a -> a.getAttributeValue().stream())
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertThat(attributeValues, hasItems(EMPTY_ATTRIBUTE_ROLE));

    }
}
