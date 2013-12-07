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
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.services.FormService;
import org.keycloak.services.email.EmailException;
import org.keycloak.services.email.EmailSender;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.messages.Messages;

import javax.imageio.spi.ServiceRegistry;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FormFlows {

    public static final String CODE = "code";

    private String message;

    public static enum MessageType {SUCCESS, WARNING, ERROR};
    private MessageType messageType = MessageType.ERROR;

    private MultivaluedMap<String, String> formData;

    private Map<String, String> queryParams;

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
                return forwardToActionForm(Pages.LOGIN_CONFIG_TOTP, Messages.ACTION_WARN_TOTP);
            case UPDATE_PROFILE:
                return forwardToActionForm(Pages.LOGIN_UPDATE_PROFILE, Messages.ACTION_WARN_PROFILE);
            case UPDATE_PASSWORD:
                return forwardToActionForm(Pages.LOGIN_UPDATE_PASSWORD, Messages.ACTION_WARN_PASSWD);
            case VERIFY_EMAIL:
                try {
                    new EmailSender(realm.getSmtpConfig()).sendEmailVerification(userModel, realm, accessCode, uriInfo);
                } catch (EmailException e) {
                    return setError("emailSendError").forwardToErrorPage();
                }
                return forwardToActionForm(Pages.LOGIN_VERIFY_EMAIL, Messages.ACTION_WARN_EMAIL);
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

    private Response forwardToForm(String template, FormService.FormServiceDataBean formDataBean) {

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

        if (queryParams != null) {
            for (Map.Entry<String, String> q : queryParams.entrySet()) {
                uriBuilder.replaceQueryParam(q.getKey(), q.getValue());
            }
        }

        URI baseURI = uriBuilder.build();
        formDataBean.setBaseURI(baseURI);

        // TODO find a better way to obtain contextPath
        // Getting context path by removing "rest/" substring from the BaseUri path
        formDataBean.setContextPath(requestURI.substring(0, requestURI.length() - 6));
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

    public Response forwardToForm(String template) {

        FormService.FormServiceDataBean formDataBean = new FormService.FormServiceDataBean(realm, userModel, formData, queryParams, message);
        formDataBean.setMessageType(messageType);

        return forwardToForm(template, formDataBean);
    }

    private Response forwardToActionForm(String template, String warningSummary) {

        // If no other message is set, notify user about required action in the warning window
        // so it's clear that this is a req. action form not a login form
        if (message == null){
            messageType = MessageType.WARNING;
            message = warningSummary;
        }

        return forwardToForm(template);
    }

    public Response forwardToLogin() {
        return forwardToForm(Pages.LOGIN);
    }

    public Response forwardToPasswordReset() {
        return forwardToForm(Pages.LOGIN_RESET_PASSWORD);
    }

    public Response forwardToUsernameReminder() {
        return forwardToForm(Pages.LOGIN_USERNAME_REMINDER);
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

    public Response forwardToErrorPage() {
        return forwardToForm(Pages.ERROR);
    }

    public Response forwardToOAuthGrant(){

        FormService.FormServiceDataBean formDataBean = new FormService.FormServiceDataBean(realm, userModel, formData, queryParams, message);

        formDataBean.setOAuthRealmRolesRequested((List<RoleModel>) request.getAttribute("realmRolesRequested"));
        formDataBean.setOAuthResourceRolesRequested((MultivaluedMap<String, RoleModel>) request.getAttribute("resourceRolesRequested"));
        formDataBean.setOAuthClient((UserModel)request.getAttribute("client"));
        formDataBean.setOAuthCode((String)request.getAttribute("code"));
        formDataBean.setOAuthAction((String)request.getAttribute("action"));

        return forwardToForm(Pages.OAUTH_GRANT, formDataBean);
    }

    public FormFlows setAccessCode(AccessCodeEntry accessCode) {
        this.accessCode = accessCode;
        return this;
    }

    public FormFlows setQueryParam(String key, String value) {
        if (queryParams == null) {
            queryParams = new HashMap<String, String>();
        }
        queryParams.put(key, value);
        return this;
    }

    public FormFlows setError(String message) {
        this.message = message;
        this.messageType = MessageType.ERROR;
        return this;
    }

    public FormFlows setSuccess(String message) {
        this.message = message;
        this.messageType = MessageType.SUCCESS;
        return this;
    }

    public FormFlows setWarning(String message) {
        this.message = message;
        this.messageType = MessageType.WARNING;
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
