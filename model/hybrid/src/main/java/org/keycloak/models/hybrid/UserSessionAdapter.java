package org.keycloak.models.hybrid;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.Session;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter implements UserSessionModel {

    private HybridModelProvider provider;
    private RealmModel realm;
    private Session session;

    UserSessionAdapter(HybridModelProvider provider, RealmModel realm, Session session) {
        this.provider = provider;
        this.realm = realm;
        this.session = session;
    }

    Session getSession() {
        return session;
    }

    @Override
    public String getId() {
        return session.getId();
    }

    @Override
    public void setId(String id) {
        session.setId(id);
    }

    @Override
    public UserModel getUser() {
        return provider.getUserById(session.getUser(), realm);
    }

    @Override
    public void setUser(UserModel user) {
        session.setUser(user.getId());
    }

    @Override
    public String getIpAddress() {
        return session.getIpAddress();
    }

    @Override
    public void setIpAddress(String ipAddress) {
       session.setIpAddress(ipAddress);
    }

    @Override
    public int getStarted() {
        return session.getStarted();
    }

    @Override
    public void setStarted(int started) {
          session.setStarted(started);
    }

    @Override
    public int getLastSessionRefresh() {
        return session.getLastSessionRefresh();
    }

    @Override
    public void setLastSessionRefresh(int seconds) {
          session.setLastSessionRefresh(seconds);
    }

    @Override
    public void associateClient(ClientModel client) {
        session.associateClient(client.getId());
    }

    @Override
    public List<ClientModel> getClientAssociations() {
        List<ClientModel> clients = new LinkedList<ClientModel>();
        for (String id : session.getClientAssociations()) {
            clients.add(realm.findClientById(id));
        }
        return clients;
    }

    @Override
    public void removeAssociatedClient(ClientModel client) {
        session.removeAssociatedClient(client.getId());
    }

}
