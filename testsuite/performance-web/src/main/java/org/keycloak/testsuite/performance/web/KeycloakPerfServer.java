package org.keycloak.testsuite.performance.web;

import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletInfo;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.keycloak.services.filters.ClientConnectionFilter;
import org.keycloak.services.filters.KeycloakSessionServletFilter;
import org.keycloak.test.tools.KeycloakTestApplication;
import org.keycloak.testutils.KeycloakServer;

import javax.servlet.DispatcherType;
import java.io.InputStream;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakPerfServer {

    private KeycloakServer keycloakServer;

    public static void main(String[] args) throws Throwable {
        // TODO: should be better than programmatic setup here, but don't copy persistence.xml again...
        System.setProperty("hibernate.hbm2ddl.auto", "update");

        if (System.getProperty("undertowWorkerThreads") == null) {
            System.setProperty("undertowWorkerThreads", "256");
        }

        KeycloakServer keycloakServer = KeycloakServer.bootstrapKeycloakServer(args);
        System.out.println("Keycloak server bootstrapped");

        KeycloakSessionFactoryHolder.setKeycloakSessionFactory(keycloakServer.getSessionFactory());
        new KeycloakPerfServer(keycloakServer).start();
    }

    public KeycloakPerfServer(KeycloakServer keycloakServer) {
        this.keycloakServer = keycloakServer;
    }

    public void start() {
        importPerfRealm();
        deployPerfTools();
        deployPerfApp();
    }

    protected void importPerfRealm() {
        InputStream perfRealmStream = KeycloakPerfServer.class.getClassLoader().getResourceAsStream("perfrealm.json");
        keycloakServer.importRealm(perfRealmStream);
    }

    protected void deployPerfTools() {
        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplicationClass(KeycloakToolsApplication.class.getName());

        UndertowJaxrsServer server = keycloakServer.getServer();

        DeploymentInfo di = server.undertowDeployment(deployment, "");
        di.setClassLoader(KeycloakTestApplication.class.getClassLoader());
        di.setContextPath("/keycloak-tools");
        di.setDeploymentName("KeycloakTools");

        FilterInfo filter = Servlets.filter("SessionFilter", KeycloakSessionServletFilter.class);
        di.addFilter(filter);
        di.addFilterUrlMapping("SessionFilter", "/perf/*", DispatcherType.REQUEST);

        FilterInfo connectionFilter = Servlets.filter("ClientConnectionFilter", ClientConnectionFilter.class);
        di.addFilter(connectionFilter);
        di.addFilterUrlMapping("ClientConnectionFilter", "/perf/*", DispatcherType.REQUEST);

        server.deploy(di);

        System.out.println("Keycloak tools deployed");
    }

    protected void deployPerfApp() {
        DeploymentInfo deploymentInfo = new DeploymentInfo();
        deploymentInfo.setClassLoader(getClass().getClassLoader());
        deploymentInfo.setDeploymentName("PerfApp");
        deploymentInfo.setContextPath("/perf-app");

        ServletInfo servlet = new ServletInfo("PerfAppServlet", PerfAppServlet.class);
        servlet.addMapping("/perf-servlet/*");
        servlet.addInitParam(PerfAppServlet.BASE_URL_INIT_PARAM, "http://" + keycloakServer.getConfig().getHost() + ":" + keycloakServer.getConfig().getPort());

        deploymentInfo.addServlet(servlet);

        deploymentInfo.setResourceManager(new ClassPathResourceManager(getClass().getClassLoader()));

        keycloakServer.getServer().deploy(deploymentInfo);

        System.out.println("PerfApp deployed");
    }
}
