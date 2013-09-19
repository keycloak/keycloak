/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.services.resources.flows;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.services.email.EmailSender;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.UserModel.RequiredAction;
import org.picketlink.idm.model.sample.Realm;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FormFlows {

    public static final String DATA = "KEYCLOAK_FORMS_DATA";
    public static final String ERROR_MESSAGE = "KEYCLOAK_FORMS_ERROR_MESSAGE";
    public static final String REALM = Realm.class.getName();
    public static final String USER = UserModel.class.getName();
    public static final String SOCIAL_REGISTRATION = "socialRegistration";
    public static final String CODE = "code";

    private String error;
    private MultivaluedMap<String, String> formData;

    private RealmModel realm;

    private HttpRequest request;
    private UserModel userModel;

    private boolean socialRegistration;
    private String code;
    private UriInfo uriInfo;

    FormFlows(RealmModel realm, HttpRequest request, UriInfo uriInfo) {
        this.realm = realm;
        this.request = request;
        this.uriInfo = uriInfo;
    }

    public Response forwardToAction(RequiredAction action) {
        switch (action) {
            case CONFIGURE_TOTP:
                return forwardToTotp();
            case UPDATE_PROFILE:
                return forwardToAccount();
            case RESET_PASSWORD:
                return forwardToPassword();
            case VERIFY_EMAIL:
                return forwardToVerifyEmail();
            default:
                return null; // TODO
        }
    }

    public Response forwardToAccess() {
        return forwardToForm(Pages.ACCESS);
    }

    public Response forwardToAccount() {
        return forwardToForm(Pages.ACCOUNT);
    }

    private Response forwardToForm(String form) {
        request.setAttribute(REALM, realm);

        if (error != null) {
            request.setAttribute(ERROR_MESSAGE, error);
        }

        if (formData != null) {
            request.setAttribute(DATA, formData);
        }

        if (userModel != null) {
            request.setAttribute(USER, userModel);
        }

        if (code != null) {
            request.setAttribute(CODE, code);
        }

        request.setAttribute(SOCIAL_REGISTRATION, socialRegistration);

        request.forward(form);
        return null;
    }

    public Response forwardToLogin() {
        return forwardToForm(Pages.LOGIN);
    }

    public Response forwardToLoginTotp() {
        return forwardToForm(Pages.LOGIN_TOTP);
    }

    public Response forwardToPassword() {
        return forwardToForm(Pages.PASSWORD);
    }

    public Response forwardToRegistration() {
        return forwardToForm(Pages.REGISTER);
    }

    public Response forwardToSocial() {
        return forwardToForm(Pages.SOCIAL);
    }

    public Response forwardToTotp() {
        return forwardToForm(Pages.TOTP);
    }

    public Response forwardToVerifyEmail() {
        new EmailSender().sendEmailVerification(userModel, realm, code, uriInfo);
        return forwardToForm(Pages.VERIFY_EMAIL);
    }

    public FormFlows setCode(String code) {
        this.code = code;
        return this;
    }

    public FormFlows setError(String error) {
        this.error = error;
        return this;
    }

    public FormFlows setUser(UserModel userModel) {
        this.userModel = userModel;
        return this;
    }

    // Set flag whether user registration is triggered from social login
    public FormFlows setSocialRegistration(boolean socialRegistration) {
        this.socialRegistration = socialRegistration;
        return this;
    }

    public FormFlows setFormData(MultivaluedMap<String, String> formData) {
        this.formData = formData;
        return this;
    }

}
