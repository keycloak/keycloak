package org.keycloak.services.managers;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.TokenIdGenerator;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceAdminManager {
    protected static Logger logger = Logger.getLogger(ResourceAdminManager.class);

    public void singleLogOut(RealmModel realm, String user) {
        ResteasyClient client = new ResteasyClientBuilder()
                .disableTrustManager() // todo fix this, should have a trust manager or a good default
                .build();

        try {
            List<ApplicationModel> resources = realm.getApplications();
            logger.debug("logging out {0} resources ", resources.size());
            for (ApplicationModel resource : resources) {
                logoutResource(realm, resource, user, client);
            }
        } finally {
            client.close();
        }
    }

    protected boolean logoutResource(RealmModel realm, ApplicationModel resource, String user, ResteasyClient client) {
        String managementUrl = resource.getManagementUrl();
        if (managementUrl != null) {
            LogoutAction adminAction = new LogoutAction(TokenIdGenerator.generateId(), (int)(System.currentTimeMillis() / 1000) + 30, resource.getName(), user);
            String token = new TokenManager().encodeToken(realm, adminAction);
            logger.info("logout user: {0} resource: {1} url: {2}", user, resource.getName(), managementUrl);
            Response response = client.target(managementUrl).path(AdapterConstants.K_LOGOUT).request().post(Entity.text(token));
            boolean success = response.getStatus() == 204;
            response.close();
            logger.info("logout success.");
            return success;
        } else {
            logger.info("logout failure.");
            return false;
        }
    }

    public void pushRealmRevocationPolicy(RealmModel realm) {
        ResteasyClient client = new ResteasyClientBuilder()
                .disableTrustManager() // todo fix this, should have a trust manager or a good default
                .build();

        try {
            for (ApplicationModel application : realm.getApplications()) {
                pushRevocationPolicy(realm, application, realm.getNotBefore(), client);
            }
        } finally {
            client.close();
        }
    }

    public void pushApplicationRevocationPolicy(RealmModel realm, ApplicationModel application) {
        ResteasyClient client = new ResteasyClientBuilder()
                .disableTrustManager() // todo fix this, should have a trust manager or a good default
                .build();

        try {
            pushRevocationPolicy(realm, application, application.getNotBefore(), client);
        } finally {
            client.close();
        }
    }


    protected boolean pushRevocationPolicy(RealmModel realm, ApplicationModel resource, int notBefore, ResteasyClient client) {
        if (notBefore <= 0) return false;
        String managementUrl = resource.getManagementUrl();
        if (managementUrl != null) {
            PushNotBeforeAction adminAction = new PushNotBeforeAction(TokenIdGenerator.generateId(), (int)(System.currentTimeMillis() / 1000) + 30, resource.getName(), notBefore);
            String token = new TokenManager().encodeToken(realm, adminAction);
            logger.info("pushRevocation resource: {0} url: {1}", resource.getName(), managementUrl);
            Response response = client.target(managementUrl).path(AdapterConstants.K_PUSH_NOT_BEFORE).request().post(Entity.text(token));
            boolean success = response.getStatus() == 204;
            response.close();
            logger.info("pushRevocation success.");
            return success;
        } else {
            logger.info("no management URL for application: " + resource.getName());
            return false;
        }


    }
}
