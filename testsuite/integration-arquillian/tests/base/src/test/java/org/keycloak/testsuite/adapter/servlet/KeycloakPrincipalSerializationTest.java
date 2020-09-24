package org.keycloak.testsuite.adapter.servlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.page.SerializationServletPage;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;


@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
// The purpose of this class is to test KeycloakPrincipal serialization on different app-server-jdks
public class KeycloakPrincipalSerializationTest extends AbstractServletsAdapterTest {
    @Page
    protected SerializationServletPage serializationServlet;

    @Deployment(name = SerializationServletPage.DEPLOYMENT_NAME)
    protected static WebArchive serializationServlet() {
        return servletDeployment(SerializationServletPage.DEPLOYMENT_NAME, SerializationServlet.class, ErrorServlet.class, ServletTestUtils.class);
    }

    @Test
    public void testKeycloakPrincipalSerialization() {
        serializationServlet.navigateTo();
        testRealmLoginPage.form().login("bburke@redhat.com", "password");

        assertThat(driver.getPageSource(), containsString("Serialization/Deserialization was successful"));
        assertThat(driver.getPageSource(), not(containsString("Context was not instance of RefreshableKeycloakSecurityContext")));
        assertThat(driver.getPageSource(), not(containsString("Deserialization failed")));
    }
}
