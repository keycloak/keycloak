package org.keycloak.testsuite.util.userprofile;

import java.io.IOException;
import java.util.Set;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPAttributeRequired;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.util.JsonSerialization;

import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;

public class UserProfileUtil {

    public static final String SCOPE_DEPARTMENT = "department";
    public static final String ATTRIBUTE_DEPARTMENT = "department";

    public static final String PERMISSIONS_ALL = "\"permissions\": {\"view\": [\"admin\", \"user\"], \"edit\": [\"admin\", \"user\"]}";
    public static final String PERMISSIONS_ADMIN_ONLY = "\"permissions\": {\"view\": [\"admin\"], \"edit\": [\"admin\"]}";
    public static final String PERMISSIONS_ADMIN_EDITABLE = "\"permissions\": {\"view\": [\"admin\", \"user\"], \"edit\": [\"admin\"]}";

    public static String VALIDATIONS_LENGTH = "\"validations\": {\"length\": { \"min\": 3, \"max\": 255 }}";

    public static final String CONFIGURATION_FOR_USER_EDIT = "{\"attributes\": ["
            + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + "},"
            + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
            + "{\"name\": \"department\"," + PERMISSIONS_ALL + "}"
            + "]}";

    public static UPConfig setUserProfileConfiguration(RealmResource testRealm, String configuration) {
        try {
            UPConfig config = configuration == null ? null : JsonSerialization.readValue(configuration, UPConfig.class);

            if (config != null) {
                UPAttribute username = config.getAttribute(UserModel.USERNAME);

                if (username == null) {
                    config.addOrReplaceAttribute(new UPAttribute(UserModel.USERNAME));
                }

                UPAttribute email = config.getAttribute(UserModel.EMAIL);

                if (email == null) {
                    config.addOrReplaceAttribute(new UPAttribute(UserModel.EMAIL, new UPAttributePermissions(Set.of(ROLE_USER, ROLE_ADMIN), Set.of(ROLE_USER, ROLE_ADMIN)), new UPAttributeRequired(Set.of(ROLE_USER), Set.of())));
                }
            }

            testRealm.users().userProfile().update(config);

            return config;
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to read configuration", ioe);
        }
    }

    public static UPConfig enableUnmanagedAttributes(UserProfileResource upResource) {
        UPConfig cfg = upResource.getConfiguration();
        cfg.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        upResource.update(cfg);
        return cfg;
    }

}
