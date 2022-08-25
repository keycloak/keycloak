package org.keycloak.forms.login.freemarker.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.MultivaluedMap;

import org.keycloak.models.KeycloakSession;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.AttributeValidatorMetadata;
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
        return attributes.keySet().stream().map(name -> profile.getAttributes().getMetadata(name)).filter((am) -> writeableOnly ? !profile.getAttributes().isReadOnly(am.getName()) : true).map(Attribute::new).sorted().collect(Collectors.toList());
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

        public String getGroup() {
            if (metadata.getAttributeGroupMetadata() != null) {
                return metadata.getAttributeGroupMetadata().getName();
            }
            return null;
        }

        public String getGroupDisplayHeader() {
            if (metadata.getAttributeGroupMetadata() != null) {
                return metadata.getAttributeGroupMetadata().getDisplayHeader();
            }
            return null;
        }

        public String getGroupDisplayDescription() {
            if (metadata.getAttributeGroupMetadata() != null) {
                return metadata.getAttributeGroupMetadata().getDisplayDescription();
            }
            return null;
        }

        public Map<String, Object> getGroupAnnotations() {

            if ((metadata.getAttributeGroupMetadata() == null) || (metadata.getAttributeGroupMetadata().getAnnotations() == null)) {
                return Collections.emptyMap();
            }
            
            return metadata.getAttributeGroupMetadata().getAnnotations();
        }

        @Override
        public int compareTo(Attribute o) {
            return Integer.compare(metadata.getGuiOrder(), o.metadata.getGuiOrder());
        }
    }
}
