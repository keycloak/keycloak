package org.keycloak.models;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface AccountRoles {

    String VIEW_PROFILE = "view-profile";
    String MANAGE_ACCOUNT = "manage-account";

    String[] ALL = {VIEW_PROFILE, MANAGE_ACCOUNT};

}
