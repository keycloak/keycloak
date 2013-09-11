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
package org.keycloak.forms;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.keycloak.services.resources.flows.FormFlows;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@ManagedBean(name = "register")
@RequestScoped
public class RegisterBean {

    private HashMap<String, String> formData;

    private boolean socialRegistration;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) ctx.getExternalContext().getRequest();

        this.formData = new HashMap<String, String>();

        Boolean socialRegistrationAttr = (Boolean)request.getAttribute(FormFlows.SOCIAL_REGISTRATION);
        this.socialRegistration = socialRegistrationAttr != null && socialRegistrationAttr;

        @SuppressWarnings("unchecked")
        MultivaluedMap<String, String> formData = (MultivaluedMap<String, String>) request.getAttribute(FormFlows.DATA);
        if (formData != null) {
            for (String k : formData.keySet()) {
                this.formData.put(k, formData.getFirst(k));
            }
        }
    }

    public Map<String, String> getFormData() {
        return formData;
    }

    public boolean isSocialRegistration() {
        return socialRegistration;
    }

}
