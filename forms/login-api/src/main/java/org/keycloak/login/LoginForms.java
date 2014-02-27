package org.keycloak.login;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface LoginForms {

    public Response createResponse(UserModel.RequiredAction action);

    public Response createLogin();

    public Response createPasswordReset();

    public Response createLoginTotp();

    public Response createRegistration();

    public Response createErrorPage();

    public Response createOAuthGrant();

    public LoginForms setAccessCode(String accessCodeId, String accessCode);

    public LoginForms setAccessRequest(List<RoleModel> realmRolesRequested, MultivaluedMap<String,RoleModel> resourceRolesRequested);

    public LoginForms setError(String message);

    public LoginForms setSuccess(String message);

    public LoginForms setWarning(String message);

    public LoginForms setUser(UserModel user);

    public LoginForms setClient(ClientModel client);

    public LoginForms setFormData(MultivaluedMap<String, String> formData);

    public LoginForms setStatus(Response.Status status);

}
