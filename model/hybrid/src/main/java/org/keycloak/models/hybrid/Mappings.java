package org.keycloak.models.hybrid;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.realms.Application;
import org.keycloak.models.realms.Client;
import org.keycloak.models.realms.OAuthClient;
import org.keycloak.models.realms.Realm;
import org.keycloak.models.realms.Role;
import org.keycloak.models.sessions.LoginFailure;
import org.keycloak.models.sessions.Session;
import org.keycloak.models.users.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Mappings {

    private final HybridModelProvider provider;
    private Map<Object, Object> mappings = new HashMap<Object, Object>();

    public Mappings(HybridModelProvider provider) {
        this.provider = provider;
    }

    public RealmModel wrap(Realm realm) {
        if (realm == null) return null;

        RealmAdapter adapter = (RealmAdapter) mappings.get(realm);
        if (adapter == null) {
            adapter = new RealmAdapter(provider, realm);
            mappings.put(realm, adapter);
        }
        return adapter;
    }

    public List<RealmModel> wrap(List<Realm> realms) {
        List<RealmModel> adapters = new LinkedList<RealmModel>();
        for (Realm realm : realms) {
            adapters.add(wrap(realm));
        }
        return adapters;
    }

    public RoleModel wrap(Role role) {
        if (role == null) return null;

        RoleAdapter adapter = (RoleAdapter) mappings.get(role);
        if (adapter == null) {
            adapter = new RoleAdapter(provider, role);
            mappings.put(role, adapter);
        }
        return adapter;
    }

    public Set<RoleModel> wrap(Set<Role> roles) {
        Set<RoleModel> adapters = new HashSet<RoleModel>();
        for (Role role : roles) {
            adapters.add(wrap(role));
        }
        return adapters;
    }

    public Role unwrap(RoleModel role) {
        if (role instanceof RoleAdapter) {
            return ((RoleAdapter) role).getRole();
        } else {
            return provider.realms().getRoleById(role.getId(), getRealm(role.getContainer()));
        }
    }

    public ApplicationModel wrap(Application application) {
        return application != null ? new ApplicationAdapter(provider, application) : null;
    }

    public List<ApplicationModel> wrapApps(List<Application> applications) {
       List<ApplicationModel> adapters = new LinkedList<ApplicationModel>();
        for (Application application : applications) {
            adapters.add(wrap(application));
        }
        return adapters;
    }

    public Map<String, ApplicationModel> wrap(Map<String, Application> applications) {
        Map<String, ApplicationModel> adapters = new HashMap<String, ApplicationModel>();
        for (Map.Entry<String, Application> e : applications.entrySet()) {
            adapters.put(e.getKey(), wrap(e.getValue()));
        }
        return adapters;
    }

    public OAuthClientModel wrap(OAuthClient client) {
        return client != null ? new OAuthClientAdapter(provider, client) : null;
    }

    public List<OAuthClientModel> wrapClients(List<OAuthClient> clients) {
       List<OAuthClientModel> adapters = new LinkedList<OAuthClientModel>();
        for (OAuthClient client : clients) {
            adapters.add(wrap(client));
        }
        return adapters;
    }

    public Client unwrap(ClientModel client) {
        if (client == null) {
            return null;
        }

        if (client instanceof ApplicationAdapter) {
            return ((ApplicationAdapter) client).getApplication();
        } else if (client instanceof OAuthClientAdapter) {
            return ((OAuthClientAdapter) client).getOauthClient();
        } else {
            throw new IllegalArgumentException("Not a hybrid model");
        }
    }

    public Application unwrap(ApplicationModel application) {
        if (application == null) {
            return null;
        }

        if (!(application instanceof ApplicationAdapter)) {
            throw new IllegalArgumentException("Not a hybrid model");
        }

        return ((ApplicationAdapter) application).getApplication();
    }

    public UserModel wrap(RealmModel realm, User user) {
        return user != null ? new UserAdapter(provider, realm, user) : null;
    }

    public List<UserModel> wrapUsers(RealmModel realm, List<User> users) {
        List<UserModel> adapters = new LinkedList<UserModel>();
        for (User user : users) {
            adapters.add(wrap(realm, user));
        }
        return adapters;
    }

    public static User unwrap(UserModel user) {
        if (user == null) {
            return null;
        }

        if (!(user instanceof UserAdapter)) {
            throw new IllegalArgumentException("Not a hybrid model");
        }

        return ((UserAdapter) user).getUser();
    }

    public UserSessionModel wrap(RealmModel realm, Session session) {
        return session != null ? new UserSessionAdapter(provider, realm, session) : null;
    }

    public List<UserSessionModel> wrapSessions(RealmModel realm, List<Session> sessions) {
        List<UserSessionModel> adapters = new LinkedList<UserSessionModel>();
        for (Session session : sessions) {
            adapters.add(wrap(realm, session));
        }
        return adapters;
    }

    public Set<UserSessionModel> wrapSessions(RealmModel realm, Set<Session> sessions) {
        Set<UserSessionModel> adapters = new HashSet<UserSessionModel>();
        for (Session session : sessions) {
            adapters.add(wrap(realm, session));
        }
        return adapters;
    }

    public Session unwrap(UserSessionModel session) {
        if (session == null) {
            return null;
        }

        if (!(session instanceof UserSessionAdapter)) {
            throw new IllegalArgumentException("Not a hybrid model");
        }

        return ((UserSessionAdapter) session).getSession();
    }

    public UsernameLoginFailureModel wrap(LoginFailure loginFailure) {
        return loginFailure != null ? new UsernameLoginFailureAdapter(provider, loginFailure) : null;
    }

    public List<UsernameLoginFailureModel> wrapLoginFailures(List<LoginFailure> loginFailures) {
        List<UsernameLoginFailureModel> adapters = new LinkedList<UsernameLoginFailureModel>();
        for (LoginFailure loginFailure : loginFailures) {
            adapters.add(wrap(loginFailure));
        }
        return adapters;
    }

    private String getRealm(RoleContainerModel container) {
        if (container instanceof RealmModel) {
            return ((RealmModel) container).getId();
        } else {
            return ((ApplicationModel) container).getRealm().getId();
        }
    }

}
