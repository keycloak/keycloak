package org.keycloak.models.workflow;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import org.jboss.logging.Logger;

public class UnlinkUserStepProvider implements WorkflowStepProvider {

    private final Logger log = Logger.getLogger(UnlinkUserStepProvider.class);
    public static final String CONFIG_ALIAS = "idp";

    private final KeycloakSession session;
    private final ComponentModel stepModel;

    public UnlinkUserStepProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.stepModel = model;
    }

    @Override
    public void close() {
    }

    @Override
    public void run(WorkflowExecutionContext context) {
        UserModel user = session.users().getUserById(getRealm(), context.getResourceId());
        getConfiguredProviders().forEach(alias -> UnlinkUserFromIdp(user, alias));
    }

    private void UnlinkUserFromIdp(UserModel user, String alias) {
        RealmModel realm = getRealm();
        // If alias is "*", unlink from all IdPs
        if ("*".equals(alias)) {
            log.debugv("Unlinking user {0} ({1}) from all Identity Providers.", user.getUsername(), user.getId());
            session.users()
                    .getFederatedIdentitiesStream(realm, user)
                    .forEach(identity -> session.users().removeFederatedIdentity(
                            realm,
                            user,
                            identity.getIdentityProvider()));
        } else {
            log.debugv("Unlinking user {0} ({1}) from Identity Provider with alias {2}.", user.getUsername(),
                    user.getId(), alias);
            session.users().removeFederatedIdentity(realm, user, alias);
        }
    }

    private Stream<String> getConfiguredProviders() {
        List<String> idpAliases = stepModel.getConfig().getOrDefault(CONFIG_ALIAS, List.of());
        if (idpAliases.isEmpty()) {
            log.warnv("Unlink operation skipped: no Identity Provider alias configured ({0}). " +
                    "Specify one or more Identity Provider aliases or '*' for all.", CONFIG_ALIAS);
            return Stream.of();
        }

        return idpAliases.stream().map(String::trim).filter(s -> !s.isEmpty());
    }

    private RealmModel getRealm() {
        return session.getContext().getRealm();
    }
}
