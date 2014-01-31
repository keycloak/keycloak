package org.keycloak.testsuite.rule;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import org.junit.rules.ExternalResource;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testutils.KeycloakServer;
import org.keycloak.util.JsonSerialization;

import javax.servlet.Servlet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractKeycloakRule extends ExternalResource {
    protected KeycloakServer server;

    protected void before() throws Throwable {
        server = new KeycloakServer();
        server.start();


        setupKeycloak();
    }

    protected void setupKeycloak() {
        KeycloakSession session = server.getKeycloakSessionFactory().createSession();
        session.getTransaction().begin();

        try {
            RealmManager manager = new RealmManager(session);

            RealmModel adminstrationRealm = manager.getRealm(Constants.ADMIN_REALM);

            configure(manager, adminstrationRealm);

            session.getTransaction().commit();
        } finally {
            session.close();
        }

    }

    protected void configure(RealmManager manager, RealmModel adminRealm) {

    }

    public void deployServlet(String name, String contextPath, Class<? extends Servlet> servletClass) {
        DeploymentInfo deploymentInfo = new DeploymentInfo();
        deploymentInfo.setClassLoader(getClass().getClassLoader());
        deploymentInfo.setDeploymentName(name);
        deploymentInfo.setContextPath(contextPath);

        ServletInfo servlet = new ServletInfo(servletClass.getSimpleName(), servletClass);
        servlet.addMapping("/*");

        deploymentInfo.addServlet(servlet);
        server.getServer().deploy(deploymentInfo);
    }

    @Override
    protected void after() {
        server.stop();
    }

    public RealmRepresentation loadJson(String path) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) {
            os.write(c);
        }
        byte[] bytes = os.toByteArray();
        return JsonSerialization.readValue(bytes, RealmRepresentation.class);
    }
}
