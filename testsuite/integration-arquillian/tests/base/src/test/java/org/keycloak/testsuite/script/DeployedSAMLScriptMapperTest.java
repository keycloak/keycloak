package org.keycloak.testsuite.script;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.ScriptBasedMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.provider.ScriptProviderDescriptor;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.saml.RoleMapperTest;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.ProtocolMappersUpdater;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.util.JsonSerialization;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.keycloak.common.Profile.Feature.SCRIPTS;
import static org.keycloak.testsuite.arquillian.DeploymentTargetModifier.AUTH_SERVER_CURRENT;
import static org.keycloak.testsuite.saml.RoleMapperTest.createSamlProtocolMapper;
import static org.keycloak.testsuite.util.SamlStreams.assertionsUnencrypted;
import static org.keycloak.testsuite.util.SamlStreams.attributeStatements;
import static org.keycloak.testsuite.util.SamlStreams.attributesUnecrypted;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableFeature(value = SCRIPTS, skipRestart = true)
public class DeployedSAMLScriptMapperTest extends AbstractSamlTest {

    private static final String SCRIPT_DEPLOYMENT_NAME = "scripts.jar";

    private ClientAttributeUpdater cau;
    private ProtocolMappersUpdater pmu;

    // Managed to make sure that archive is deployed once in @BeforeClass stage and undeployed once in @AfterClass stage
    @Deployment(name = SCRIPT_DEPLOYMENT_NAME, managed = true, testable = false)
    @TargetsContainer(AUTH_SERVER_CURRENT)
    public static JavaArchive deploy() throws IOException {
        ScriptProviderDescriptor representation = new ScriptProviderDescriptor();

        representation.addSAMLMapper("My Mapper", "mapper-a.js");

        return ShrinkWrap.create(JavaArchive.class, SCRIPT_DEPLOYMENT_NAME)
                .addAsManifestResource(new StringAsset(JsonSerialization.writeValueAsPrettyString(representation)),
                        "keycloak-scripts.json")
                .addAsResource("scripts/mapper-example.js", "mapper-a.js");
    }

    @BeforeClass
    public static void verifyEnvironment() {
        ContainerAssume.assumeNotAuthServerUndertow();
    }

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
    @DisableFeature(value = SCRIPTS, executeAsLast = false, skipRestart = true)
    public void testScriptMapperNotAvailableThroughAdminRest() {
        assertFalse(adminClient.serverInfo().getInfo().getProtocolMapperTypes().get(SamlProtocol.LOGIN_PROTOCOL).stream()
                .anyMatch(
                        mapper -> ScriptBasedMapper.PROVIDER_ID.equals(mapper.getId())));

        // Doublecheck not possible to create mapper through admin REST
        ProtocolMapperRepresentation mapperRep = createSamlProtocolMapper(ScriptBasedMapper.PROVIDER_ID,
                ProviderConfigProperty.SCRIPT_TYPE, "'hello_' + user.username",
                AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC,
                AttributeStatementHelper.SAML_ATTRIBUTE_NAME, "SCRIPT_ATTRIBUTE"
        );

        Response response = pmu.getResource().createMapper(mapperRep);
        Assert.assertEquals(404, response.getStatus());
        response.close();
    }


    @Test
    public void testScriptMappingThroughServerDeploy() {
        // ScriptBasedMapper still not available even if SCRIPTS feature is enabled
        testScriptMapperNotAvailableThroughAdminRest();

        pmu.add(
                createSamlProtocolMapper("script-mapper-a.js",
                        AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC,
                        AttributeStatementHelper.SAML_ATTRIBUTE_NAME, "SCRIPT_ATTRIBUTE"
                )
        ).update();

        assertLoginSuccessWithAttributeAvailable();
    }


    private void assertLoginSuccessWithAttributeAvailable() {
        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_EMPLOYEE_2, RoleMapperTest.SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2, SamlClient.Binding.POST)
                .build()
                .login().user(bburkeUser).build()
                .getSamlResponse(SamlClient.Binding.POST);

        assertThat(samlResponse.getSamlObject(), Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));

        Stream<AssertionType> assertions = assertionsUnencrypted(samlResponse.getSamlObject());
        Stream<AttributeType> attributes = attributesUnecrypted(attributeStatements(assertions));
        String scriptAttrValue = attributes
                .filter(attribute -> "SCRIPT_ATTRIBUTE".equals(attribute.getName()))
                .map(attribute -> attribute.getAttributeValue().get(0).toString())
                .findFirst().orElseThrow(() -> new AssertionError("Attribute SCRIPT_ATTRIBUTE was not available in SAML assertion"));

        Assert.assertEquals("hello_bburke", scriptAttrValue);
    }

}
