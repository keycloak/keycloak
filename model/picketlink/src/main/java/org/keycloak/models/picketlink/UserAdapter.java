package org.keycloak.models.picketlink;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.UserModel;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.sample.User;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAdapter implements UserModel {
    private static final String EMAIL_VERIFIED_ATTR = "emailVerified";
    private static final String KEYCLOAK_TOTP_ATTR = "totpEnabled";
    private static final String REQUIRED_ACTIONS_ATTR = "requiredActions";

    private static final String REDIRECT_URIS = "redirectUris";
    private static final String WEB_ORIGINS = "webOrigins";

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
    public String getFirstName() {
        return user.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        user.setFirstName(firstName);
        idm.update(user);
    }

    @Override
    public String getLastName() {
        return user.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        user.setLastName(lastName);
        idm.update(user);
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public void setEmail(String email) {
        user.setEmail(email);
        idm.update(user);
    }

    @Override
    public boolean isEmailVerified() {
        Attribute<Boolean> a = user.getAttribute(EMAIL_VERIFIED_ATTR);
        return a != null ? a.getValue() : false;
    }

    @Override
    public void setEmailVerified(boolean verified) {
        user.setAttribute(new Attribute<Boolean>(EMAIL_VERIFIED_ATTR, verified));
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
        if (attribute == null || attribute.getValue() == null)
            return null;
        return attribute.getValue().toString();
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attributes = new HashMap<String, String>();
        for (Attribute<?> attribute : user.getAttributes()) {
            if (attribute.getValue() != null)
                attributes.put(attribute.getName(), attribute.getValue().toString());
        }
        return attributes;
    }

    @Override
    public Set<RequiredAction> getRequiredActions() {
        return getAttributeSet(REQUIRED_ACTIONS_ATTR);
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        addToAttributeSet(REQUIRED_ACTIONS_ATTR, action);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        removeFromAttributeSet(REQUIRED_ACTIONS_ATTR, action);
    }

    @Override
    public Set<String> getRedirectUris() {
        return getAttributeSet(REDIRECT_URIS);
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        setAttributeSet(REDIRECT_URIS, redirectUris);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        addToAttributeSet(REDIRECT_URIS, redirectUri);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        removeFromAttributeSet(REDIRECT_URIS, redirectUri);
    }

    @Override
    public Set<String> getWebOrigins() {
        return getAttributeSet(WEB_ORIGINS);
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        setAttributeSet(WEB_ORIGINS, webOrigins);
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        addToAttributeSet(WEB_ORIGINS, webOrigin);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        removeFromAttributeSet(WEB_ORIGINS, webOrigin);
    }

    @Override
    public boolean isTotp() {
        Attribute<Boolean> a = user.getAttribute(KEYCLOAK_TOTP_ATTR);
        return a != null ? a.getValue() : false;
    }

    @Override
    public void setTotp(boolean totp) {
        user.setAttribute(new Attribute<Boolean>(KEYCLOAK_TOTP_ATTR, totp));
        idm.update(user);
    }

    @SuppressWarnings("unchecked")
    private <T extends Serializable> Set<T> getAttributeSet(String name) {
        Attribute<Serializable> a = user.getAttribute(name);

        Set<Serializable> s = new HashSet<Serializable>();

        if (a != null) {
            Serializable o = a.getValue();
            if (o instanceof Serializable[]) {
                for (Serializable t : (Serializable[]) o) {
                    s.add(t);
                }
            } else {
                s.add(o);
            }
        }

        return (Set<T>) s;
    }

    private <T extends Serializable> void setAttributeSet(String name, Set<T> set) {
        if (set.isEmpty()) {
            user.removeAttribute(name);
        } else {
            user.setAttribute(new Attribute<Serializable[]>(name, set.toArray(new Serializable[set.size()])));
        }
        idm.update(user);
    }

    private <T extends Serializable> void addToAttributeSet(String name, T t) {
        Set<Serializable> set = getAttributeSet(name);
        if (set == null) {
            set = new HashSet<Serializable>();
        }

        if (set.add(t)) {
            setAttributeSet(name, set);
            idm.update(user);
        }
    }

    private <T extends Serializable> void removeFromAttributeSet(String name, T t) {
        Set<Serializable> set = getAttributeSet(name);
        if (set == null) {
            return;
        }

        if (set.remove(t)) {
            setAttributeSet(name, set);
            idm.update(user);
        }
    }

}
