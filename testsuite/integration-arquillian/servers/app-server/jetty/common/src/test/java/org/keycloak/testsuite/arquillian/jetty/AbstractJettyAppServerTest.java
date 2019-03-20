package org.keycloak.testsuite.arquillian.jetty;

import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.SecurityHandler;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.adapters.jetty.KeycloakJettyAuthenticator;
import org.keycloak.adapters.saml.jetty.KeycloakSamlAuthenticator;
import org.keycloak.testsuite.arquillian.jetty.container.JettyAppServerProvider;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;

/**
 * This is a basic set of sanity tests for checking Jetty server compatibility.
 * If this test suite is green, it is OK to integrate it with the Arquillian testsuite.
 */
public abstract class AbstractJettyAppServerTest {

    public static final String TEST_SERVLET_NAME = "TestServlet";
    public static final String TEST_SERVLET_URL_MAPPING = "test-servlet";

    @Test
    public void testServerStartupAndShutdown() throws Exception {
        // given
        int httpPort = 8081;
        int httpsPort = 8082;

        JettyAppServerConfiguration configuration = new JettyAppServerConfiguration();
        configuration.setBindHttpPort(httpPort);
        configuration.setBindHttpsPort(httpsPort);
        JettyAppServer server = new JettyAppServer();
        server.setup(configuration);

        // when
        server.start();
        boolean wasFreeOnHTTPPortWhenServerStarted = isFree("localhost", httpPort);
        boolean wasFreeOnHTTPSPortWhenServerStarted = isFree("localhost", httpsPort);
        server.stop();
        boolean wasFreeOnHTTPWhenServerStopped = isFree("localhost", httpPort);
        boolean wasFreeOnHTTPSWhenServerStopped = isFree("localhost", httpsPort);

        // then
        Assert.assertFalse(wasFreeOnHTTPPortWhenServerStarted);
        Assert.assertTrue(wasFreeOnHTTPWhenServerStopped);
        Assert.assertFalse(wasFreeOnHTTPSPortWhenServerStarted);
        Assert.assertTrue(wasFreeOnHTTPSWhenServerStopped);
    }

    @Test
    public void testDeployingServletApp() throws Exception {
        // given
        WebArchive archive = ShrinkWrap.create(WebArchive.class,"archive.war")
                .addClasses(ExampleServlet.class);

        JettyAppServer server = new JettyAppServer();
        Response responseFromTheApp = null;

        // when
        try {
            server.start();
            ProtocolMetaData data = server.deploy(archive);

            HTTPContext servletContext = data.getContexts(HTTPContext.class).iterator().next();
            URI appURI = servletContext.getServletByName(TEST_SERVLET_NAME).getBaseURI().resolve(TEST_SERVLET_URL_MAPPING);

            Client client = ClientBuilder.newClient();
            responseFromTheApp = client.target(appURI).request().get();
        } finally {
            server.stop();
        }

        // assert
        Assert.assertNotNull(responseFromTheApp);
        Assert.assertEquals(200, responseFromTheApp.getStatus());
    }

    @Test
    public void testDeployingRESTApp() throws Exception {
        // given
        WebArchive archive = ShrinkWrap.create(WebArchive.class,"archive.war")
                .addClasses(ExampleRest.class);

        JettyAppServer server = new JettyAppServer();
        Response responseFromTheApp = null;

        // when
        try {
            server.start();
            ProtocolMetaData data = server.deploy(archive);

            HTTPContext servletContext = data.getContexts(HTTPContext.class).iterator().next();
            URI appURI = servletContext.getServlets().get(0).getBaseURI();

            Client client = ClientBuilder.newClient();
            responseFromTheApp = client.target(appURI).request().get();
        } finally {
            server.stop();
        }

        // assert
        Assert.assertNotNull(responseFromTheApp);
        Assert.assertEquals(200, responseFromTheApp.getStatus());
    }

    @Test
    public void testDeployingAndUndeploying() throws Exception {
        // given
        WebArchive archive = ShrinkWrap.create(WebArchive.class,"archive.war")
                .addClasses(ExampleRest.class);

        JettyAppServer server = new JettyAppServer();
        Response responseFromTheApp = null;

        // when
        try {
            server.start();
            ProtocolMetaData data = server.deploy(archive);

            HTTPContext servletContext = data.getContexts(HTTPContext.class).iterator().next();
            URI appURI = servletContext.getServlets().get(0).getBaseURI();

            server.undeploy(archive);

            Client client = ClientBuilder.newClient();
            responseFromTheApp = client.target(appURI).request().get();
        } finally {
            server.stop();
        }

        // assert
        Assert.assertNotNull(responseFromTheApp);
        Assert.assertEquals(404, responseFromTheApp.getStatus());
    }

    @Test
    public void testDetectingSAML() throws Exception {
        // given
        URL webXml = AbstractJettyAppServerTest.class.getResource("/web-saml.xml");
        WebArchive archive = ShrinkWrap.create(WebArchive.class,"archive.war")
                .addAsWebInfResource(webXml, "web.xml");

        JettyAppServer server = new JettyAppServer();

        // when
        Authenticator installedAuthenticator = null;
        try {
            server.start();
            server.deploy(archive);

            installedAuthenticator = server.getServer()
                    .getBean(DeploymentManager.class).getApps().iterator().next()
                    .getContextHandler().getChildHandlerByClass(SecurityHandler.class).getAuthenticator();
        } finally {
            server.stop();
        }

        // assert
        Assert.assertTrue(installedAuthenticator instanceof KeycloakSamlAuthenticator);
    }

    @Test
    public void testDetectingOIDC() throws Exception {
        // given
        URL webXml = AbstractJettyAppServerTest.class.getResource("/web-oidc.xml");
        WebArchive archive = ShrinkWrap.create(WebArchive.class,"archive.war")
                .addAsWebInfResource(webXml, "web.xml");

        JettyAppServer server = new JettyAppServer();

        // when
        Authenticator installedAuthenticator = null;
        try {
            server.start();
            server.deploy(archive);

            installedAuthenticator = server.getServer()
                    .getBean(DeploymentManager.class).getApps().iterator().next()
                    .getContextHandler().getChildHandlerByClass(SecurityHandler.class).getAuthenticator();
        } finally {
            server.stop();
        }

        // assert
        Assert.assertTrue(installedAuthenticator instanceof KeycloakJettyAuthenticator);
    }

    @Test
    public void testJettyVersion() throws Exception {
        // given
        String versionRegexp = "jetty\\d\\d";

        // when
        String appServerName = new JettyAppServerProvider().getName();

        // assert
        Assert.assertTrue(appServerName.matches(versionRegexp));
    }

    @WebServlet(name = TEST_SERVLET_NAME, urlPatterns = "/" + TEST_SERVLET_URL_MAPPING)
    public static class ExampleServlet extends HttpServlet {

        @Override
        public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().println("ok");
        }
    }

    @Path("/")
    public static class ExampleRest extends HttpServlet {

        @GET
        public Response doGet() {
            return Response.ok().build();
        }
    }

    public static boolean isFree(String hostName, int port) {
        SocketAddress socketAddress = new InetSocketAddress(hostName, port);
        Socket socket = new Socket();
        try {
            socket.connect(socketAddress, 2000);
            socket.close();
            return false;
        } catch (Exception exception) {
            return true;
        }
    }

}
