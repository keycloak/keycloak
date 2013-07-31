package org.keycloak.services.models;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserModel {
    protected User user;
    protected IdentityManager idm;

    public UserModel(User user, IdentityManager idm) {
        this.user = user;
        this.idm = idm;
    }

    protected User getUser() {
        return user;
    }

    public String getLoginName() {
        return user.getLoginName();
    }

    public boolean isEnabled() {
        return user.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        user.setEnabled(enabled);
        idm.update(user);
    }

    public void setAttribute(String name, String value) {
        user.setAttribute(new Attribute<String>(name, value));
        idm.update(user);
    }

    public void removeAttribute(String name) {
        user.removeAttribute(name);
        idm.update(user);
    }

    public String getAttribute(String name) {
        Attribute<String> attribute = user.getAttribute(name);
        if (attribute == null || attribute.getValue() == null) return null;
        return attribute.getValue().toString();
    }

    public Map<String, String> getAttributes() {
        Map<String, String> attributes = new HashMap<String, String>();
        for (Attribute attribute : user.getAttributes()) {
           if (attribute.getValue() != null) attributes.put(attribute.getName(), attribute.getValue().toString());
        }
        return attributes;
    }
}
