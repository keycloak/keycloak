package org.keycloak.services.util;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.ClientConnection;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.AccountService;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class CsrfHelper {

    public static MultivaluedMap<String, String> initStateChecker(RealmModel realm, HttpHeaders headers, UriInfo uriInfo, ClientConnection clientConnection) {
        String stateChecker = getStateChecker(headers);
        if (stateChecker == null) {
            stateChecker = UUID.randomUUID().toString();
            String cookiePath = AuthenticationManager.getRealmCookiePath(realm, uriInfo);
            boolean secureOnly = realm.getSslRequired().isRequired(clientConnection);
            CookieHelper.addCookie(AccountService.KEYCLOAK_STATE_CHECKER, stateChecker, cookiePath, null, null, -1, secureOnly, true);
        }

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl<String, String>();
        formData.putSingle("stateChecker", stateChecker);
        return formData;
    }

    public static void csrfCheck(HttpHeaders headers, MultivaluedMap<String, String> formData) {
        String stateCheckerCookie = getStateChecker(headers);
        String stateCheckerForm = formData.getFirst("stateChecker");
        if (stateCheckerCookie == null || !stateCheckerCookie.equals(stateCheckerForm)) {
            throw new ForbiddenException();
        }
    }

    public static String getStateChecker(HttpHeaders headers) {
        Cookie cookie = headers.getCookies().get(AccountService.KEYCLOAK_STATE_CHECKER);
        return cookie != null ? cookie.getValue() : null;
    }

}
