package org.keycloak.testsuite.rule;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.WebResourceCollection;
import org.junit.rules.ExternalResource;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CacheKeycloakSession;
import org.keycloak.provider.ProviderSession;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.ModelToRepresentation;
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

    public UserRepresentation getUser(String realm, String name) {
        ProviderSession providerSession = server.getProviderSessionFactory().createSession();
        KeycloakSession session = providerSession.getProvider(KeycloakSession.class);
        try {
            UserModel user = session.getRealmByName(realm).getUser(name);
            return user != null ? ModelToRepresentation.toRepresentation(user) : null;
        } finally {
            providerSession.close();
        }
    }

    public UserRepresentation getUserById(String realm, String id) {
        ProviderSession providerSession = server.getProviderSessionFactory().createSession();
        KeycloakSession session = providerSession.getProvider(KeycloakSession.class);
        try {
            return ModelToRepresentation.toRepresentation(session.getRealmByName(realm).getUserById(id));
        } finally {
            providerSession.close();
        }
    }

    protected void setupKeycloak() {
        ProviderSession providerSession = server.getProviderSessionFactory().createSession();
        KeycloakSession session = providerSession.getProvider(KeycloakSession.class);
        session.getTransaction().begin();

        try {
            RealmManager manager = new RealmManager(session);

            RealmModel adminstrationRealm = manager.getRealm(Config.getAdminRealm());

            configure(manager, adminstrationRealm);

            session.getTransaction().commit();
        } finally {
            providerSession.close();
        }
    }

    protected void configure(RealmManager manager, RealmModel adminRealm) {

    }

    public void deployServlet(String name, String contextPath, Class<? extends Servlet> servletClass) {
        DeploymentInfo deploymentInfo = createDeploymentInfo(name, contextPath, servletClass);
        server.getServer().deploy(deploymentInfo);
    }

    private DeploymentInfo createDeploymentInfo(String name, String contextPath, Class<? extends Servlet> servletClass) {
        DeploymentInfo deploymentInfo = new DeploymentInfo();
        deploymentInfo.setClassLoader(getClass().getClassLoader());
        deploymentInfo.setDeploymentName(name);
        deploymentInfo.setContextPath(contextPath);

        ServletInfo servlet = new ServletInfo(servletClass.getSimpleName(), servletClass);
        servlet.addMapping("/*");

        deploymentInfo.addServlet(servlet);
        return deploymentInfo;
    }

    public void deployApplication(String name, String contextPath, Class<? extends Servlet> servletClass, String adapterConfigPath, String role) {
        DeploymentInfo di = createDeploymentInfo(name, contextPath, servletClass);
        di.addInitParameter("keycloak.config.file", adapterConfigPath);
        SecurityConstraint constraint = new SecurityConstraint();
        WebResourceCollection collection = new WebResourceCollection();
        collection.addUrlPattern("/*");
        constraint.addWebResourceCollection(collection);
        constraint.addRoleAllowed(role);
        di.addSecurityConstraint(constraint);
        LoginConfig loginConfig = new LoginConfig("KEYCLOAK", "demo");
        di.setLoginConfig(loginConfig);
        server.getServer().deploy(di);
    }

    @Override
    protected void after() {
        server.stop();

        // Add some variable delay (Some windows envs have issues as server is not stopped immediately after server.stop)
        try {
            int sleepInterval = Integer.parseInt(System.getProperty("testsuite.delay", "0"));
            Thread.sleep(sleepInterval);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
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

    public ProviderSession startSession() {
        ProviderSession providerSession = server.getProviderSessionFactory().createSession();
        KeycloakSession session = providerSession.getProvider(KeycloakSession.class);
        session.getTransaction().begin();
        return providerSession;
    }

    public KeycloakSession startCacheSession() {
        ProviderSession providerSession = server.getProviderSessionFactory().createSession();
        KeycloakSession session = providerSession.getProvider(CacheKeycloakSession.class);
        session.getTransaction().begin();
        return session;
    }

    public void stopSession(ProviderSession session, boolean commit) {
        if (commit) {
            session.getProvider(KeycloakSession.class).getTransaction().commit();
        }
        session.close();
    }
}
