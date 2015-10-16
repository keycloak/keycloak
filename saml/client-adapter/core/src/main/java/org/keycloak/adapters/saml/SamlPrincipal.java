package org.keycloak.adapters.saml;

import org.keycloak.common.util.MultivaluedHashMap;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlPrincipal implements Serializable, Principal {
    private MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    private MultivaluedHashMap<String, String> friendlyAttributes = new MultivaluedHashMap<>();
    private String name;
    private String samlSubject;
    private String nameIDFormat;

    public SamlPrincipal(String name, String samlSubject, String nameIDFormat, MultivaluedHashMap<String, String> attributes, MultivaluedHashMap<String, String> friendlyAttributes) {
        this.name = name;
        this.attributes = attributes;
        this.friendlyAttributes = friendlyAttributes;
        this.samlSubject = samlSubject;
        this.nameIDFormat = nameIDFormat;
    }

    public SamlPrincipal() {
    }

    public String getSamlSubject() {
        return samlSubject;
    }

    public String getNameIDFormat() {
        return nameIDFormat;
    }

    @Override
    public String getName() {
        return name;
    }


    public List<String> getAttributes(String name) {
        List<String> list = attributes.get(name);
        if (list != null) {
            return Collections.unmodifiableList(list);
        } else {
            return Collections.emptyList();
        }

    }
    public List<String> getFriendlyAttributes(String friendlyName) {
        List<String> list = friendlyAttributes.get(name);
        if (list != null) {
            return Collections.unmodifiableList(list);
        } else {
            return Collections.emptyList();
        }

    }

    public String getAttribute(String name) {
        return attributes.getFirst(name);
    }

    public String getFriendlyAttribute(String friendlyName) {
        return friendlyAttributes.getFirst(friendlyName);
    }

    public Set<String> getAttributeNames() {
        return Collections.unmodifiableSet(attributes.keySet());

    }

    public Set<String> getFriendlyNames() {
        return Collections.unmodifiableSet(friendlyAttributes.keySet());

    }

}
