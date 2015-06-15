package org.keycloak.authentication.actions;

import org.keycloak.Config;
import org.keycloak.Version;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.Errors;
import org.keycloak.freemarker.BrowserSecurityHeaderSetup;
import org.keycloak.freemarker.FreeMarkerException;
import org.keycloak.freemarker.FreeMarkerUtil;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TermsAndConditions implements RequiredActionProvider, RequiredActionFactory {

    public static final String PROVIDER_ID = "terms_and_conditions";

    public static class Resource {

        public Resource(RequiredActionContext context) {
            this.context = context;
        }

        protected RequiredActionContext context;

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public Response agree(final MultivaluedMap<String, String> formData)  throws URISyntaxException, IOException, FreeMarkerException {
            if (formData.containsKey("cancel")) {
                LoginProtocol protocol = context.getSession().getProvider(LoginProtocol.class, context.getClientSession().getAuthMethod());
                protocol.setRealm(context.getRealm())
                        .setHttpHeaders(context.getHttpRequest().getHttpHeaders())
                        .setUriInfo(context.getUriInfo());
                context.getEvent().error(Errors.REJECTED_BY_USER);
                return protocol.consentDenied(context.getClientSession());
            }
            context.getUser().removeRequiredAction(PROVIDER_ID);
            return AuthenticationManager.nextActionAfterAuthentication(context.getSession(), context.getUserSession(), context.getClientSession(), context.getConnection(), context.getHttpRequest(), context.getUriInfo(), context.getEvent());
        }

    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

    }

    @Override
    public Response invokeRequiredAction(RequiredActionContext context) {
        return context.getSession().getProvider(LoginFormsProvider.class)
                .setClientSessionCode(new ClientSessionCode(context.getRealm(), context.getClientSession()).getCode())
                .setUser(context.getUser())
                .createForm("terms.ftl", new HashMap<String, Object>());
    }

    @Override
    public Object jaxrsService(RequiredActionContext context) {
        return new Resource(context);
    }

    @Override
    public String getDisplayText() {
        return "Terms and Conditions";
    }

    @Override
    public void close() {

    }
}
