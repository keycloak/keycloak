package org.keycloak.admin.ui.rest;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.keycloak.userprofile.UserProfileContext.USER_API;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.utils.StringUtil;

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
            Map<String, List<String>> managedAttributes = profile.getAttributes().getReadable();
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

            return attributes.entrySet().stream()
                    .filter(entry -> ofNullable(entry.getValue()).orElse(emptyList()).stream().anyMatch(StringUtil::isNotBlank))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        }

        return Collections.emptyMap();
    }

}
