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

import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.keycloak.forms.model.RequiredCredential;
import org.keycloak.services.resources.flows.FormFlows;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@ManagedBean(name = "login")
@RequestScoped
public class LoginBean {

    @ManagedProperty(value = "#{realm}")
    private RealmBean realm;

    private String username;

    private List<RequiredCredential> requiredCredentials;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) ctx.getExternalContext().getRequest();

        @SuppressWarnings("unchecked")
        MultivaluedMap<String, String> formData = (MultivaluedMap<String, String>) request.getAttribute(FormFlows.DATA);
        if (formData != null) {
            username = formData.getFirst("username");
        }

        requiredCredentials = new LinkedList<RequiredCredential>();
        for (org.keycloak.services.models.RequiredCredentialModel c : realm.getRealm().getRequiredCredentials()) {
            if (c.isInput()) {
                requiredCredentials.add(new RequiredCredential(c.getType(), c.isSecret(), c.getFormLabel()));
            }
        }
    }

    public String getUsername() {
        return username;
    }

    public List<RequiredCredential> getRequiredCredentials() {
        return requiredCredentials;
    }

    public RealmBean getRealm() {
        return realm;
    }

    public void setRealm(RealmBean realm) {
        this.realm = realm;
    }

}
