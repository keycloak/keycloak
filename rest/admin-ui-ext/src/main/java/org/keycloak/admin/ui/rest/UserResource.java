package org.keycloak.admin.ui.rest;

import static org.keycloak.userprofile.UserProfileContext.USER_API;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserResource {

    private final KeycloakSession session;
    private final UserModel user;

    public UserResource(KeycloakSession session, UserModel user) {
        this.session = session;
        this.user = user;
    }

    @GET
    @Path("unmanagedAttributes")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> getUnmanagedAttributes() {
        RealmModel realm = session.getContext().getRealm();
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);

        if (provider.isEnabled(realm)) {
            UserProfile profile = provider.create(USER_API, user);
            Map<String, List<String>> managedAttributes = profile.getAttributes().getReadable(false);
            Map<String, List<String>> attributes = new HashMap<>(user.getAttributes());
            UPConfig upConfig = provider.getConfiguration();

            if (upConfig.getUnmanagedAttributePolicy() == null) {
                return Collections.emptyMap();
            }

            Map<String, List<String>> unmanagedAttributes = profile.getAttributes().getUnmanagedAttributes();
            managedAttributes.entrySet().removeAll(unmanagedAttributes.entrySet());
            attributes.entrySet().removeAll(managedAttributes.entrySet());

            attributes.remove(UserModel.USERNAME);
            attributes.remove(UserModel.EMAIL);
            attributes.remove(UserModel.FIRST_NAME);
            attributes.remove(UserModel.LAST_NAME);

            return attributes;
        }

        return Collections.emptyMap();
    }

}
