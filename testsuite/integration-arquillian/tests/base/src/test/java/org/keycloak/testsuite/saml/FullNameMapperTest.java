package org.keycloak.testsuite.saml;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.FullNameMapper;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.ProtocolMappersUpdater;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.saml.RoleMapperTest.createSamlProtocolMapper;
import static org.keycloak.testsuite.util.SamlStreams.assertionsUnencrypted;
import static org.keycloak.testsuite.util.SamlStreams.attributeStatements;
import static org.keycloak.testsuite.util.SamlStreams.attributesUnecrypted;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class FullNameMapperTest extends AbstractSamlTest {

    public static final String FULL_NAME_ATTRIBUTE_NAME = "fullname";

    private ClientAttributeUpdater cau;
    private ProtocolMappersUpdater pmu;

    @Before
    public void cleanMappersAndScopes() {
        this.cau = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_EMPLOYEE_2)
                .setDefaultClientScopes(Collections.EMPTY_LIST)
                .update();
        this.pmu = cau.protocolMappers()
                .clear()
                .update();

        getCleanup(REALM_NAME)
                .addCleanup(this.cau)
                .addCleanup(this.pmu);
    }

    @Test
    public void fullnameMapperTest() throws Exception {
        String fullName = bburkeUser.getFirstName() + " " + bburkeUser.getLastName();

        pmu.add(
            createSamlProtocolMapper(FullNameMapper.PROVIDER_ID,
                AttributeStatementHelper.SAML_ATTRIBUTE_NAME, FULL_NAME_ATTRIBUTE_NAME,
                AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC,
                FullNameMapper.ATTRIBUTE_VALUE
            )
        ).update();

        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
            .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_EMPLOYEE_2, SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2, SamlClient.Binding.POST)
            .build()
            .login().user(bburkeUser).build()
            .getSamlResponse(SamlClient.Binding.POST);

        assertThat(samlResponse.getSamlObject(), Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));

        Stream<AssertionType> assertions = assertionsUnencrypted(samlResponse.getSamlObject());
        Stream<AttributeType> attributes = attributesUnecrypted(attributeStatements(assertions));
        Set<String> attributeValues = attributes
                .filter(a -> a.getName().equals(FULL_NAME_ATTRIBUTE_NAME))
                .flatMap(a -> a.getAttributeValue().stream())
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertThat(attributeValues, hasSize(1));
        assertThat(attributeValues.iterator().next(), fullName);
    }
}
