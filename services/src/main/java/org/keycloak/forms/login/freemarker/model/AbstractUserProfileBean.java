package org.keycloak.forms.login.freemarker.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

import org.keycloak.models.KeycloakSession;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;

/**
 * Abstract base for Freemarker context bean providing informations about user profile to render dynamic or crafted forms.  
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 */
public abstract class AbstractUserProfileBean {

    protected final MultivaluedMap<String, String> formData;
    protected UserProfile profile;
    protected List<Attribute> attributes;
    protected Map<String, Attribute> attributesByName;

    public AbstractUserProfileBean(MultivaluedMap<String, String> formData) {
        this.formData = formData;
    }
    
    /**
     * Subclass have to call this method at the end of constructor to init user profile data. 
     * 
     * @param session
     */
    protected void init(KeycloakSession session) {
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        this.profile = createUserProfile(provider);
        this.attributes = toAttributes(profile.getAttributes().getReadable());
        this.attributesByName = attributes.stream().collect(Collectors.toMap((a) -> a.getName(), (a) -> a));        
    }

    /**
     * Create UserProfile instance of the relevant type. Is called from {@link #init(KeycloakSession)}.
     * 
     * @param provider to create UserProfile from
     * @return user profile instance
     */
    protected abstract UserProfile createUserProfile(UserProfileProvider provider);

    /**
     * Get attribute default value to be pre-filled into the form on first show.
     * 
     * @param name of the attribute
     * @return attribute default value (can be null or empty list)
     */
    protected abstract List<String> getAttributeDefaultValue(String name);

    /**
     * All attributes to be shown in form sorted by the configured GUI order. Useful to render dynamic form.
     * 
     * @return list of attributes
     */
    public List<Attribute> getAttributes() {
        return attributes;
    }
    
    /**
     * Get map of all attributes where attribute name is key. Useful to render crafted form.
     * 
     * @return map of attributes by name
     */
    public Map<String, Attribute> getAttributesByName() {
        return attributesByName;
    }

    private List<Attribute> toAttributes(Map<String, List<String>> readable) {
        return readable.keySet().stream().map(name -> profile.getAttributes().getMetadata(name)).map(Attribute::new).sorted().collect(Collectors.toList());
    }

    /**
     * Info about user profile attribute available in Freemarker template. 
     */
    public class Attribute implements Comparable<Attribute> {

        private final AttributeMetadata metadata;

        public Attribute(AttributeMetadata metadata) {
            this.metadata = metadata;
        }

        public String getName() {
            return metadata.getName();
        }

        public String getDisplayName() {
            return metadata.getAttributeDisplayName();
        }

        public String getValue() {
            List<String> v = formData.getOrDefault(getName(), getAttributeDefaultValue(getName()));
            if (v == null || v.isEmpty()) {
                return null;
            }
            return v.get(0);
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
