package org.keycloak.models.utils;

import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum ImportUserActions {
    AS_IS {
        @Override
        public void addUserActions(UserRepresentation userRep, UserModel user, KeycloakSession session) {
            if (userRep.getRequiredActions() != null) {
                for (String requiredAction : userRep.getRequiredActions()) {
                    try {
                        user.addRequiredAction(UserModel.RequiredAction.valueOf(requiredAction.toUpperCase()));
                    } catch (IllegalArgumentException iae) {
                        user.addRequiredAction(requiredAction);
                    }
                }
            }
        }
    },
    ADD_DEFAULT_ACTIONS {
        @Override
        public void addUserActions(UserRepresentation userRep, UserModel user, KeycloakSession session) {
            List<String> reqActions = userRep.getRequiredActions();
            if (reqActions != null) {
                Set<String> allActions = new HashSet<>();
                for (ProviderFactory factory : session.getKeycloakSessionFactory().getProviderFactories(RequiredActionProvider.class)) {
                    allActions.add(factory.getId());
                }
                for (String action : allActions) {
                    if (reqActions.contains(action)) {
                        user.addRequiredAction(action);
                    }
                }
            }
            createActionOnNewCredentials(userRep, user);
        }
    },
    ADD_DEFAULT_ACTIONS_REMOVE_MISSING {
        @Override
        public void addUserActions(UserRepresentation userRep, UserModel user, KeycloakSession session) {
            List<String> reqActions = userRep.getRequiredActions();
            if (reqActions != null) {
                Set<String> allActions = new HashSet<>();
                for (ProviderFactory factory : session.getKeycloakSessionFactory().getProviderFactories(RequiredActionProvider.class)) {
                    allActions.add(factory.getId());
                }
                for (String action : allActions) {
                    if (reqActions.contains(action)) {
                        user.addRequiredAction(action);
                    } else {
                        user.removeRequiredAction(action);
                    }
                }
            }
            createActionOnNewCredentials(userRep, user);
        }
    };

    void createActionOnNewCredentials(UserRepresentation userRep, UserModel user) {
        List<CredentialRepresentation> credentials = userRep.getCredentials();
        if (credentials != null) {
            for (CredentialRepresentation credential : credentials) {
                if (CredentialRepresentation.PASSWORD.equals(credential.getType()) && credential.isTemporary() != null
                        && credential.isTemporary()) {
                    user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                }
            }
        }
    }

    abstract public void addUserActions(UserRepresentation userRep, UserModel user, KeycloakSession session);
}
