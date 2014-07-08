package org.keycloak.models.hybrid;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.users.Attributes;
import org.keycloak.models.users.Credentials;
import org.keycloak.models.users.Feature;
import org.keycloak.models.users.User;
import org.keycloak.models.utils.Pbkdf2PasswordEncoder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.keycloak.models.utils.Pbkdf2PasswordEncoder.getSalt;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserAdapter implements UserModel {

    private HybridModelProvider provider;
    private RealmModel realm;
    private User user;

    UserAdapter(HybridModelProvider provider, RealmModel realm, User user) {
        this.provider = provider;
        this.realm = realm;
        this.user = user;
    }

    User getUser() {
        return user;
    }

    @Override
    public String getId() {
        return user.getId();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public void setUsername(String username) {
        user.setUsername(username);
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        user.setEnabled(enabled);
    }

    @Override
    public void setAttribute(String name, String value) {
        user.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        user.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        return user.getAttribute(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        return user.getAttributes();
    }

    @Override
    public Set<RequiredAction> getRequiredActions() {
        String value = user.getAttribute(Attributes.REQUIRED_ACTIONS);
        if (value == null) {
            return Collections.emptySet();
        }

        Set<RequiredAction> actions = new HashSet<RequiredAction>();
        for (String a : value.substring(1, value.length() - 1).split(",")) {
            actions.add(RequiredAction.valueOf(a.trim()));
        }
        return actions;
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        Set<RequiredAction> actions;
        if (user.getAttribute(Attributes.REQUIRED_ACTIONS) == null) {
            actions = new HashSet<RequiredAction>();
        } else {
            actions = getRequiredActions();
        }

        if (!actions.contains(action)) {
            actions.add(action);
            user.setAttribute(Attributes.REQUIRED_ACTIONS, actions.toString());
        }
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        Set<RequiredAction> actions = getRequiredActions();
        if (actions.contains(action)) {
            actions.remove(action);

            if (actions.isEmpty()) {
                user.removeAttribute(Attributes.REQUIRED_ACTIONS);
            } else {
                user.setAttribute(Attributes.REQUIRED_ACTIONS, actions.toString());
            }
        }
    }

    @Override
    public String getFirstName() {
        return user.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        user.setFirstName(firstName);
    }

    @Override
    public String getLastName() {
        return user.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        user.setLastName(lastName);
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public void setEmail(String email) {
        user.setEmail(email);
    }

    @Override
    public boolean isEmailVerified() {
        return isBooleanAttribute(Attributes.EMAIL_VERIFIED);
    }

    @Override
    public void setEmailVerified(boolean verified) {
        setBooleanAttribute(Attributes.EMAIL_VERIFIED, verified);
    }

    @Override
    public boolean isTotp() {
        return isBooleanAttribute(Attributes.TOTP_ENABLED);
    }

    @Override
    public void setTotp(boolean totp) {
        setBooleanAttribute(Attributes.TOTP_ENABLED, totp);
    }

    @Override
    public void updateCredential(UserCredentialModel model) {
        if (provider.users().supports(Feature.UPDATE_CREDENTIALS)) {
            Credentials credentials;

            if (model.getType().equals(UserCredentialModel.PASSWORD)) {
                byte[] salt = getSalt();
                int hashIterations = 1;
                PasswordPolicy policy = realm.getPasswordPolicy();
                if (policy != null) {
                    hashIterations = policy.getHashIterations();
                    if (hashIterations == -1) hashIterations = 1;
                }
                String value = new Pbkdf2PasswordEncoder(salt).encode(model.getValue(), hashIterations);

                credentials = new Credentials(model.getType(), salt, value, hashIterations, model.getDevice());
            } else {
                credentials = new Credentials(model.getType(), model.getValue(), model.getDevice());
            }

            user.updateCredential(credentials);
        } else {
            throw new RuntimeException("Users store doesn't support updating credentials");
        }
    }

    @Override
    public List<UserCredentialValueModel> getCredentialsDirectly() {
        if (provider.users().supports(Feature.READ_CREDENTIALS)) {
            List<UserCredentialValueModel> models = new LinkedList<UserCredentialValueModel>();
            for (Credentials cred : user.getCredentials()) {
                UserCredentialValueModel model = new UserCredentialValueModel();
                model.setType(cred.getType());
                model.setValue(cred.getValue());
                model.setDevice(cred.getDevice());
                model.setSalt(cred.getSalt());
                model.setHashIterations(cred.getHashIterations());
                models.add(model);
            }
            return models;
        } else {
            throw new IllegalStateException("Users provider doesn't support reading credentials");
        }
    }

    @Override
    public void updateCredentialDirectly(UserCredentialValueModel model) {
        if (provider.users().supports(Feature.UPDATE_CREDENTIALS)) {
            Credentials credentials = new Credentials(model.getType(), model.getSalt(), model.getValue(), model.getHashIterations(), model.getDevice());
            user.updateCredential(credentials);
        } else {
            throw new IllegalStateException("Users provider doesn't support updating credentials");
        }
    }

    @Override
    public AuthenticationLinkModel getAuthenticationLink() {
        for (Map.Entry<String, String> e : user.getAttributes().entrySet()) {
            if (e.getKey().matches("keycloak\\.authenticationLink\\..*\\.userId")) {
                String provider = e.getKey().split("\\.")[2];
                return new AuthenticationLinkModel(provider, e.getValue());
            }
        }
        return null;
    }

    @Override
    public void setAuthenticationLink(AuthenticationLinkModel authenticationLink) {
        Iterator<Map.Entry<String, String>> itr = user.getAttributes().entrySet().iterator();
        while (itr.hasNext()) {
            if (itr.next().getKey().matches("keycloak\\.authenticationLink\\..*\\.userId")) {
                itr.remove();
            }
        }
        user.setAttribute("keycloak.authenticationLink." + authenticationLink.getAuthProvider() + ".userId", authenticationLink.getAuthUserId());
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (RoleModel r : getRoleMappings()) {
            if (r.getContainer() instanceof RealmModel) {
                roles.add(r);
            }
        }
        return roles;
    }

    @Override
    public Set<RoleModel> getApplicationRoleMappings(ApplicationModel app) {
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (RoleModel r : getRoleMappings()) {
            if (r.getContainer() instanceof ApplicationModel && ((ApplicationModel) r.getContainer()).getId().equals(app.getId())) {
                roles.add(r);
            }
        }
        return roles;
    }

    @Override
    public boolean hasRole(RoleModel role) {
        for (RoleModel r : getRoleMappings()) {
            if (r.hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void grantRole(RoleModel role) {
        user.grantRole(role.getId());
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (String r : user.getRoleMappings()) {
            roles.add(realm.getRoleById(r));
        }
        return roles;
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        user.deleteRoleMapping(role.getId());
    }

    private boolean isBooleanAttribute(String name) {
        String v = user.getAttribute(name);
        return v != null ? v.equals("true") : false;
    }

    private void setBooleanAttribute(String name, boolean enable) {
        if (enable) {
            user.setAttribute(name, "true");
        } else {
            user.removeAttribute(name);
        }
    }

}
