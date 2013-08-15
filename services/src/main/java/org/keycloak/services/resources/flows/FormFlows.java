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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.services.models.RealmModel;
import org.picketlink.idm.model.sample.Realm;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
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
