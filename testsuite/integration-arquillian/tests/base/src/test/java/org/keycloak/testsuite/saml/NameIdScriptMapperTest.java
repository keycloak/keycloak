package org.keycloak.testsuite.saml;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.mappers.NameIdMapperHelper;
import org.keycloak.protocol.saml.mappers.NameIdScriptBasedMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.ProtocolMappersUpdater;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.SamlClient.Binding;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.saml.RoleMapperTest.createSamlProtocolMapper;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SCHEME;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;

import java.io.IOException;

public class NameIdScriptMapperTest extends AbstractSamlTest {
    public static final String SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2 = AUTH_SERVER_SCHEME + "://localhost:"
            + (AUTH_SERVER_SSL_REQUIRED ? AUTH_SERVER_PORT : 8080) + "/employee2/";

    private ClientAttributeUpdater cau;
    private ProtocolMappersUpdater pmu;

    private final String SCRIPT_USERNAME_EMAIL = "var result = user.getUsername() + user.getEmail();\nexports = result;";
    private final String SCRIPT_ERROR = "exports = nothing;";
    private final String SCRIPT_REALM_BINDING = "var result = realm.getName();\nexports = result;";

    @Before
    public void setNameIdConfigAndCleanMappers() {
        this.cau = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_EMPLOYEE_2)
                        .setAttribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "persistent")
                        .setAttribute(SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, "true").update();
        this.pmu = cau.protocolMappers().clear().update();
    }

    @After
    public void revertCleanMappersAndScopes() throws IOException {
            this.pmu.close();
            this.cau.close();
    }

    @Test
    public void testNameIdScriptMapperUserBinding() {
        pmu.add(createSamlProtocolMapper(NameIdScriptBasedMapper.PROVIDER_ID, 
                NameIdMapperHelper.MAPPER_NAMEID_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get(),
                ProviderConfigProperty.SCRIPT_TYPE, SCRIPT_USERNAME_EMAIL))
            .update();
        testExpectedNameId(bburkeUser.getUsername() + bburkeUser.getEmail());
    }

    @Test
    public void testNameIdScriptMapperRealmBinding() {
        pmu.add(createSamlProtocolMapper(NameIdScriptBasedMapper.PROVIDER_ID, 
                NameIdMapperHelper.MAPPER_NAMEID_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get(),
                ProviderConfigProperty.SCRIPT_TYPE, SCRIPT_REALM_BINDING))
            .update();
        testExpectedNameId(REALM_NAME);
    }

    @Test
    public void testScriptException() {
        pmu.add(createSamlProtocolMapper(NameIdScriptBasedMapper.PROVIDER_ID, 
                NameIdMapperHelper.MAPPER_NAMEID_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get(),
                ProviderConfigProperty.SCRIPT_TYPE, SCRIPT_ERROR)).update();
        testExpectedNameId("");
    }

    private void testExpectedNameId(String expectedNameId) {
        ResponseType rt = getSamlResponseObject();
        NameIDType nameId = (NameIDType) rt.getAssertions().get(0).getAssertion().getSubject().getSubType().getBaseID();
        assertEquals(expectedNameId,nameId.getValue());
        assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(),rt.getStatus().getStatusCode().getValue().toString());
    }

    private ResponseType getSamlResponseObject(){
        SAMLDocumentHolder document = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_EMPLOYEE_2,
                        SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2, Binding.POST)
                .build().login().user(bburkeUser).build().getSamlResponse(Binding.POST);
        return (ResponseType) document.getSamlObject();
    }
}
