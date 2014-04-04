package org.keycloak.account;

import org.keycloak.audit.Event;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface Account {

    Response createResponse(AccountPages page);

    Account setError(String message);

    Account setSuccess(String message);

    Account setWarning(String message);

    Account setUser(UserModel user);

    Account setStatus(Response.Status status);

    Account setRealm(RealmModel realm);

    Account setReferrer(String[] referrer);

    Account setEvents(List<Event> events);

    Account setFeatures(boolean social, boolean audit, boolean passwordUpdateSupported);
}
