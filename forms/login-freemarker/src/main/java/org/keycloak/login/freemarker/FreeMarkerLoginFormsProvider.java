package org.keycloak.login.freemarker;

import org.keycloak.login.LoginForms;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.RealmModel;

import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerLoginFormsProvider implements LoginFormsProvider {

    @Override
    public LoginForms createForms(RealmModel realm, UriInfo uriInfo) {
        return new FreeMarkerLoginForms(realm, uriInfo);
    }

}
