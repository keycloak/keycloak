package org.keycloak.account;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface Account {

    public Response createResponse(AccountPages page);

    public Account setError(String message);

    public Account setSuccess(String message);

    public Account setWarning(String message);

    public Account setUser(UserModel user);

    public Account setStatus(Response.Status status);

    public Account setRealm(RealmModel realm);

    public Account setReferrer(String[] referrer);

}
