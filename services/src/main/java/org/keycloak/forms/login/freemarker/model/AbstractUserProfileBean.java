package org.keycloak.forms.login.freemarker.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.models.KeycloakSession;
import org.keycloak.userprofile.AttributeGroupMetadata;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.AttributeValidatorMetadata;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;
/**
 * Abstract base for Freemarker context bean providing information about user profile to render dynamic or crafted forms.  
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 */
public abstract class AbstractUserProfileBean {


    private static final Comparator<Attribute> ATTRIBUTE_COMPARATOR = (a1, a2) -> {
        AttributeGroup g1 = a1.getGroup();
        AttributeGroup g2 = a2.getGroup();

        if (g1 == null && g2 == null) {
            return a1.compareTo(a2);
        }

        if (g1 != null && g1.equals(g2)) {
            return a1.compareTo(a2);
        }

        return Comparator.nullsFirst(AttributeGroup::compareTo).compare(g1, g2);
    };

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
     * @param writeableOnly if true then only writeable (no read-only) attributes are put into template, if false then all readable attributes are there 
     */
    protected void init(KeycloakSession session, boolean writeableOnly) {
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        this.profile = createUserProfile(provider);
        this.attributes = toAttributes(profile.getAttributes().getReadable(), writeableOnly);
        if(this.attributes != null)
            this.attributesByName = attributes.stream().collect(Collectors.toMap((a) -> a.getName(), (a) -> a));        
    }

    /**
     * Create UserProfile instance of the relevant type. Is called from {@link #init(KeycloakSession, boolean)}.
     * 
     * @param provider to create UserProfile from
     * @return user profile instance
     */
    protected abstract UserProfile createUserProfile(UserProfileProvider provider);

    /**
     * Get attribute default values to be pre-filled into the form on first show.
     * 
     * @param name of the attribute
     * @return attribute default value (can be null)
     */
    protected abstract Stream<String> getAttributeDefaultValues(String name);

    /**
     * Get context the template is used for, so view can be customized for distinct contexts. 
     * 
     * @return name of the context
     */
    public abstract String getContext();
    
    /**
     * All attributes to be shown in form sorted by the configured GUI order. Useful to render dynamic form.
     * 
     * @return list of attributes
     */
    public List<Attribute> getAttributes() {
        return attributes;
    }

    public Map<String, Object> getHtml5DataAnnotations() {
        return getAttributes().stream().map(Attribute::getHtml5DataAnnotations)
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (l, r) -> l));
    }
    
    /**
     * Get map of all attributes where attribute name is key. Useful to render crafted form.
     * 
     * @return map of attributes by name
     */
    public Map<String, Attribute> getAttributesByName() {
        return attributesByName;
    }

    private List<Attribute> toAttributes(Map<String, List<String>> attributes, boolean writeableOnly) {
        if(attributes == null)
            return null;
        Attributes profileAttributes = profile.getAttributes();
        return attributes.keySet().stream().map(profileAttributes::getMetadata)
                .filter(Objects::nonNull)
                .filter((am) -> writeableOnly ? !profileAttributes.isReadOnly(am.getName()) : true)
                .filter((am) -> !profileAttributes.getUnmanagedAttributes().containsKey(am.getName()))
                .map(Attribute::new)
                .sorted(ATTRIBUTE_COMPARATOR)
                .collect(Collectors.toList());
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

        public boolean isMultivalued() {
            return metadata.isMultivalued();
        }

        public String getDefaultValue() {
            return metadata.getDefaultValue();
        }

        public String getValue() {
            List<String> v = getValues();
            if (v == null || v.isEmpty()) {
                return null;
            } else {
                return v.get(0);
            }
        }
        
        public List<String> getValues() {
            List<String> v = formData != null ? formData.get(getName()) : null;
            if (v == null || v.isEmpty()) {
                Stream<String> vs = getAttributeDefaultValues(getName());
                if(vs == null)
                    return Collections.emptyList();
                else
                    return vs.collect(Collectors.toList());
            } else {
                return v;
            }
        }

        public boolean isRequired() {
            return profile.getAttributes().isRequired(getName());
        }

        public boolean isReadOnly() {
            return profile.getAttributes().isReadOnly(getName());
        }
        
        /** define value of the autocomplete attribute for html input tag. if null then no html input tag attribute is added */
        public String getAutocomplete() {
            if(getName().equals("email") || getName().equals("username"))
                return getName();
            else
                return null;    
            
        }

        public Map<String, Object> getAnnotations() {
            Map<String, Object> annotations = metadata.getAnnotations();

            if (annotations == null) {
                return Collections.emptyMap();
            }

            return annotations;
        }

        public Map<String, Object> getHtml5DataAnnotations() {
            Map<String, Object> groupAnnotations = Optional.ofNullable(getGroup()).map(AttributeGroup::getAnnotations).orElse(Map.of());
            Map<String, Object> annotations = Stream.concat(getAnnotations().entrySet().stream(), groupAnnotations.entrySet().stream())
                    .filter((entry) -> entry.getKey().startsWith("kc")).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

            if (isMultivalued()) {
                annotations = new HashMap<>(annotations);
                annotations.put("kcMultivalued", "");
            }

            return annotations;
        }
      
        /**
         * Get info about validators applied to attribute.  
         * 
         * @return never null, map where key is validatorId and value is map with configuration for given validator (loaded from UserProfile configuration, never null)
         */
        public Map<String, Map<String, Object>> getValidators(){
            
            if(metadata.getValidators() == null) {
                return Collections.emptyMap();
            }
            return metadata.getValidators().stream().collect(Collectors.toMap(AttributeValidatorMetadata::getValidatorId, AttributeValidatorMetadata::getValidatorConfig));
        }

        public AttributeGroup getGroup() {
            AttributeGroupMetadata groupMetadata = metadata.getAttributeGroupMetadata();

            if (groupMetadata != null) {
                return new AttributeGroup(groupMetadata);
            }

            return null;
        }

        @Override
        public int compareTo(Attribute o) {
            return Integer.compare(metadata.getGuiOrder(), o.metadata.getGuiOrder());
        }
    }

    public class AttributeGroup implements Comparable<AttributeGroup> {

        private AttributeGroupMetadata metadata;

        AttributeGroup(AttributeGroupMetadata metadata) {
            this.metadata = metadata;
        }

        public String getName() {
            return metadata.getName();
        }

        public String getDisplayHeader() {
            return Optional.ofNullable(metadata.getDisplayHeader()).orElse(getName());
        }

        public String getDisplayDescription() {
            return metadata.getDisplayDescription();
        }

        public Map<String, Object> getAnnotations() {
            return Optional.ofNullable(metadata.getAnnotations()).orElse(Map.of());
        }

        public Map<String, Object> getHtml5DataAnnotations() {
            return getAnnotations().entrySet().stream()
                    .filter((entry) -> entry.getKey().startsWith("kc")).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AttributeGroup that = (AttributeGroup) o;
            return Objects.equals(metadata.getName(), that.metadata.getName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(metadata);
        }

        @Override
        public String toString() {
            return metadata.getName();
        }

        @Override
        public int compareTo(AttributeGroup o) {
            return getDisplayHeader().compareTo(o.getDisplayHeader());
        }
    }
}
