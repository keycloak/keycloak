package org.keycloak.migration.migrators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.UserStorageProvider;

public class MigrateTo26_7_0 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("26.7.0");

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }


    @Override
    public void migrate(KeycloakSession session) {
        // addOrganizationAdminRoles accesses cross-realm entities (master admin client)
        // which fails with LazyInitializationException when RealmMigration clears the
        // persistence context between realms. Handle it in a separate pass.
        session.realms().getRealmsStream().forEach(this::addOrganizationAdminRoles);
        super.migrate(session);
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep,
            boolean skipUserDependent) {
        addOrganizationAdminRoles(realm);
        super.migrateImport(session, realm, rep, skipUserDependent);
    }

    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        updatePasswordAfterEmailVerificationDuringRegistrationOfUsers(realm);
        updateAdminPermissionsSchema(session, realm);
        renameDynamicScopeAttributes(realm);
        migrateParameterizedScopeTypes(realm);
        migrateLdapBinaryAttributeMappers(realm);
        migrateLdapGroupAttributeMappers(realm);
    }

    private void migrateParameterizedScopeTypes(RealmModel realm) {
        realm.getClientScopesStream()
                .filter(scope -> scope.isParameterizedScope())
                .filter(scope -> scope.getAttribute(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE) == null)
                .forEach(scope -> {
                    String regexp = scope.getParameterizedScopeRegexp();
                    if (regexp == null) {
                        LOG.warnf("Parameterized client scope '%s' has no regexp. Defaulting to 'string' type.", scope.getName());
                        scope.setAttribute(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE, "string");
                        return;
                    }

                    String prefix = scope.getName() + ":";
                    String defaultRegexp = prefix + "*";
                    if (defaultRegexp.equals(regexp)) {
                        scope.setAttribute(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE, "string");
                        scope.removeAttribute(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP);
                    } else if (regexp.startsWith(prefix)) {
                        scope.setAttribute(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE, "custom");
                        scope.setAttribute(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP, regexp.substring(prefix.length()));
                    } else {
                        LOG.warnf("Parameterized client scope '%s' has a regexp '%s' that does not start with the expected prefix '%s'. Please migrate this scope manually.", scope.getName(), regexp, prefix);
                    }
                });
    }

    private void updatePasswordAfterEmailVerificationDuringRegistrationOfUsers(RealmModel realm) {
        Map<String, RequiredActionProviderModel> reqActionsByAlias = new HashMap<>();
        List<Integer> reqActionPriorities = new ArrayList<>();
        realm.getRequiredActionProvidersStream().forEach((reqAction) -> {
            reqActionsByAlias.put(reqAction.getAlias(), reqAction);
            reqActionPriorities.add(reqAction.getPriority());
        });

        RequiredActionProviderModel verifyEmail = reqActionsByAlias.get(UserModel.RequiredAction.VERIFY_EMAIL.name());
        RequiredActionProviderModel configureTotp = reqActionsByAlias.get(UserModel.RequiredAction.CONFIGURE_TOTP.name());
        RequiredActionProviderModel updatePassword = reqActionsByAlias.get(UserModel.RequiredAction.UPDATE_PASSWORD.name());

        if (verifyEmail == null) {
            return;
        }

        // Default case when admin did not changed anything. Set priorities same way like in DefaultRequiredActions
        if (configureTotp != null && updatePassword != null &&
                verifyEmail.getPriority() == 50 && configureTotp.getPriority() == 10 && updatePassword.getPriority() == 30) {
            configureTotp.setPriority(54);
            realm.updateRequiredActionProvider(configureTotp);
            updatePassword.setPriority(57);
            realm.updateRequiredActionProvider(updatePassword);
        } else {
            // Case when admin changed priorities of required actions. Add configureTotp and updatePassword to the first free places after verifyEmail
            int nextAvailablePriority = getFirstAvailablePriorityAfter(verifyEmail.getPriority(), reqActionPriorities);
            if (configureTotp != null) {
                configureTotp.setPriority(nextAvailablePriority);
                realm.updateRequiredActionProvider(configureTotp);
                nextAvailablePriority = getFirstAvailablePriorityAfter(nextAvailablePriority, reqActionPriorities);
            }
            if (updatePassword != null) {
                updatePassword.setPriority(nextAvailablePriority);
                realm.updateRequiredActionProvider(updatePassword);
            }
        }
    }

    private int getFirstAvailablePriorityAfter(int priority, List<Integer> reqActionPriorities) {
        for (int i = priority + 1 ; i < (priority + reqActionPriorities.size() + 2) ; i++) {
            if (!reqActionPriorities.contains(i)) {
                return i;
            }
        }

        // Should not happen
        return reqActionPriorities.get(reqActionPriorities.size() - 1) + 1;
    }

    private void addOrganizationAdminRoles(RealmModel realm) {
        MigrationUtils.addAdminRole(realm, AdminRoles.VIEW_ORGANIZATIONS);
        MigrationUtils.addAdminRole(realm, AdminRoles.MANAGE_ORGANIZATIONS);
        MigrationUtils.addAdminRole(realm, AdminRoles.QUERY_ORGANIZATIONS);

        addQueryCompositeRoles(realm.getMasterAdminClient());
        if (!realm.getName().equals(Config.getAdminRealm())) {
            addQueryCompositeRoles(realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID));
        }
    }

    private void addQueryCompositeRoles(ClientModel client) {
        if (client == null) return;
        RoleModel viewOrganizations = client.getRole(AdminRoles.VIEW_ORGANIZATIONS);
        RoleModel queryOrganizations = client.getRole(AdminRoles.QUERY_ORGANIZATIONS);
        if (viewOrganizations != null && queryOrganizations != null && !viewOrganizations.hasRole(queryOrganizations)) {
            viewOrganizations.addCompositeRole(queryOrganizations);
        }
    }

    private void updateAdminPermissionsSchema(KeycloakSession session, RealmModel realm) {
        if (realm.getAdminPermissionsClient() != null) {
            AdminPermissionsSchema.SCHEMA.init(session, realm);
        }
    }

    private void renameDynamicScopeAttributes(RealmModel realm) {
        realm.getClientScopesStream().forEach(clientScope -> {
            renameAttribute(clientScope, "is.dynamic.scope", ClientScopeModel.IS_PARAMETERIZED_SCOPE);
            renameAttribute(clientScope, "dynamic.scope.regexp", ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP);
        });
    }

    private void renameAttribute(ClientScopeModel clientScope, String oldName, String newName) {
        String value = clientScope.getAttribute(oldName);
        if (value != null) {
            clientScope.setAttribute(newName, value);
            clientScope.removeAttribute(oldName);
        }
    }

    private void migrateLdapBinaryAttributeMappers(RealmModel realm) {
        realm.getComponentsStream(realm.getId(), UserStorageProvider.class.getName())
                .filter(c -> LDAPConstants.LDAP_PROVIDER.equals(c.getProviderId()))
                .flatMap(ldap -> realm.getComponentsStream(ldap.getId(), "org.keycloak.storage.ldap.mappers.LDAPStorageMapper"))
                .filter(c -> "user-attribute-ldap-mapper".equals(c.getProviderId()))
                .filter(c -> "true".equals(c.getConfig().getFirst("is.binary.attribute")))
                .filter(c -> c.getConfig().getFirst("binary.attribute.decoder") == null)
                .forEach(c -> {
                    c.getConfig().putSingle("binary.attribute.decoder", "base64");
                    realm.updateComponent(c);
                });
    }

    private void migrateLdapGroupAttributeMappers(RealmModel realm) {
        realm.getComponentsStream(realm.getId(), UserStorageProvider.class.getName())
                .filter(c -> LDAPConstants.LDAP_PROVIDER.equals(c.getProviderId()))
                .flatMap(ldap -> realm.getComponentsStream(ldap.getId(), "org.keycloak.storage.ldap.mappers.LDAPStorageMapper"))
                .filter(c -> "group-ldap-mapper".equals(c.getProviderId()))
                .filter(c -> c.getConfig().getFirst("decode.group.uuid.attribute") == null)
                .forEach(c -> {
                    c.getConfig().putSingle("decode.group.uuid.attribute", "false");
                    realm.updateComponent(c);
                });
    }
}
