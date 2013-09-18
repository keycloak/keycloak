package org.keycloak.services.models.picketlink;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.utils.ArrayUtils;
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
    private static final String STATUS_ATTR = "status";

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

    public UserModel.Status getStatus() {
        Attribute<UserModel.Status> a = user.getAttribute(STATUS_ATTR);
        if (a != null) {
            return a.getValue();
        } else {
            return user.isEnabled() ? UserModel.Status.ENABLED : UserModel.Status.DISABLED;
        }
    }

    @Override
    public void setStatus(UserModel.Status status) {
        user.setAttribute(new Attribute<UserModel.Status>(STATUS_ATTR, status));
        if (status == UserModel.Status.DISABLED) {
            user.setEnabled(false);
        } else {
            user.setEnabled(true);
        }
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

    private RequiredAction[] getRequiredActionsArray() {
        Attribute<?> a = user.getAttribute(REQUIRED_ACTIONS_ATTR);
        if (a == null) {
            return null;
        }

        Object o = a.getValue();
        if (o instanceof RequiredAction) {
            return new RequiredAction[] { (RequiredAction) o };
        } else {
            return (RequiredAction[]) o;
        }
    }

    @Override
    public List<RequiredAction> getRequiredActions() {
        RequiredAction[] actions = getRequiredActionsArray();
        if (actions == null) {
            return null;
        } else {
            return Collections.unmodifiableList(Arrays.asList(actions));
        }
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        RequiredAction[] actions = getRequiredActionsArray();
        if (actions == null) {
            actions = new RequiredAction[] { action };
        } else {
            actions = ArrayUtils.add(actions, action);
        }

        Attribute<RequiredAction[]> a = new Attribute<RequiredAction[]>(REQUIRED_ACTIONS_ATTR, actions);

        user.setAttribute(a);
        idm.update(user);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        RequiredAction[] actions = getRequiredActionsArray();
        if (actions != null) {
            if (Arrays.binarySearch(actions, action) >= 0) {
                actions = ArrayUtils.remove(actions, action);

                if (actions.length == 0) {
                    user.removeAttribute(REQUIRED_ACTIONS_ATTR);
                } else {
                    Attribute<RequiredAction[]> a = new Attribute<RequiredAction[]>(REQUIRED_ACTIONS_ATTR, actions);
                    user.setAttribute(a);
                }

                idm.update(user);
            }
        }
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

}
