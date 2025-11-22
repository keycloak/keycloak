package org.keycloak.testframework.realm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public class RealmConfigBuilder {

    private final RealmRepresentation rep;

    private RealmConfigBuilder(RealmRepresentation rep) {
        this.rep = rep;
    }

    public static RealmConfigBuilder create() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setEnabled(true);
        return new RealmConfigBuilder(rep);
    }

    public static RealmConfigBuilder update(RealmRepresentation rep) {
        return new RealmConfigBuilder(rep);
    }

    public RealmConfigBuilder name(String name) {
        rep.setRealm(name);
        return this;
    }

    public RealmConfigBuilder displayName(String displayName) {
        rep.setDisplayName(displayName);
        return this;
    }

    public RealmConfigBuilder client(ClientRepresentation client) {
        rep.setClients(Collections.combine(rep.getClients(), client));
        return this;
    }

    public ClientConfigBuilder addClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();
        rep.setClients(Collections.combine(rep.getClients(), client));
        return ClientConfigBuilder.update(client).enabled(true).clientId(clientId);
    }

    public UserConfigBuilder addUser(String username) {
        UserRepresentation user = new UserRepresentation();
        rep.setUsers(Collections.combine(rep.getUsers(), user));
        return UserConfigBuilder.update(user).enabled(true).username(username);
    }

    public GroupConfigBuilder addGroup(String name) {
        GroupRepresentation group = new GroupRepresentation();
        rep.setGroups(Collections.combine(rep.getGroups(), group));
        return GroupConfigBuilder.update(group).name(name);
    }

    public RoleConfigBuilder addRole(String name) {
        if (rep.getRoles() == null) {
            rep.setRoles(new RolesRepresentation());
        }

        RoleRepresentation role = new RoleRepresentation();
        rep.getRoles().setRealm(Collections.combine(rep.getRoles().getRealm(), role));
        return RoleConfigBuilder.update(role).name(name);
    }

    public RoleConfigBuilder addClientRole(String clientName, String roleName) {
        if (rep.getRoles() == null) {
            rep.setRoles(new RolesRepresentation());
        }

        RoleRepresentation role = new RoleRepresentation();
        rep.getRoles().setClient(Collections.combine(rep.getRoles().getClient(), clientName, role));
        return RoleConfigBuilder.update(role).name(roleName);
    }

    public RealmConfigBuilder registrationEmailAsUsername(boolean registrationEmailAsUsername) {
        rep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
        return this;
    }

    public RealmConfigBuilder editUsernameAllowed(boolean allowed) {
        rep.setEditUsernameAllowed(allowed);
        return this;
    }

    public RealmConfigBuilder defaultSignatureAlgorithm(String algorithm) {
        rep.setDefaultSignatureAlgorithm(algorithm);
        return this;
    }

    public RealmConfigBuilder adminPermissionsEnabled(boolean enabled) {
        rep.setAdminPermissionsEnabled(enabled);
        return this;
    }

    public RealmConfigBuilder eventsEnabled(boolean enabled) {
        rep.setEventsEnabled(enabled);
        return this;
    }

    public RealmConfigBuilder adminEventsEnabled(boolean enabled) {
        rep.setAdminEventsEnabled(enabled);
        return this;
    }

    public RealmConfigBuilder adminEventsDetailsEnabled(boolean enabled) {
        rep.setAdminEventsDetailsEnabled(enabled);
        return this;
    }

    public RealmConfigBuilder enabledEventTypes(String... enabledEventTypes) {
        rep.setEnabledEventTypes(Collections.combine(rep.getEnabledEventTypes(), enabledEventTypes));
        return this;
    }

    public RealmConfigBuilder setEnabledEventTypes(String... enabledEventTypes) {
        rep.setEnabledEventTypes(List.of(enabledEventTypes));
        return this;
    }

    public RealmConfigBuilder eventsListeners(String... eventListeners) {
        rep.setEventsListeners(Collections.combine(rep.getEventsListeners(), eventListeners));
        return this;
    }

    public RealmConfigBuilder overwriteEventsListeners(String... eventListeners) {
        rep.setEventsListeners(List.of(eventListeners));
        return this;
    }

    public RealmConfigBuilder eventsExpiration(long eventsExpiration) {
        rep.setEventsExpiration(eventsExpiration);
        return this;
    }

    public RealmConfigBuilder roles(String... roleNames) {
        if (rep.getRoles() == null) {
            rep.setRoles(new RolesRepresentation());
        }
        rep.getRoles().setRealm(Collections.combine(
                rep.getRoles().getRealm(),
                Arrays.stream(roleNames).map(r -> Representations.toRole(r, false))
        ));
        return this;
    }

    public RealmConfigBuilder clientRoles(String client, String... clientRoles) {
        if (rep.getRoles() == null) {
            rep.setRoles(new RolesRepresentation());
        }
        rep.getRoles().setClient(Collections.combine(
                rep.getRoles().getClient(),
                client,
                Arrays.stream(clientRoles).map(r -> Representations.toRole(r, true))
        ));
        return this;
    }

    public RealmConfigBuilder groups(String... groupsNames) {
        rep.setGroups(Collections.combine(rep.getGroups(), Arrays.stream(groupsNames).map(Representations::toGroup)));
        return this;
    }

    public RealmConfigBuilder defaultGroups(String... groupsNames) {
        rep.setDefaultGroups(Collections.combine(rep.getDefaultGroups(), groupsNames));
        return this;
    }

    public RealmConfigBuilder internationalizationEnabled(boolean enabled) {
        rep.setInternationalizationEnabled(enabled);
        return this;
    }

    public RealmConfigBuilder supportedLocales(String... supportedLocales) {
        rep.setSupportedLocales(Collections.combine(rep.getSupportedLocales(), supportedLocales));
        return this;
    }

    public RealmConfigBuilder defaultLocale(String locale) {
        rep.setDefaultLocale(locale);
        return this;
    }

    public RealmConfigBuilder smtp(String host, int port, String from) {
        Map<String, String> config = new HashMap<>();
        config.put("host", host);
        config.put("port", Integer.toString(port));
        config.put("from", from);
        rep.setSmtpServer(config);
        return this;
    }

    public RealmConfigBuilder organizationsEnabled(boolean enabled) {
        rep.setOrganizationsEnabled(enabled);
        return this;
    }

    public RealmConfigBuilder bruteForceProtected(boolean enabled) {
        rep.setBruteForceProtected(enabled);
        return this;
    }

    public RealmConfigBuilder failureFactor(int count) {
        rep.setFailureFactor(count);
        return this;
    }

    public RealmConfigBuilder duplicateEmailsAllowed(boolean allowed) {
        rep.setDuplicateEmailsAllowed(allowed);
        return this;
    }

    public RealmConfigBuilder sslRequired(String sslRequired) {
        rep.setSslRequired(sslRequired);
        return this;
    }

    public RealmConfigBuilder identityProvider(IdentityProviderRepresentation identityProvider) {
        rep.addIdentityProvider(identityProvider);
        return this;
    }

    public RealmConfigBuilder setRememberMe(boolean enabled) {
        rep.setRememberMe(enabled);
        return this;
    }

    /**
     * Best practice is to use other convenience methods when configuring a realm, but while the framework is under
     * active development there may not be a way to perform all updates required. In these cases this method allows
     * applying any changes to the underlying representation.
     *
     * @param update
     * @return this
     * @deprecated
     */
    public RealmConfigBuilder update(RealmUpdate... update) {
        Arrays.stream(update).forEach(u -> u.update(rep));
        return this;
    }

    public RealmRepresentation build() {
        return rep;
    }

    public interface RealmUpdate {

        void update(RealmRepresentation realm);

    }

}
