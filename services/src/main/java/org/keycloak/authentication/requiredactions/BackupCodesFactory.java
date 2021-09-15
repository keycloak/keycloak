package org.keycloak.authentication.requiredactions;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;

public class BackupCodesFactory implements RequiredActionFactory {

    private static final BackupCodes INSTANCE = new BackupCodes();

    @Override
    public String getDisplayText() {
        return "Backup Codes";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return INSTANCE;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }

    @Override
    public String getId() {
        return UserModel.RequiredAction.CONFIGURE_BACKUP_CODES.name();
    }

}
