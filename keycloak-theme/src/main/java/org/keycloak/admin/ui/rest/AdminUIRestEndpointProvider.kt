package org.keycloak.admin.ui.rest

import org.keycloak.Config
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory
import org.keycloak.models.RealmModel
import org.keycloak.services.resources.admin.AdminEventBuilder
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator

class AdminUIRestEndpointProvider : AdminRealmResourceProviderFactory, AdminRealmResourceProvider {
    override fun create(session: KeycloakSession): AdminRealmResourceProvider {
        return this
    }

    override fun init(config: Config.Scope) {}
    override fun postInit(factory: KeycloakSessionFactory) {}
    override fun close() {}
    override fun getId(): String {
        return "admin-ui"
    }

    override fun getResource(
        session: KeycloakSession,
        realm: RealmModel,
        auth: AdminPermissionEvaluator,
        adminEvent: AdminEventBuilder
    ): Any {
        return AdminUIExtendedResource(realm, auth)
    }
}