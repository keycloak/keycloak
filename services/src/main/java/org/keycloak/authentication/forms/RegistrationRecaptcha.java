package org.keycloak.authentication.forms;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RegistrationRecaptcha implements FormAction, FormActionFactory {
    public static final String G_RECAPTCHA_RESPONSE = "g-recaptcha-response";
    public static final String RECAPTCHA_REFERENCE_CATEGORY = "recaptcha";
    protected static Logger logger = Logger.getLogger(RegistrationRecaptcha.class);

    public static final String PROVIDER_ID = "registration-recaptcha-action";

    @Override
    public String getDisplayType() {
        return "Recaptcha";
    }

    @Override
    public String getReferenceCategory() {
        return RECAPTCHA_REFERENCE_CATEGORY;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[0];
    }

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        form.setAttribute("recaptchaRequired", true);
        form.setAttribute("recaptchaSiteKey", "6LcFEAkTAAAAAOaY-5RJk3zIYw4AalNtqfac27Bn");
        List<String> scripts = new LinkedList<>();
        scripts.add("https://www.google.com/recaptcha/api.js");
        form.setAttribute("scripts", scripts);
    }

    @Override
    public void validate(ValidationContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        List<FormMessage> errors = new ArrayList<>();
        boolean success = false;
        context.getEvent().detail(Details.REGISTER_METHOD, "form");

        String captcha = formData.getFirst(G_RECAPTCHA_RESPONSE);
        if (Validation.isBlank(captcha)) {

            HttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
            HttpPost post = new HttpPost("https://www.google.com/recaptcha/api/siteverify");
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("secret", "6LcFEAkTAAAAAM0SErEs9NlfhYpOTRj_vOVJSAMI"));
            formparams.add(new BasicNameValuePair("response", captcha));
            formparams.add(new BasicNameValuePair("remoteip", context.getConnection().getRemoteAddr()));
            try {
                UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
                post.setEntity(form);
                HttpResponse response = httpClient.execute(post);
                InputStream content = response.getEntity().getContent();
                try {
                    Map json = JsonSerialization.readValue(content, Map.class);
                    Object val = json.get("success");
                    success = Boolean.TRUE.equals(val);
                } finally {
                    content.close();
                }
            } catch (Exception e) {
                logger.error("Recaptcha failed", e);
            }
        }
        if (success) {
            context.success();
        } else {
            String usernameField = RegistrationPage.FIELD_USERNAME;
            if (context.getRealm().isRegistrationEmailAsUsername()) {
                usernameField = RegistrationPage.FIELD_EMAIL;
            }
            errors.add(new FormMessage(usernameField, Messages.RECAPTCHA_FAILED));
            formData.remove(G_RECAPTCHA_RESPONSE);
            context.getEvent().error(Errors.INVALID_REGISTRATION);
            context.validationError(formData, errors);
            return;


        }
    }

    @Override
    public void success(FormContext context) {

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }

    @Override
    public FormAction create(KeycloakSession session) {
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
}
