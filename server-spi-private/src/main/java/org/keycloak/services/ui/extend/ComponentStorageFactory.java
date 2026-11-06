package org.keycloak.services.ui.extend;

import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * Optional storage backend for declarative UI extensions that should not use the default component table.
 */
public interface ComponentStorageFactory {

    Stream<ComponentModel> listComponents(KeycloakSession session, RealmModel realm, String parentId, String providerId);

    ComponentModel getComponent(KeycloakSession session, RealmModel realm, String id);

    ComponentModel createComponent(KeycloakSession session, RealmModel realm, ComponentModel model);

    ComponentModel updateComponent(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel);

    void removeComponent(KeycloakSession session, RealmModel realm, ComponentModel model);
}
