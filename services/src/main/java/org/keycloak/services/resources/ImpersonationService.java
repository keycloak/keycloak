package org.keycloak.services.resources;

import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.Config;
import org.keycloak.events.EventBuilder;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.ImpersonationServiceConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ImpersonationService extends AbstractSecuredLocalService {

    public static final String UNKNOWN_USER_MESSAGE = "unknownUser";
    private EventBuilder event;

    public ImpersonationService(RealmModel realm, ClientModel client, EventBuilder event) {
        super(realm, client);
        this.event = event;
    }

    private static Set<String> VALID_PATHS = new HashSet<String>();

    static {
    }

    @Override
    protected Set<String> getValidPaths() {
        return VALID_PATHS;
    }

    @Override
    protected URI getBaseRedirectUri() {
        return Urls.realmBase(uriInfo.getBaseUri()).path(RealmsResource.class, "getImpersonationService").build(realm.getName());
    }

    @GET
    public Response impersonatePage() {
        Response challenge = authenticateBrowser();
        if (challenge != null) return challenge;
        LoginFormsProvider page = page();
        return renderPage(page);
    }

    protected LoginFormsProvider page() {
        UserModel user = auth.getUser();
        LoginFormsProvider page = session.getProvider(LoginFormsProvider.class)
                .setActionUri(getBaseRedirectUri())
                .setAttribute("stateChecker", stateChecker);
        if (realm.getName().equals(Config.getAdminRealm())) {
            List<String> realms = new LinkedList<>();
            for (RealmModel possibleRealm : session.realms().getRealms()) {
                ClientModel realmAdminApp = realm.getClientByClientId(KeycloakModelUtils.getMasterRealmAdminApplicationClientId(possibleRealm));
                RoleModel role = realmAdminApp.getRole(ImpersonationServiceConstants.IMPERSONATION_ALLOWED);
                if (user.hasRole(role)) {
                   realms.add(possibleRealm.getName());
                }
            }
            if (realms.isEmpty()) {
                throw new ForbiddenException("not authorized to access impersonation", ErrorPage.error(session, Messages.NO_ACCESS));
            }
            if (realms.size() > 1 || !realms.get(0).equals(realm.getName())) {
                page.setAttribute("realmList", realms);
            }
        } else {
            authorizeCurrentRealm();
        } return page;
    }

    protected Response renderPage(LoginFormsProvider page) {
        return page
                .createForm("impersonate.ftl", new HashMap<String, Object>());
    }

    protected void authorizeMaster(String realmName) {
        RealmModel possibleRealm = session.realms().getRealmByName(realmName);
        if (possibleRealm == null) {
            throw new NotFoundException("Could not find realm");
        }
        ClientModel realmAdminApp = realm.getClientByClientId(KeycloakModelUtils.getMasterRealmAdminApplicationClientId(possibleRealm));
        RoleModel role = realmAdminApp.getRole(ImpersonationServiceConstants.IMPERSONATION_ALLOWED);
        if (!auth.getUser().hasRole(role)) {
            throw new ForbiddenException("not authorized to access impersonation", ErrorPage.error(session, Messages.NO_ACCESS));
        }
    }

    private void authorizeCurrentRealm() {
        UserModel user = auth.getUser();
        String realmAdminApplicationClientId = Constants.REALM_MANAGEMENT_CLIENT_ID;
        ClientModel realmAdminApp = realm.getClientByClientId(realmAdminApplicationClientId);
        RoleModel role = realmAdminApp.getRole(ImpersonationServiceConstants.IMPERSONATION_ALLOWED);
        if (!user.hasRole(role)) {
            throw new ForbiddenException("not authorized to access impersonation", ErrorPage.error(session, Messages.NO_ACCESS));
        }
    }

    @POST
    public Response impersonate() {
        Response challenge = authenticateBrowser();
        if (challenge != null) return challenge;
        MultivaluedMap<String, String> formData = request.getDecodedFormParameters();
        String realmName = formData.getFirst("realm");
        RealmModel chosenRealm = null;
        if (realmName == null) {
            chosenRealm = realm;
        } else{
            chosenRealm = session.realms().getRealmByName(realmName);
            if (chosenRealm == null) {
                throw new NotFoundException("Could not find realm");
            }
        }

        if (realm.getName().equals(Config.getAdminRealm())) {
            authorizeMaster(chosenRealm.getName());
        } else {
            if (realmName == null) authorizeCurrentRealm();
            else {
                throw new ForbiddenException("not authorized to access impersonation", ErrorPage.error(session, Messages.NO_ACCESS));
            }
        }

        csrfCheck(formData);

        if (formData.containsKey("cancel")) {
            return renderPage(page());
        }
        String username = formData.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            return renderPage(
                    page().setError(UNKNOWN_USER_MESSAGE)
            );
        }
        UserModel user = session.users().getUserByUsername(username, chosenRealm);
        if (user == null) {
            user = session.users().getUserByEmail(username, chosenRealm);
        }
        if (user == null) {
            return renderPage(
                    page().setError(UNKNOWN_USER_MESSAGE)
            );
        }
        // if same realm logout before impersonation
        if (chosenRealm.getId().equals(realm.getId())) {
            AuthenticationManager.backchannelLogout(session, realm, auth.getSession(), uriInfo, clientConnection, headers, true);
        }
        UserSessionModel userSession = session.sessions().createUserSession(chosenRealm, user, username, clientConnection.getRemoteAddr(), "impersonate", false, null, null);
        AuthenticationManager.createLoginCookie(chosenRealm, userSession.getUser(), userSession, uriInfo, clientConnection);
        URI redirect = AccountService.accountServiceApplicationPage(uriInfo).build(chosenRealm.getName());
        return Response.status(302).location(redirect).build();


    }
}