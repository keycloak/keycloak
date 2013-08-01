package org.keycloak.services.models.picketlink;

import org.keycloak.services.models.UserModel;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.sample.User;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAdapter implements UserModel {
    protected User user;
    protected IdentityManager idm;

    public UserAdapter(User user, IdentityManager idm) {
        this.user = user;
        this.idm = idm;
    }

    protected User getUser() {
        return user;
    }

    @Override
    public String getLoginName() {
        return user.getLoginName();
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        user.setEnabled(enabled);
        idm.update(user);
    }

    @Override
    public void setAttribute(String name, String value) {
        user.setAttribute(new Attribute<String>(name, value));
        idm.update(user);
    }

    @Override
    public void removeAttribute(String name) {
        user.removeAttribute(name);
        idm.update(user);
    }

    @Override
    public String getAttribute(String name) {
        Attribute<String> attribute = user.getAttribute(name);
        if (attribute == null || attribute.getValue() == null) return null;
        return attribute.getValue().toString();
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attributes = new HashMap<String, String>();
        for (Attribute attribute : user.getAttributes()) {
           if (attribute.getValue() != null) attributes.put(attribute.getName(), attribute.getValue().toString());
        }
        return attributes;
    }
}
