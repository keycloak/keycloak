package org.keycloak.testsuite.saml;

import java.io.IOException;

import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.mappers.NameIdMapperHelper;
import org.keycloak.protocol.saml.mappers.UserAttributeNameIdMapper;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.ProtocolMappersUpdater;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.saml.RoleMapperTest.createSamlProtocolMapper;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SCHEME;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;

import static org.junit.Assert.assertEquals;

public class NameIdMapperTest extends AbstractSamlTest {

    public static final String SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2 = AUTH_SERVER_SCHEME + "://localhost:"
            + (AUTH_SERVER_SSL_REQUIRED ? AUTH_SERVER_PORT : 8080) + "/employee2/";

    private ClientAttributeUpdater cau;
    private ProtocolMappersUpdater pmu;

    @Before
    public void setNameIdConfigAndCleanMappers() {
        this.cau = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_EMPLOYEE_2)
                        .setAttribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username")
                        .setAttribute(SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, "true").update();
        this.pmu = cau.protocolMappers().clear().update();
    }

    @After
    public void revertCleanMappersAndScopes() throws IOException {
            this.pmu.close();
            this.cau.close();
    }

    @Test
    public void testNameIdMapper() {
        pmu.add(createSamlProtocolMapper(UserAttributeNameIdMapper.PROVIDER_ID, 
                NameIdMapperHelper.MAPPER_NAMEID_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get(),
                ProtocolMapperUtils.USER_ATTRIBUTE,"email"))
                .update();
        testExpectedNameId(bburkeUser.getEmail());
    }

    @Test
    public void testNameIdMapperNotFound() {
        pmu.add(createSamlProtocolMapper(UserAttributeNameIdMapper.PROVIDER_ID,
                NameIdMapperHelper.MAPPER_NAMEID_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get(),
                ProtocolMapperUtils.USER_ATTRIBUTE, "email"))
                .update();
        testExpectedNameId(bburkeUser.getUsername());
    }

    @Test
    public void testNameIdMapperValueIsNull() {
        pmu.add(createSamlProtocolMapper(UserAttributeNameIdMapper.PROVIDER_ID, 
                NameIdMapperHelper.MAPPER_NAMEID_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get(),
                ProtocolMapperUtils.USER_ATTRIBUTE,"keycloak"))
                .update();
        testExpectedStatusCode(JBossSAMLURIConstants.STATUS_INVALID_NAMEIDPOLICY.get());
    }

    private void testExpectedNameId(String expectedNameId) {
        ResponseType rt = getSamlResponseObject();
        NameIDType nameId = (NameIDType) rt.getAssertions().get(0).getAssertion().getSubject().getSubType().getBaseID();
        assertEquals(expectedNameId,nameId.getValue());
        assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(),rt.getStatus().getStatusCode().getValue().toString());
    }

    private void testExpectedStatusCode(String expectedStatusCode) {
        assertEquals(expectedStatusCode, 
                        getSamlResponseObject().getStatus().getStatusCode().getStatusCode().getValue().toString());
    }

    private ResponseType getSamlResponseObject(){
        SAMLDocumentHolder document = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_EMPLOYEE_2,
                        SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2, Binding.POST)
                .build().login().user(bburkeUser).build().getSamlResponse(Binding.POST);
        return (ResponseType) document.getSamlObject();
    }
}
