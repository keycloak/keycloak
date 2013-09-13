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

import java.net.URI;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

import org.keycloak.services.resources.flows.FormFlows;
import org.keycloak.services.resources.flows.Urls;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@ManagedBean(name = "url")
@RequestScoped
public class UrlBean {

    private URI baseURI;

    @ManagedProperty(value = "#{realm}")
    private RealmBean realm;

    @ManagedProperty(value = "#{register}")
    private RegisterBean registerBean;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        HttpServletRequest request = (HttpServletRequest) ctx.getExternalContext().getRequest();

        UriBuilder b = UriBuilder.fromUri(request.getRequestURI()).replaceQuery(request.getQueryString())
                .replacePath(request.getContextPath()).path("rest");

        if (request.getAttribute(FormFlows.CODE) != null) {
            b.queryParam("code", request.getAttribute(FormFlows.CODE));
        }

        baseURI = b.build();
    }

    public RealmBean getRealm() {
        return realm;
    }

    public void setRealm(RealmBean realm) {
        this.realm = realm;
    }

    public RegisterBean getRegisterBean() {
        return registerBean;
    }

    public void setRegisterBean(RegisterBean registerBean) {
        this.registerBean = registerBean;
    }

    public String getAccessUrl() {
        return Urls.accountAccessPage(baseURI, realm.getId()).toString();
    }

    public String getAccountUrl() {
        return Urls.accountPage(baseURI, realm.getId()).toString();
    }

    URI getBaseURI() {
        return baseURI;
    }

    public String getLoginAction() {
        if (realm.isSaas()) {
            return Urls.saasLoginAction(baseURI).toString();
        } else {
            return Urls.realmLoginAction(baseURI, realm.getId()).toString();
        }
    }

    public String getLoginUrl() {
        if (realm.isSaas()) {
            return Urls.saasLoginPage(baseURI).toString();
        } else {
            return Urls.realmLoginPage(baseURI, realm.getId()).toString();
        }
    }

    public String getPasswordUrl() {
        return Urls.accountPasswordPage(baseURI, realm.getId()).toString();
    }

    public String getRegistrationAction() {
        if (realm.isSaas()) {
            // TODO: saas social registration
            return Urls.saasRegisterAction(baseURI).toString();
        } else if (registerBean.isSocialRegistration()) {
            return Urls.socialRegisterAction(baseURI, realm.getId()).toString();
        } else {
            return Urls.realmRegisterAction(baseURI, realm.getId()).toString();
        }
    }

    public String getRegistrationUrl() {
        if (realm.isSaas()) {
            return Urls.saasRegisterPage(baseURI).toString();
        } else {
            return Urls.realmRegisterPage(baseURI, realm.getId()).toString();
        }
    }

    public String getSocialUrl() {
        return Urls.accountSocialPage(baseURI, realm.getId()).toString();
    }

    public String getTotpUrl() {
        return Urls.accountTotpPage(baseURI, realm.getId()).toString();
    }

}
