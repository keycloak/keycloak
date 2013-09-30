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

import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;

import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.resources.flows.FormFlows;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public interface FormService {

    String getId();

    public String process(String pageId, FormServiceDataBean data);

    public static class FormServiceDataBean {

        private RealmModel realm;
        private UserModel userModel;
        private String error;

        private FormFlows.ErrorType errorType;

        private MultivaluedMap<String, String> formData;
        private URI baseURI;

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

        public FormServiceDataBean(RealmModel realm, UserModel userModel, MultivaluedMap<String, String> formData, String error){
            this.realm = realm;
            this.userModel = userModel;
            this.formData = formData;
            this.error = error;
        }

        public URI getBaseURI() {
            return baseURI;
        }

        public void setBaseURI(URI baseURI) {
            this.baseURI = baseURI;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
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

        public FormFlows.ErrorType getErrorType() {
            return errorType;
        }

        public void setErrorType(FormFlows.ErrorType errorType) {
            this.errorType = errorType;
        }
    }
}
