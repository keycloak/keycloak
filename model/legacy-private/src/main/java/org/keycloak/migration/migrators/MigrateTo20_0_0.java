package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RealmRepresentation;

public class MigrateTo20_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("20.0.0");

    @Override
    public void migrate(KeycloakSession session) {

        session.realms().getRealmsStream().forEach(realm -> {
           addViewGroupsRole(realm);
           addAccountRoles(realm);
        });
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        addViewGroupsRole(realm);
        addAccountRoles(realm);
    }

    private void addViewGroupsRole(RealmModel realm) {
        ClientModel accountClient = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (accountClient != null && accountClient.getRole(AccountRoles.VIEW_GROUPS) == null) {
            RoleModel viewGroupsRole = accountClient.addRole(AccountRoles.VIEW_GROUPS);
            viewGroupsRole.setDescription("${role_" + AccountRoles.VIEW_GROUPS + "}");
            ClientModel accountConsoleClient = realm.getClientByClientId(Constants.ACCOUNT_CONSOLE_CLIENT_ID);
            accountConsoleClient.addScopeMapping(viewGroupsRole);
        }
    }

    private void addAccountRoles(RealmModel realm) {
        ClientModel accountClient = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        RoleModel manageAccount = accountClient.getRole(AccountRoles.MANAGE_ACCOUNT);
        if (accountClient != null && accountClient.getRole(AccountRoles.MANAGE_ACCOUNT_BASIC_AUTH) == null) {
            RoleModel manageAccountBasicAuth = accountClient.addRole(AccountRoles.MANAGE_ACCOUNT_BASIC_AUTH);
            manageAccountBasicAuth.setDescription("${role_" + AccountRoles.MANAGE_ACCOUNT_BASIC_AUTH + "}");
            manageAccount.addCompositeRole(manageAccountBasicAuth);
        }
        if (accountClient != null && accountClient.getRole(AccountRoles.MANAGE_ACCOUNT_2FA) == null) {
            RoleModel manageAccount2fa = accountClient.addRole(AccountRoles.MANAGE_ACCOUNT_2FA);
            manageAccount2fa.setDescription("${role_" + AccountRoles.MANAGE_ACCOUNT_2FA + "}");
            manageAccount.addCompositeRole(manageAccount2fa);
        }
        ClientModel accountConsoleClient = realm.getClientByClientId(Constants.ACCOUNT_CONSOLE_CLIENT_ID);
        accountConsoleClient.addScopeMapping(accountClient.getRole(AccountRoles.DELETE_ACCOUNT));
        accountConsoleClient.addScopeMapping(accountClient.getRole(AccountRoles.MANAGE_ACCOUNT_2FA));
        accountConsoleClient.addScopeMapping(accountClient.getRole(AccountRoles.MANAGE_ACCOUNT_BASIC_AUTH));
        accountConsoleClient.addScopeMapping(accountClient.getRole(AccountRoles.MANAGE_ACCOUNT_LINKS));
        accountConsoleClient.addScopeMapping(accountClient.getRole(AccountRoles.MANAGE_CONSENT));
        accountConsoleClient.addScopeMapping(accountClient.getRole(AccountRoles.VIEW_APPLICATIONS));
        accountConsoleClient.addScopeMapping(accountClient.getRole(AccountRoles.VIEW_CONSENT));
        accountConsoleClient.addScopeMapping(accountClient.getRole(AccountRoles.VIEW_PROFILE));
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
