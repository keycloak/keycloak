package org.keycloak.testsuite.adapter.servlet;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.StatementAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.RoleListMapper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.saml.SAML2ErrorResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.constants.X500SAMLProfileConstants;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.adapter.page.Employee2Servlet;
import org.keycloak.testsuite.adapter.page.EmployeeSigServlet;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.w3c.dom.Document;

import static org.keycloak.testsuite.admin.ApiUtil.getCreatedId;
import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_PRIVATE_KEY;
import static org.keycloak.testsuite.saml.AbstractSamlTest.REALM_PUBLIC_KEY;
import static org.keycloak.testsuite.util.Matchers.bodyHC;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;
import static org.keycloak.testsuite.util.UIUtils.getRawPageSource;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * @author mhajas
 */
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP8)
public class SAMLLoginResponseHandlingTest extends AbstractSAMLServletAdapterTest {

    @Page
    protected Employee2Servlet employee2ServletPage;

    @Page
    protected EmployeeSigServlet employeeSigServletPage;

    @Deployment(name = Employee2Servlet.DEPLOYMENT_NAME)
    protected static WebArchive employee2() {
        return samlServletDeployment(Employee2Servlet.DEPLOYMENT_NAME, WEB_XML_WITH_ACTION_FILTER, SendUsernameServlet.class, AdapterActionsFilter.class);
    }

    @Deployment(name = EmployeeSigServlet.DEPLOYMENT_NAME)
    protected static WebArchive employeeSig() {
        return samlServletDeployment(EmployeeSigServlet.DEPLOYMENT_NAME, SendUsernameServlet.class);
    }

    @Test
    public void testNilAttributeValueAttribute() {
        beginAuthenticationAndLogin(employee2ServletPage, SamlClient.Binding.POST)
                .processSamlResponse(SamlClient.Binding.POST) // Update response with Nil attribute
                .transformObject(ob -> {
                    assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                    ResponseType resp = (ResponseType) ob;

                    Set<StatementAbstractType> statements = resp.getAssertions().get(0).getAssertion().getStatements();

                    AttributeStatementType attributeType = (AttributeStatementType) statements.stream()
                            .filter(statement -> statement instanceof AttributeStatementType)
                            .findFirst().orElse(new AttributeStatementType());

                    AttributeType attr = new AttributeType("attribute-with-null-attribute-value");
                    attr.addAttributeValue(null);

                    attributeType.addAttribute(new AttributeStatementType.ASTChoiceType(attr));
                    resp.getAssertions().get(0).getAssertion().addStatement(attributeType);

                    return ob;
                })
                .build()
                .navigateTo(employee2ServletPage.getUriBuilder().clone().path("getAttributes").build())
                .execute(response -> {
                            assertThat(response, statusCodeIsHC(Response.Status.OK));
                            assertThat(response, bodyHC(containsString("attribute-with-null-attribute-value: <br />")));
                        }
                );
    }

    @Test
    public void testErrorHandlingUnsigned() throws Exception {
        SAML2ErrorResponseBuilder builder = new SAML2ErrorResponseBuilder()
                .destination(employeeSigServletPage.toString() + "saml")
                .issuer("http://localhost:" + System.getProperty("auth.server.http.port", "8180") + "/realms/demo")
                .status(JBossSAMLURIConstants.STATUS_REQUEST_DENIED.get());
        Document document = builder.buildDocument();

        new SamlClientBuilder()
                .addStep((client, currentURI, currentResponse, context) ->
                        SamlClient.Binding.REDIRECT.createSamlUnsignedResponse(URI.create(employeeSigServletPage.toString() + "/saml"), null, document))
                .execute(closeableHttpResponse -> assertThat(closeableHttpResponse, bodyHC(containsString("INVALID_SIGNATURE"))));
    }

    @Test
    public void testErrorHandlingSigned() throws Exception {
        SAML2ErrorResponseBuilder builder = new SAML2ErrorResponseBuilder()
                .destination(employeeSigServletPage.toString() + "saml")
                .issuer("http://localhost:" + System.getProperty("auth.server.http.port", "8180") + "/realms/demo")
                .status(JBossSAMLURIConstants.STATUS_REQUEST_DENIED.get());
        Document document = builder.buildDocument();

        new SamlClientBuilder()
                .addStep((client, currentURI, currentResponse, context) ->
                        SamlClient.Binding.REDIRECT.createSamlSignedResponse(URI.create(employeeSigServletPage.toString() + "/saml"), null, document, REALM_PRIVATE_KEY, REALM_PUBLIC_KEY))
                .execute(closeableHttpResponse -> assertThat(closeableHttpResponse, bodyHC(containsString("ERROR_STATUS"))));
    }

