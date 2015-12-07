package org.keycloak.protocol.oidc.endpoints;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.forms.RegistrationPage;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.AttributeFormDataProcessor;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.validation.Validation;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RegisterEndpoint {

    private static final Logger logger = Logger.getLogger(RegisterEndpoint.class);
    private MultivaluedMap<String, String> formParams;

    @Context
    private KeycloakSession session;

    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @Context
    private ClientConnection clientConnection;

    private final RealmModel realm;
    private final EventBuilder event;

    public RegisterEndpoint(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
    }

    @POST
    public Response build() {
        formParams = request.getDecodedFormParameters();

        checkSsl();
        checkRealm();
        
        event.event(EventType.REGISTER);

        return buildUserRegistration();
    }

    @OPTIONS
    public Response preflight() {
        if (logger.isDebugEnabled()) {
            logger.debugv("CORS preflight from: {0}", headers.getRequestHeaders().getFirst("Origin"));
        }
        return Cors.add(request, Response.ok()).auth().preflight().build();
    }

    private void checkSsl() {
        if (!uriInfo.getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new ErrorResponseException("invalid_request", "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            throw new ErrorResponseException("access_denied", "Realm not enabled", Response.Status.FORBIDDEN);
        }
    }

    public Response buildUserRegistration() {

    	validate();
    	
    	Map<String, Object> res = success();

        event.success();

        return Cors.add(request, Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).build();
    }

    public void validate() {
        
        event.detail(Details.REGISTER_METHOD, "direct");

    	String username = formParams.getFirst(RegistrationPage.FIELD_USERNAME);
    	
        if (!realm.isRegistrationEmailAsUsername()) {
        	
        	if (Validation.isBlank(username)) {
                event.error(Errors.INVALID_USER_CREDENTIALS);
                throw new ErrorResponseException(Errors.INVALID_USER_CREDENTIALS, Messages.MISSING_USERNAME, Response.Status.BAD_REQUEST);
        	}
        }

        if (Validation.isBlank(formParams.getFirst((RegistrationPage.FIELD_FIRST_NAME)))) {
            event.error(Errors.INVALID_USER_CREDENTIALS);
            throw new ErrorResponseException(Errors.INVALID_USER_CREDENTIALS, Messages.MISSING_FIRST_NAME, Response.Status.BAD_REQUEST);
        }

        if (Validation.isBlank(formParams.getFirst((RegistrationPage.FIELD_LAST_NAME)))) {
            event.error(Errors.INVALID_USER_CREDENTIALS);
            throw new ErrorResponseException(Errors.INVALID_USER_CREDENTIALS, Messages.MISSING_LAST_NAME, Response.Status.BAD_REQUEST);
        }

        String email = formParams.getFirst(Validation.FIELD_EMAIL);
        if (Validation.isBlank(email)) {
            event.error(Errors.INVALID_EMAIL);
            throw new ErrorResponseException(Errors.INVALID_EMAIL, Messages.MISSING_EMAIL, Response.Status.BAD_REQUEST);
        } else if (!Validation.isEmailValid(email)) {
            event.detail(Details.EMAIL, email);
            event.error(Errors.INVALID_EMAIL);
            throw new ErrorResponseException(Errors.INVALID_EMAIL, Messages.INVALID_EMAIL, Response.Status.BAD_REQUEST);
        }

        if (Validation.isBlank(formParams.getFirst(RegistrationPage.FIELD_PASSWORD))) {
            event.error(Errors.INVALID_USER_CREDENTIALS);
            throw new ErrorResponseException(Errors.INVALID_USER_CREDENTIALS, Messages.MISSING_PASSWORD, Response.Status.BAD_REQUEST);
        }
        if (formParams.getFirst(RegistrationPage.FIELD_PASSWORD) != null) {
            PasswordPolicy.Error err = realm.getPasswordPolicy().validate(realm.isRegistrationEmailAsUsername() ? formParams.getFirst(RegistrationPage.FIELD_EMAIL) : formParams.getFirst(RegistrationPage.FIELD_USERNAME), formParams.getFirst(RegistrationPage.FIELD_PASSWORD));
            if (err != null) {
                event.error(Errors.INVALID_USER_CREDENTIALS);
                throw new ErrorResponseException(Errors.INVALID_USER_CREDENTIALS, err.getMessage()==null?Messages.INVALID_PASSWORD_EXISTING:err.getMessage(), Response.Status.BAD_REQUEST);
            }
        }

        if (!Validation.isBlank(username) && session.users().getUserByUsername(username, realm) != null) {
            event.detail(Details.USERNAME, username);
            event.error(Errors.USERNAME_IN_USE);
            throw new ErrorResponseException(Errors.USERNAME_IN_USE, Messages.USERNAME_EXISTS, Response.Status.BAD_REQUEST);
        }

        if (session.users().getUserByEmail(email, realm) != null) {
            event.detail(Details.EMAIL, email);
            event.error(Errors.EMAIL_IN_USE);
            throw new ErrorResponseException(Errors.EMAIL_IN_USE, Messages.EMAIL_EXISTS, Response.Status.BAD_REQUEST);
        }
    }

    public Map<String, Object> success() {
        String username = formParams.getFirst(RegistrationPage.FIELD_USERNAME);
        String password = formParams.getFirst(RegistrationPage.FIELD_PASSWORD);
        String email = formParams.getFirst(Validation.FIELD_EMAIL);
        
        if (realm.isRegistrationEmailAsUsername()) {
            username = formParams.getFirst(RegistrationPage.FIELD_EMAIL);
        }
        event.detail(Details.USERNAME, username)
                .detail(Details.REGISTER_METHOD, "form")
                .detail(Details.EMAIL, email)
        ;
        
        UserModel user = session.users().addUser(realm, username);
        user.setEnabled(true);

        user.setEmail(email);
        user.setFirstName(formParams.getFirst(RegistrationPage.FIELD_FIRST_NAME));
        user.setLastName(formParams.getFirst(RegistrationPage.FIELD_LAST_NAME));
        
        AttributeFormDataProcessor.process(formParams, realm, user);

        event.user(user);
        event.success();
        
        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(CredentialRepresentation.PASSWORD);
        credentials.setValue(password);
        session.users().updateCredential(realm, user, UserCredentialModel.password(password));

        Map<String, Object> userInfo = new HashMap<String, Object>();
        userInfo.putAll(user.getAttributes());
        userInfo.put(RegistrationPage.FIELD_EMAIL, user.getEmail());
        userInfo.put(RegistrationPage.FIELD_USERNAME, user.getUsername());
        userInfo.put(RegistrationPage.FIELD_LAST_NAME, user.getLastName());
        userInfo.put(RegistrationPage.FIELD_FIRST_NAME, user.getFirstName());
       
        return userInfo;
    }

}