package org.keycloak.testsuite.broker;


import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.provider.ScriptMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.HashMap;

import static org.junit.Assert.assertTrue;

public class SamlJavascriptIdentityProviderMapperTest extends AbstractIdentityProviderMapperTest {

    private static String USER_ATTRIBUTE = "identifier";
    private static String DELIMITER = "@";


    private static String SIMPLE_SCRIPT_TEXT = "brokeredIdentityContext.setUserAttribute('custom-attr','custom-value');";

    private static String COMPLEX_SCRIPT_TEXT =
            "var idp_alias = brokeredIdentityContext.getIdpConfig().getAlias();\n" +
            "var username = brokeredIdentityContext.getUsername();\n" +
            "brokeredIdentityContext.setUserAttribute('"+USER_ATTRIBUTE+"', username+'"+DELIMITER+"'+idp_alias);";


    @Test
    public void testSimple(){
        final IdentityProviderRepresentation idp = setupIdentityProvider();
        final IdentityProviderMapperRepresentation idpMapper = getSimpleJavascriptMapper();
        addIdPMapperToIdp(idp, idpMapper);
        createUserInProviderRealm(new HashMap<>());
        logInAsUserInIDPForFirstTime();
        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertTrue(user.getAttributes().get("custom-attr").contains("custom-value"));

    }


    @Test
    public void testComplex(){
        final IdentityProviderRepresentation idp = setupIdentityProvider();
        final IdentityProviderMapperRepresentation idpMapper = getComplexJavascriptMapper();
        addIdPMapperToIdp(idp, idpMapper);
        createUserInProviderRealm(new HashMap<>());
        logInAsUserInIDPForFirstTime();
        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertTrue(user.getAttributes().get(USER_ATTRIBUTE).contains(BrokerTestConstants.USER_LOGIN + DELIMITER + BrokerTestConstants.IDP_SAML_ALIAS));

    }


    private IdentityProviderMapperRepresentation getSimpleJavascriptMapper(){
        IdentityProviderMapperRepresentation jsSamlIdPMapper = new IdentityProviderMapperRepresentation();
        jsSamlIdPMapper.setName("javascript-saml-idp-mapper");
        jsSamlIdPMapper.setIdentityProviderMapper(ScriptMapper.PROVIDER_ID);
        jsSamlIdPMapper.setConfig(ImmutableMap.<String,String>builder()
                .put(ProviderConfigProperty.SCRIPT_TYPE, SIMPLE_SCRIPT_TEXT)
                .put(ScriptMapper.SINGLE_VALUE_ATTRIBUTE, "true")
                .build());
        return jsSamlIdPMapper;
    }

    private IdentityProviderMapperRepresentation getComplexJavascriptMapper(){
        IdentityProviderMapperRepresentation jsSamlIdPMapper = new IdentityProviderMapperRepresentation();
        jsSamlIdPMapper.setName("javascript-saml-idp-mapper");
        jsSamlIdPMapper.setIdentityProviderMapper(ScriptMapper.PROVIDER_ID);
        jsSamlIdPMapper.setConfig(ImmutableMap.<String,String>builder()
                .put(ProviderConfigProperty.SCRIPT_TYPE, COMPLEX_SCRIPT_TEXT)
                .put(ScriptMapper.SINGLE_VALUE_ATTRIBUTE, "true")
                .build());
        return jsSamlIdPMapper;
    }

    private void addIdPMapperToIdp(IdentityProviderRepresentation idp, IdentityProviderMapperRepresentation jsSamlIdPMapper){
        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        jsSamlIdPMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(jsSamlIdPMapper).close();
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlBrokerConfiguration();
    }

}
