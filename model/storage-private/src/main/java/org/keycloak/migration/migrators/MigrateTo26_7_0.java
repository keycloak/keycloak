package org.keycloak.migration.migrators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;

public class MigrateTo26_7_0 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("26.7.0");

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }


    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
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
}
