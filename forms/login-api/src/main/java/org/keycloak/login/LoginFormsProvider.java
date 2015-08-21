package org.keycloak.login;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface LoginFormsProvider extends Provider {

    /**
     * Adds a script to the html header
     *
     * @param scriptUrl
     */
    void addScript(String scriptUrl);

    public Response createResponse(UserModel.RequiredAction action);

    Response createForm(String form);

    public Response createLogin();

    public Response createPasswordReset();

    public Response createLoginTotp();

    public Response createRegistration();

    public Response createInfoPage();

    public Response createErrorPage();

    public Response createOAuthGrant(ClientSessionModel clientSessionModel);

    public Response createCode();

    public LoginFormsProvider setClientSessionCode(String accessCode);

    public LoginFormsProvider setAccessRequest(List<RoleModel> realmRolesRequested, MultivaluedMap<String,RoleModel> resourceRolesRequested, List<ProtocolMapperModel> protocolMappers);
    public LoginFormsProvider setAccessRequest(String message);

    /**
     * Set one global error message.
     * 
     * @param message key of message
     * @param parameters to be formatted into message
     */
    public LoginFormsProvider setError(String message, Object ... parameters);
    
    /**
     * Set multiple error messages.
     * 
     * @param messages to be set
     */
    public LoginFormsProvider setErrors(List<FormMessage> messages);

    LoginFormsProvider addError(FormMessage errorMessage);

    /**
     * Add a success message to the form
     *
     * @param errorMessage
     * @return
     */
    LoginFormsProvider addSuccess(FormMessage errorMessage);

    public LoginFormsProvider setSuccess(String message, Object ... parameters);

    public LoginFormsProvider setUser(UserModel user);

    public LoginFormsProvider setResponseHeader(String headerName, String headerValue);

    public LoginFormsProvider setFormData(MultivaluedMap<String, String> formData);

    LoginFormsProvider setAttribute(String name, Object value);

    public LoginFormsProvider setStatus(Response.Status status);

    LoginFormsProvider setActionUri(URI requestUri);
}
