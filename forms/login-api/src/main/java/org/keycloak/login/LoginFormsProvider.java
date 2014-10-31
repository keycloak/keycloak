package org.keycloak.login;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface LoginFormsProvider extends Provider {

    public LoginFormsProvider setRealm(RealmModel realm);

    public LoginFormsProvider setUriInfo(UriInfo uriInfo);

    public Response createResponse(UserModel.RequiredAction action);

    public Response createLogin();

    public Response createPasswordReset();

    public Response createLoginTotp();

    public Response createRegistration();

    public Response createErrorPage();

    public Response createOAuthGrant();

    public Response createCode();

    public LoginFormsProvider setClientSessionCode(String accessCode);

    public LoginFormsProvider setAccessRequest(List<RoleModel> realmRolesRequested, MultivaluedMap<String,RoleModel> resourceRolesRequested);

    public LoginFormsProvider setError(String message);

    public LoginFormsProvider setSuccess(String message);

    public LoginFormsProvider setWarning(String message);

    public LoginFormsProvider setUser(UserModel user);

    public LoginFormsProvider setClient(ClientModel client);

    public LoginFormsProvider setQueryParams(MultivaluedMap<String, String> queryParams);

    public LoginFormsProvider setFormData(MultivaluedMap<String, String> formData);

    public LoginFormsProvider setStatus(Response.Status status);

}