    @Test
    public void testAttributes() throws Exception {
        ClientResource clientResource = ApiUtil.findClientResourceByClientId(testRealmResource(), AbstractSamlTest.SAML_CLIENT_ID_EMPLOYEE_2);
        ProtocolMappersResource protocolMappersResource = clientResource.getProtocolMappers();

        Map<String, String> config = new LinkedHashMap<>();
        config.put("attribute.nameformat", "Basic");
        config.put("user.attribute", "topAttribute");
        config.put("attribute.name", "topAttribute");
        getCleanup().addCleanup(createProtocolMapper(protocolMappersResource, "topAttribute", "saml", "saml-user-attribute-mapper", config));

        config = new LinkedHashMap<>();
        config.put("attribute.nameformat", "Basic");
        config.put("user.attribute", "level2Attribute");
        config.put("attribute.name", "level2Attribute");
        getCleanup().addCleanup(createProtocolMapper(protocolMappersResource, "level2Attribute", "saml", "saml-user-attribute-mapper", config));

        config = new LinkedHashMap<>();
        config.put("attribute.nameformat", "Basic");
        config.put("single", "true");
        config.put("attribute.name", "group");
        getCleanup().addCleanup(createProtocolMapper(protocolMappersResource, "groups", "saml", "saml-group-membership-mapper", config));

        setRolesToCheck("manager,user");

        employee2ServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
        testRealmSAMLPostLoginPage.form().login("level2GroupUser", "password");

        driver.navigate().to(employee2ServletPage.getUriBuilder().clone().path("getAttributes").build().toURL());
        waitUntilElement(By.xpath("//body")).text().contains("topAttribute: true");
        waitUntilElement(By.xpath("//body")).text().contains("level2Attribute: true");
        waitUntilElement(By.xpath("//body")).text().contains(X500SAMLProfileConstants.EMAIL.get() + ": level2@redhat.com");
        waitUntilElement(By.xpath("//body")).text().not().contains("group: []");
        waitUntilElement(By.xpath("//body")).text().not().contains("group: null");
        waitUntilElement(By.xpath("//body")).text().not().contains("group: <br />");
        waitUntilElement(By.xpath("//body")).text().contains("group: level2");

        employee2ServletPage.logout();
        checkLoggedOut(employee2ServletPage, testRealmSAMLPostLoginPage);

        setRolesToCheck("manager,employee,user");

        employee2ServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
        testRealmSAMLPostLoginPage.form().login(bburkeUser);

        driver.navigate().to(employee2ServletPage.getUriBuilder().clone().path("getAttributes").build().toURL());
        waitUntilElement(By.xpath("//body")).text().contains(X500SAMLProfileConstants.EMAIL.get() + ": bburke@redhat.com");
        waitUntilElement(By.xpath("//body")).text().contains("friendly email: bburke@redhat.com");
        waitUntilElement(By.xpath("//body")).text().contains("phone: 617");
        waitUntilElement(By.xpath("//body")).text().not().contains("friendly phone:");

        driver.navigate().to(employee2ServletPage.getUriBuilder().clone().path("getAssertionFromDocument").build().toURL());
        waitForPageToLoad();
        Assert.assertEquals("", getRawPageSource());

        employee2ServletPage.logout();
        checkLoggedOut(employee2ServletPage, testRealmSAMLPostLoginPage);

        config = new LinkedHashMap<>();
        config.put("attribute.value", "hard");
        config.put("attribute.nameformat", "Basic");
        config.put("attribute.name", "hardcoded-attribute");
        getCleanup().addCleanup(createProtocolMapper(protocolMappersResource, "hardcoded-attribute", "saml", "saml-hardcode-attribute-mapper", config));

        config = new LinkedHashMap<>();
        config.put("role", "hardcoded-role");
        getCleanup().addCleanup(createProtocolMapper(protocolMappersResource, "hardcoded-role", "saml", "saml-hardcode-role-mapper", config));

        config = new LinkedHashMap<>();
        config.put("new.role.name", "pee-on");
        config.put("role", "http://localhost:8280/employee/.employee");
        getCleanup().addCleanup(createProtocolMapper(protocolMappersResource, "renamed-employee-role", "saml", "saml-role-name-mapper", config));

        for (ProtocolMapperRepresentation mapper : clientResource.toRepresentation().getProtocolMappers()) {
            if (mapper.getName().equals("role-list")) {
                protocolMappersResource.delete(mapper.getId());
                Map<String, String> origConfig = new HashMap<>(mapper.getConfig());

                mapper.setId(null);
                mapper.getConfig().put(RoleListMapper.SINGLE_ROLE_ATTRIBUTE, "true");
                mapper.getConfig().put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, "memberOf");

                try (Response response = protocolMappersResource.createMapper(mapper)) {
                    String createdId = getCreatedId(response);
                    getCleanup().addCleanup(() -> {
                        protocolMappersResource.delete(createdId);
                        mapper.setConfig(origConfig);
                        protocolMappersResource.createMapper(mapper).close();
                    });
                }
            }
        }

        setRolesToCheck("pee-on,el-jefe,manager,hardcoded-role");

        config = new LinkedHashMap<>();
        config.put("new.role.name", "el-jefe");
        config.put("role", "user");
        getCleanup().addCleanup(createProtocolMapper(protocolMappersResource, "renamed-role", "saml", "saml-role-name-mapper", config));

        employee2ServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
        testRealmSAMLPostLoginPage.form().login(bburkeUser);

        driver.navigate().to(employee2ServletPage.getUriBuilder().clone().path("getAttributes").build().toURL());
        waitUntilElement(By.xpath("//body")).text().contains("hardcoded-attribute: hard");
        employee2ServletPage.checkRolesEndPoint(false);
        employee2ServletPage.logout();
        checkLoggedOut(employee2ServletPage, testRealmSAMLPostLoginPage);
    }

    private void setRolesToCheck(String roles) throws Exception {
        employee2ServletPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmSAMLPostLoginPage);
        testRealmSAMLPostLoginPage.form().login(bburkeUser);
        driver.navigate().to(employee2ServletPage.getUriBuilder().clone().path("setCheckRoles").queryParam("roles", roles).build().toURL());
        WaitUtils.waitUntilElement(By.tagName("body")).text().contains("These roles will be checked:");
        employee2ServletPage.logout();
    }
}
