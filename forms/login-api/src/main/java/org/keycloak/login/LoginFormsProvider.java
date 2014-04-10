package org.keycloak.login;

import org.keycloak.models.RealmModel;

import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface LoginFormsProvider {

    public LoginForms createForms(RealmModel realm, UriInfo uriInfo);

}
