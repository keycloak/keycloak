package org.keycloak.services.managers;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.TokenIdGenerator;
import org.keycloak.representations.idm.admin.LogoutAction;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RealmModel;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceAdminManager {
    protected Logger logger = Logger.getLogger(ResourceAdminManager.class);

    public void logoutAll(RealmModel realm) {
        singleLogOut(realm, null);
    }

    public void singleLogOut(RealmModel realm, String user) {
        ResteasyClient client = new ResteasyClientBuilder()
                .disableTrustManager() // todo fix this, should have a trust manager or a good default
                .build();

        List<ApplicationModel> resources = realm.getApplications();
        logger.info("logging out " + resources.size() + " resoures.");
        for (ApplicationModel resource : resources) {
            logoutResource(realm, resource, user, client);
        }
    }

    protected boolean logoutResource(RealmModel realm, ApplicationModel resource, String user, ResteasyClient client) {
        String managementUrl = resource.getManagementUrl();
        if (managementUrl != null) {
            LogoutAction adminAction = new LogoutAction(TokenIdGenerator.generateId(), System.currentTimeMillis() / 1000 + 30, resource.getName(), user);
            String token = new TokenManager().encodeToken(realm, adminAction);
            Form form = new Form();
            form.param("token", token);
            logger.info("logout user: " + user + " resource: " + resource.getName() + " url" + managementUrl);
            Response response = client.target(managementUrl).queryParam("action", "logout").request().post(Entity.form(form));
            boolean success = response.getStatus() == 204;
            response.close();
            return success;
        } else {
            return false;
        }
    }

}
