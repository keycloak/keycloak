package org.keycloak.account.freemarker.model;

import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountBean {

    private final UserModel user;
    private final MultivaluedMap<String, String> profileFormData;

    public AccountBean(UserModel user, MultivaluedMap<String, String> profileFormData) {
        this.user = user;
        this.profileFormData = profileFormData;
    }

    public String getFirstName() {
        return profileFormData != null ?  profileFormData.getFirst("firstName") : user.getFirstName();
    }

    public String getLastName() {
        return profileFormData != null ?  profileFormData.getFirst("lastName") :user.getLastName();
    }

    public String getUsername() {
        return user.getUsername();
    }

    public String getEmail() {
        return profileFormData != null ?  profileFormData.getFirst("email") :user.getEmail();
    }

}
