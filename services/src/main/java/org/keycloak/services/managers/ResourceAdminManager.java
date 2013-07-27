package org.keycloak.services.managers;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.TokenIdGenerator;
import org.keycloak.representations.idm.admin.LogoutAction;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.ResourceModel;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceAdminManager {

    public void logoutAll(RealmModel realm) {
        singleLogOut(realm, null);
    }

    public void singleLogOut(RealmModel realm, String user) {
        ResteasyClient client = new ResteasyClientBuilder()
                .disableTrustManager() // todo fix this, should have a trust manager or a good default
                .build();

        List<ResourceModel> resources = realm.getResources();
        for (ResourceModel resource : resources) {
            logoutResource(realm, resource, user, client);
        }
    }

    protected boolean logoutResource(RealmModel realm, ResourceModel resource, String user, ResteasyClient client) {
        LogoutAction adminAction = new LogoutAction(TokenIdGenerator.generateId(), System.currentTimeMillis() / 1000 + 30, resource.getName(), user);
        String token = new TokenManager().encodeToken(realm, adminAction);
        Form form = new Form();
        form.param("token", token);
        Response response = client.target(resource.getManagementUrl()).queryParam("action", "logout").request().post(Entity.form(form));
        boolean success = response.getStatus() == 204;
        response.close();
        return success;
    }

}
