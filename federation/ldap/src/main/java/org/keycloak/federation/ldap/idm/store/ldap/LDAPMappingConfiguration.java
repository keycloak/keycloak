package org.keycloak.federation.ldap.idm.store.ldap;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.federation.ldap.idm.model.Attribute;
import org.keycloak.federation.ldap.idm.model.AttributedType;
import org.keycloak.federation.ldap.idm.model.IdentityType;
import org.keycloak.models.ModelException;
import org.keycloak.models.utils.reflection.NamedPropertyCriteria;
import org.keycloak.models.utils.reflection.Property;
import org.keycloak.models.utils.reflection.PropertyQueries;

/**
 * @author pedroigor
 */
public class LDAPMappingConfiguration {

    private final Class<? extends AttributedType> mappedClass;
    private Set<String> objectClasses;
    private String baseDN;
    private final Map<String, String> mappedProperties = new HashMap<String, String>();
    private Property<String> idProperty;
    private Class<? extends AttributedType> relatedAttributedType;
    private String parentMembershipAttributeName;
    private Map<String, String> parentMapping = new HashMap<String, String>();
    private final Set<String> readOnlyAttributes = new HashSet<String>();
    private int hierarchySearchDepth;
    private Property<String> bindingProperty;

    public LDAPMappingConfiguration(Class<? extends AttributedType> mappedClass) {
        this.mappedClass = mappedClass;
    }

    public Class<? extends AttributedType> getMappedClass() {
        return this.mappedClass;
    }

    public Set<String> getObjectClasses() {
        return this.objectClasses;
    }

    public String getBaseDN() {
        return this.baseDN;
    }

    public Map<String, String> getMappedProperties() {
        return this.mappedProperties;
    }

    public Property<String> getIdProperty() {
        return this.idProperty;
    }

    public Property<String> getBindingProperty() {
        return this.bindingProperty;
    }

    public Class<? extends AttributedType> getRelatedAttributedType() {
        return this.relatedAttributedType;
    }

    public String getParentMembershipAttributeName() {
        return this.parentMembershipAttributeName;
    }

    public Map<String, String> getParentMapping() {
        return this.parentMapping;
    }

    public Set<String> getReadOnlyAttributes() {
        return this.readOnlyAttributes;
    }

    public int getHierarchySearchDepth() {
        return this.hierarchySearchDepth;
    }

    private Property getBindingProperty(final String bindingPropertyName) {
        Property bindingProperty = PropertyQueries
                .<String>createQuery(getMappedClass())
                .addCriteria(new NamedPropertyCriteria(bindingPropertyName)).getFirstResult();

        // We don't have Java property, so actually delegate to setAttribute/getAttribute
        if (bindingProperty == null) {
            bindingProperty = new Property<String>() {

                @Override
                public String getName() {
                    return bindingPropertyName;
                }

                @Override
                public Type getBaseType() {
                    return null;
                }

                @Override
                public Class<String> getJavaClass() {
                    return String.class;
                }

                @Override
                public AnnotatedElement getAnnotatedElement() {
                    return null;
                }

                @Override
                public Member getMember() {
                    return null;
                }

                @Override
                public String getValue(Object instance) {
                    if (!(instance instanceof AttributedType)) {
                        throw new IllegalStateException("Instance [ " + instance + " ] not an instance of AttributedType");
                    }

                    AttributedType attributedType = (AttributedType) instance;
                    Attribute<String> attr = attributedType.getAttribute(bindingPropertyName);
                    return attr!=null ? attr.getValue() : null;
                }

                @Override
                public void setValue(Object instance, String value) {
                    if (!(instance instanceof AttributedType)) {
                        throw new IllegalStateException("Instance [ " + instance + " ] not an instance of AttributedType");
                    }

                    AttributedType attributedType = (AttributedType) instance;
                    attributedType.setAttribute(new Attribute(bindingPropertyName, value));
                }

                @Override
                public Class<?> getDeclaringClass() {
                    return null;
                }

                @Override
                public boolean isReadOnly() {
                    return false;
                }

                @Override
                public void setAccessible() {

                }

                @Override
                public boolean isAnnotationPresent(Class annotation) {
                    return false;
                }
            };
        }

        return bindingProperty;
    }

    public LDAPMappingConfiguration setObjectClasses(Set<String> objectClasses) {
        this.objectClasses = objectClasses;
        return this;
    }

    public LDAPMappingConfiguration setBaseDN(String baseDN) {
        this.baseDN = baseDN;
        return this;
    }

    public LDAPMappingConfiguration addAttributeMapping(String userAttributeName, String ldapAttributeName) {
        this.mappedProperties.put(userAttributeName, ldapAttributeName);
        return this;
    }

    public LDAPMappingConfiguration addReadOnlyAttributeMapping(String userAttributeName, String ldapAttributeName) {
        this.mappedProperties.put(userAttributeName, ldapAttributeName);
        this.readOnlyAttributes.add(userAttributeName);
        return this;
    }

    public LDAPMappingConfiguration setIdPropertyName(String idPropertyName) {

        if (idPropertyName != null) {
            this.idProperty = PropertyQueries
                    .<String>createQuery(getMappedClass())
                    .addCriteria(new NamedPropertyCriteria(idPropertyName)).getFirstResult();
        } else {
            this.idProperty = null;
        }

        if (IdentityType.class.isAssignableFrom(mappedClass) && idProperty == null) {
            throw new ModelException("Id attribute not mapped to any property of [" + mappedClass + "].");
        }

        // Binding property is idProperty by default
        if (this.bindingProperty == null) {
            this.bindingProperty = this.idProperty;
        }

        return this;
    }

    public LDAPMappingConfiguration setRelatedAttributedType(Class<? extends AttributedType> relatedAttributedType) {
        this.relatedAttributedType = relatedAttributedType;
        return this;
    }

    public LDAPMappingConfiguration setParentMembershipAttributeName(String parentMembershipAttributeName) {
        this.parentMembershipAttributeName = parentMembershipAttributeName;
        return this;
    }

    public LDAPMappingConfiguration setParentMapping(Map<String, String> parentMapping) {
        this.parentMapping = parentMapping;
        return this;
    }

    public LDAPMappingConfiguration setHierarchySearchDepth(int hierarchySearchDepth) {
        this.hierarchySearchDepth = hierarchySearchDepth;
        return this;
    }

    public LDAPMappingConfiguration setBindingPropertyName(String bindingPropertyName) {
        this.bindingProperty = getBindingProperty(bindingPropertyName);
        return this;
    }
}
