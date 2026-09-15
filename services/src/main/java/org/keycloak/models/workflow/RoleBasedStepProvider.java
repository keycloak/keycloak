package org.keycloak.models.workflow;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import org.jboss.logging.Logger;

public abstract class RoleBasedStepProvider implements WorkflowStepProvider {

    private final Logger log = Logger.getLogger(RoleBasedStepProvider.class);
    public static final String CONFIG_ROLE = "role";

    private final KeycloakSession session;
    private final ComponentModel model;

    public RoleBasedStepProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
    }

    @Override
    public void run(WorkflowExecutionContext context) {
        UserModel user = session.users().getUserById(getRealm(), context.getResourceId());

        if (user != null) {
            try {
                getRoles().forEach(role -> run(user, role));
            } catch (Exception e) {
                log.errorf(e, "Failed to grant role to user %s", user.getId());
            }
        }
    }

    protected abstract void run(UserModel user, RoleModel role);

    @Override
    public void close() {
    }

    private Stream<RoleModel> getRoles() {
        return model.getConfig().getOrDefault(CONFIG_ROLE, List.of()).stream().map(this::getRole);
    }

    private RoleModel getRole(String name) {
        RoleModel role;
        String[] parts = name.split("/");

        if (parts.length > 1) {
            ClientModel client = getRealm().getClientByClientId(parts[0]);

            if (client == null) {
                throw new IllegalStateException("Client with clientId " + parts[0] + " not found");
            }

            role = client.getRole(parts[1]);
        } else {
            role = getRealm().getRole(name);
        }

        if (role == null) {
            throw new IllegalStateException("Role " + name + " not found");
        }

        return role;
    }

    private RealmModel getRealm() {
        return session.getContext().getRealm();
    }
}
