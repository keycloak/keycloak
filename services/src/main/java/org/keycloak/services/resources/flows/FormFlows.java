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

import java.net.URI;
import java.util.Iterator;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.keycloak.services.FormService;
import org.keycloak.services.email.EmailSender;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.UserModel.RequiredAction;
import org.picketlink.idm.model.sample.Realm;

import javax.imageio.spi.ServiceRegistry;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
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
    private AccessCodeEntry accessCode;
    private UriInfo uriInfo;

    FormFlows(RealmModel realm, HttpRequest request, UriInfo uriInfo) {
        this.realm = realm;
        this.request = request;
        this.uriInfo = uriInfo;
    }

    public Response forwardToAction(RequiredAction action) {
        switch (action) {
            case CONFIGURE_TOTP:
                return forwardToForm(Pages.LOGIN_CONFIG_TOTP);
            case UPDATE_PROFILE:
                return forwardToForm(Pages.LOGIN_UPDATE_PROFILE);
            case UPDATE_PASSWORD:
                return forwardToForm(Pages.LOGIN_UPDATE_PASSWORD);
            case VERIFY_EMAIL:
                new EmailSender().sendEmailVerification(userModel, realm, accessCode, uriInfo);
                return forwardToForm(Pages.LOGIN_VERIFY_EMAIL);
            default:
                return Response.serverError().build();
        }
    }

    public Response forwardToAccess() {
        return forwardToForm(Pages.ACCESS);
    }

    public Response forwardToAccount() {
        return forwardToForm(Pages.ACCOUNT);
    }

    private Response forwardToForm(String template) {

        FormService.FormServiceDataBean formDataBean = new FormService.FormServiceDataBean(realm, userModel, formData, error);

        // Getting URI needed by form processing service
        ResteasyUriInfo uriInfo = request.getUri();
        MultivaluedMap<String, String> queryParameterMap = uriInfo.getQueryParameters();

        String requestURI = uriInfo.getBaseUri().getPath();
        UriBuilder uriBuilder = UriBuilder.fromUri(requestURI);

        for(String k : queryParameterMap.keySet()){
            uriBuilder.replaceQueryParam(k, queryParameterMap.get(k).toArray());
        }

        if (accessCode != null) {
            uriBuilder.queryParam(CODE, accessCode.getCode());
        }

        URI baseURI = uriBuilder.build();
        formDataBean.setBaseURI(baseURI);

        // TODO find a better way to obtain contextPath
        // Getting context path by removing "rest/" substring from the BaseUri path
        formDataBean.setContextPath(requestURI.substring(0,requestURI.length()-5));
        formDataBean.setSocialRegistration(socialRegistration);

        // Find the service and process relevant template
        Iterator<FormService> itr = ServiceRegistry.lookupProviders(FormService.class);

        while (itr.hasNext()) {
            FormService provider = itr.next();
            if (provider.getId().equals("FormServiceId"))
                return Response.status(200).type(MediaType.TEXT_HTML).entity(provider.process(template, formDataBean)).build();
        }

        return Response.status(200).entity("form provider not found").build();
    }

    public Response forwardToLogin() {
        return forwardToForm(Pages.LOGIN);
    }

    public Response forwardToPasswordReset() {
        return forwardToForm(Pages.LOGIN_RESET_PASSWORD);
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

    public FormFlows setAccessCode(AccessCodeEntry accessCode) {
        this.accessCode = accessCode;
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
