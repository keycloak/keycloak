package org.keycloak.account;

import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.keycloak.events.Event;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface AccountProvider extends Provider {

	AccountProvider setUriInfo(UriInfo uriInfo);

	AccountProvider setHttpHeaders(HttpHeaders httpHeaders);

	Response createResponse(AccountPages page);

	AccountProvider setError(String message, Object... parameters);

	AccountProvider setErrors(List<FormMessage> messages);

	AccountProvider setSuccess(String message, Object... parameters);

	AccountProvider setWarning(String message, Object... parameters);

	AccountProvider setUser(UserModel user);

	AccountProvider setProfileFormData(MultivaluedMap<String, String> formData);

	AccountProvider setStatus(Response.Status status);

	AccountProvider setRealm(RealmModel realm);

	AccountProvider setReferrer(String[] referrer);

	AccountProvider setEvents(List<Event> events);

	AccountProvider setSessions(List<UserSessionModel> sessions);

	AccountProvider setPasswordSet(boolean passwordSet);

	AccountProvider setStateChecker(String stateChecker);

	AccountProvider setFeatures(boolean social, boolean events, boolean passwordUpdateSupported);
}
