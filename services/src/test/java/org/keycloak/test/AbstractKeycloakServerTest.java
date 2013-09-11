package org.keycloak.test;

import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.jwt.JsonSerialization;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.keycloak.SkeletonKeyContextResolver;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.filters.KeycloakSessionServletFilter;
import org.keycloak.services.resources.KeycloakApplication;

import javax.servlet.DispatcherType;
import javax.ws.rs.client.Client;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AbstractKeycloakServerTest {
    public static UndertowJaxrsServer server;
    public static ResteasyDeployment deployment;
    public static Client client;
    public static KeycloakApplication application;

    @BeforeClass
    public static void undertowSetup() throws Exception {
        deployment = new ResteasyDeployment();
        deployment.setApplicationClass(KeycloakApplication.class.getName());
        server = new UndertowJaxrsServer().start();
        DeploymentInfo di = server.undertowDeployment(deployment);
        di.setClassLoader(AbstractKeycloakServerTest.class.getClassLoader());
        di.setContextPath("/");
        di.setDeploymentName("Keycloak");

        FilterInfo filter = Servlets.filter("SessionFilter", KeycloakSessionServletFilter.class);
        di.addFilter(filter);
        di.addFilterUrlMapping("SessionFilter", "/*", DispatcherType.REQUEST);
        server.deploy(di);
        application = (KeycloakApplication) deployment.getApplication();
        client = new ResteasyClientBuilder().connectionPoolSize(10).build();
        client.register(SkeletonKeyContextResolver.class);

    }

    @AfterClass
    public static void undertowShutdown() throws Exception {
        server.stop();
    }

    public static RealmRepresentation loadJson(String path) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) {
            os.write(c);
        }
        byte[] bytes = os.toByteArray();
        System.out.println(new String(bytes));

        return JsonSerialization.fromBytes(RealmRepresentation.class, bytes);
    }
}
