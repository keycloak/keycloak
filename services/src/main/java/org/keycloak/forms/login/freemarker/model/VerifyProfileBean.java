package org.keycloak.forms.login.freemarker.model;

import static java.util.Collections.singletonList;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class VerifyProfileBean {

    private final UserModel user;
    private final MultivaluedMap<String, String> formData;
    private final List<Attribute> attributes;
    private final UserProfile profile;

    public VerifyProfileBean(UserModel user, MultivaluedMap<String, String> formData, KeycloakSession session) {
        this.user = user;
        this.formData = formData;
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        this.profile = provider.create(UserProfileContext.UPDATE_PROFILE, user);
        this.attributes = toAttributes(profile.getAttributes().getReadable());

    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public List<Attribute> getAllAttributes() {
        return toAttributes(profile.getAttributes().toMap());
    }

    private List<Attribute> toAttributes(Map<String, List<String>> readable) {
        return readable.keySet().stream()
                .map(name -> profile.getAttributes().getMetadata(name)).map(Attribute::new)
                .sorted()
                .collect(Collectors.toList());
    }

    public class Attribute implements Comparable<Attribute> {

        private final AttributeMetadata metadata;

        public Attribute(AttributeMetadata metadata) {
            this.metadata = metadata;
        }

        public String getName() {
            return metadata.getName();
        }

        public String getValue() {
            return formData.getOrDefault(getName(), singletonList(user.getFirstAttribute(getName()))).get(0);
        }

        public boolean isRequired() {
            return profile.getAttributes().isRequired(getName());
        }

        public boolean isReadOnly() {
            return profile.getAttributes().isReadOnly(getName());
        }

        public Map<String, Object> getAnnotations() {
            Map<String, Object> annotations = metadata.getAnnotations();

            if (annotations == null) {
                return Collections.emptyMap();
            }

            return annotations;
        }

        @Override
        public int compareTo(Attribute o) {
            return getName().compareTo(o.getName());
        }
    }
}
