package org.keycloak.services.resources.flows;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.services.models.RealmModel;
import org.picketlink.idm.model.sample.Realm;

public class FormFlows {

    public static final String REALM = Realm.class.getName();
    public static final String ERROR_MESSAGE = "KEYCLOAK_FORMS_ERROR_MESSAGE";
    public static final String DATA = "KEYCLOAK_FORMS_DATA";

    private MultivaluedMap<String, String> formData;
    private String error;

    private RealmModel realm;

    private HttpRequest request;

    FormFlows(RealmModel realm, HttpRequest request) {
        this.realm = realm;
        this.request = request;
    }

    public FormFlows setFormData(MultivaluedMap<String, String> formData) {
        this.formData = formData;
        return this;
    }

    public FormFlows setError(String error) {
        this.error = error;
        return this;
    }

    public Response forwardToLogin() {
        return forwardToForm(Pages.LOGIN);
    }

    public Response forwardToRegistration() {
        return forwardToForm(Pages.REGISTER);
    }

    private Response forwardToForm(String form) {
        request.setAttribute(REALM, realm);

        if (error != null) {
            request.setAttribute(ERROR_MESSAGE, error);
        }

        if (formData != null) {
            request.setAttribute(DATA, formData);
        }

        request.forward(form);
        return null;
    }

}
