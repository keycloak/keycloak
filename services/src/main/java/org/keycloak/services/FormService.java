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
package org.keycloak.services;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.flows.FormFlows;
import org.keycloak.social.SocialProvider;

import javax.imageio.spi.ServiceRegistry;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public interface FormService {

    String getId();

    public String process(String pageId, FormServiceDataBean data);

    public static class FormServiceDataBean {

        private RealmModel realm;
        private UserModel userModel;
        private String message;

        private FormFlows.MessageType messageType;

        private MultivaluedMap<String, String> formData;
        private URI baseURI;

        private List<SocialProvider> socialProviders;

        public Boolean getSocialRegistration() {
            return socialRegistration;
        }

        public void setSocialRegistration(Boolean socialRegistration) {
            this.socialRegistration = socialRegistration;
        }

        private Boolean socialRegistration;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        private String code;

        public String getContextPath() {
            return contextPath;
        }

        public void setContextPath(String contextPath) {
            this.contextPath = contextPath;
        }

        private String contextPath;

        public FormServiceDataBean(RealmModel realm, UserModel userModel, MultivaluedMap<String, String> formData, String message) {
            this.realm = realm;
            this.userModel = userModel;
            this.formData = formData;
            this.message = message;

            socialProviders = new LinkedList<SocialProvider>();
            Map<String, String> socialConfig = realm.getSocialConfig();
            if (socialConfig != null) {
                for (Iterator<SocialProvider> itr = ServiceRegistry.lookupProviders(org.keycloak.social.SocialProvider.class); itr.hasNext(); ) {
                    SocialProvider p = itr.next();
                    if (socialConfig.containsKey(p.getId() + ".key") && socialConfig.containsKey(p.getId() + ".secret")) {
                        socialProviders.add(p);
                    }
                }
            }
        }

        public URI getBaseURI() {
            return baseURI;
        }

        public void setBaseURI(URI baseURI) {
            this.baseURI = baseURI;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public MultivaluedMap<String, String> getFormData() {
            return formData;
        }

        public void setFormData(MultivaluedMap<String, String> formData) {
            this.formData = formData;
        }

        public RealmModel getRealm() {
            return realm;
        }

        public RealmModel setRealm(RealmModel realm) {
            return realm;
        }

        public UserModel getUserModel() {
            return userModel;
        }

        public void setUserModel(UserModel userModel) {
            this.userModel = userModel;
        }

        public List<SocialProvider> getSocialProviders() {
            return socialProviders;
        }

        public FormFlows.MessageType getMessageType() {
            return messageType;
        }

        public void setMessageType(FormFlows.MessageType messageType) {
            this.messageType = messageType;
        }

        /* OAuth Part */
        private MultivaluedMap<String, RoleModel> oAuthResourceRolesRequested;
        private List<RoleModel> oAuthRealmRolesRequested;
        private UserModel oAuthClient;
        private String oAuthCode;
        private String oAuthAction;

        public String getOAuthAction() {
            return oAuthAction;
        }

        public void setOAuthAction(String action) {
            this.oAuthAction = action;
        }

        public MultivaluedMap<String, RoleModel> getOAuthResourceRolesRequested() {
            return oAuthResourceRolesRequested;
        }

        public void setOAuthResourceRolesRequested(MultivaluedMap<String, RoleModel> resourceRolesRequested) {
            this.oAuthResourceRolesRequested = resourceRolesRequested;
        }

        public List<RoleModel> getOAuthRealmRolesRequested() {
            return oAuthRealmRolesRequested;
        }

        public void setOAuthRealmRolesRequested(List<RoleModel> realmRolesRequested) {
            this.oAuthRealmRolesRequested = realmRolesRequested;
        }

        public UserModel getOAuthClient() {
            return oAuthClient;
        }

        public void setOAuthClient(UserModel client) {
            this.oAuthClient = client;
        }

        public String getOAuthCode() {
            return oAuthCode;
        }

        public void setOAuthCode(String oAuthCode) {
            this.oAuthCode = oAuthCode;
        }

    }
}
