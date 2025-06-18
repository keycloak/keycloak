package org.keycloak.broker.provider;

import org.jboss.logging.Logger;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public final class IdentityProviderMapperSyncModeDelegate {

    protected static final Logger logger = Logger.getLogger(IdentityProviderMapperSyncModeDelegate.class);

    public static void delegateUpdateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context, IdentityProviderMapper mapper) {
        IdentityProviderSyncMode effectiveSyncMode = combineIdpAndMapperSyncMode(context.getIdpConfig().getSyncMode(), mapperModel.getSyncMode());

        if (!mapper.supportsSyncMode(effectiveSyncMode)) {
            logger.warnf("The mapper %s does not explicitly support sync mode %s. Please ensure that the SPI supports the sync mode correctly and update it to reflect this.", mapper.getDisplayType(), effectiveSyncMode);
        }

        if (effectiveSyncMode == IdentityProviderSyncMode.LEGACY) {
            mapper.updateBrokeredUserLegacy(session, realm, user, mapperModel, context);
        } else if (effectiveSyncMode == IdentityProviderSyncMode.FORCE) {
            mapper.updateBrokeredUser(session, realm, user, mapperModel, context);
        }
    }

    public static IdentityProviderSyncMode combineIdpAndMapperSyncMode(IdentityProviderSyncMode syncMode, IdentityProviderMapperSyncMode mapperSyncMode) {
        return IdentityProviderMapperSyncMode.INHERIT.equals(mapperSyncMode) ? syncMode : IdentityProviderSyncMode.valueOf(mapperSyncMode.toString());
    }
}
