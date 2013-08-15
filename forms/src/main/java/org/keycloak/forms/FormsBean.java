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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.imageio.spi.ServiceRegistry;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.resources.flows.FormFlows;
import org.keycloak.services.resources.flows.Urls;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@ManagedBean(name = "forms")
@RequestScoped
public class FormsBean {

    private RealmModel realm;

    private String name;

    private String loginUrl;

    private String loginAction;

    private UriBuilder socialLoginUrlBuilder;

    private String registrationUrl;

    private String registrationAction;

    private List<RequiredCredential> requiredCredentials;

    private List<SocialProvider> providers;

    private String theme;

    private String themeUrl;

    private Map<String, Object> themeConfig;

    private String error;

    private String errorDetails;

    private String view;

    private Map<String, String> formData;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        HttpServletRequest request = (HttpServletRequest) ctx.getExternalContext().getRequest();

        realm = (RealmModel) request.getAttribute(FormFlows.REALM);

        boolean saas = RealmModel.DEFAULT_REALM.equals(realm.getName());

        if (saas) {
            name = "Keycloak";
        } else {
            name = realm.getName();
        }

        view = ctx.getViewRoot().getViewId();
        view = view.substring(view.lastIndexOf('/') + 1, view.lastIndexOf('.'));
        
        UriBuilder b = UriBuilder.fromUri(request.getRequestURI()).replaceQuery(request.getQueryString())
                .replacePath(request.getContextPath()).path("rest");
        URI baseURI = b.build();

        if (saas) {
            loginUrl = Urls.saasLoginPage(baseURI).toString();
            loginAction = Urls.saasLoginAction(baseURI).toString();

            registrationUrl = Urls.saasRegisterPage(baseURI).toString();
            registrationAction = Urls.saasRegisterAction(baseURI).toString();
        } else {
            loginUrl = Urls.realmLoginPage(baseURI, realm.getId()).toString();
            loginAction = Urls.realmLoginAction(baseURI, realm.getId()).toString();

            registrationUrl = Urls.realmRegisterPage(baseURI, realm.getId()).toString();
            registrationAction = Urls.realmRegisterAction(baseURI, realm.getId()).toString();
        }

        socialLoginUrlBuilder = UriBuilder.fromUri(Urls.socialRedirectToProviderAuth(baseURI, realm.getId()));

        addRequiredCredentials();
        addFormData(request);
        addSocialProviders();
        addErrors(request);

        // TODO Get theme name from realm
        theme = "default";
        themeUrl = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/sdk/theme/" + theme;

        themeConfig = new HashMap<String, Object>();

        themeConfig.put("styles", themeUrl + "/styles.css");

        if (RealmModel.DEFAULT_REALM.equals(realm.getName())) {
            themeConfig.put("logo", themeUrl + "/img/red-hat-logo.png");
            themeConfig.put("background", themeUrl + "/img/login-screen-background.jpg");
        } else {
            themeConfig.put("background", themeUrl + "/img/customer-login-screen-bg2.jpg");
            themeConfig.put("displayPoweredBy", true);
        }
    }

    public Map<String, Object> getThemeConfig() {
        return themeConfig;
    }

    public String getName() {
        return name;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public String getLoginAction() {
        return loginAction;
    }

    public String getError() {
        return error;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public Map<String, String> getFormData() {
        return formData;
    }

    public List<RequiredCredential> getRequiredCredentials() {
        return requiredCredentials;
    }

    public String getView() {
        return view;
    }

    public String getTheme() {
        return theme;
    }

    public String getThemeUrl() {
        return themeUrl;
    }

    public String getRegistrationUrl() {
        return registrationUrl;
    }

    public String getRegistrationAction() {
        return registrationAction;
    }

    public boolean isSocial() {
        // TODO Check if social is enabled in realm
        return true && providers.size() > 0;
    }

    public boolean isRegistrationAllowed() {
        return realm.isRegistrationAllowed();
    }

    private void addFormData(HttpServletRequest request) {
        formData = new HashMap<String, String>();

        @SuppressWarnings("unchecked")
        MultivaluedMap<String, String> t = (MultivaluedMap<String, String>) request.getAttribute(FormFlows.DATA);
        if (t != null) {
            for (String k : t.keySet()) {
                formData.put(k, t.getFirst(k));
            }
        }
    }

    private void addRequiredCredentials() {
        requiredCredentials = new LinkedList<RequiredCredential>();
        for (RequiredCredentialModel m : realm.getRequiredCredentials()) {
            if (m.isInput()) {
                requiredCredentials.add(new RequiredCredential(m.getType(), m.isSecret(), m.getFormLabel()));
            }
        }
    }

    private void addSocialProviders() {
        // TODO Add providers configured for realm instead of all providers
        providers = new LinkedList<SocialProvider>();
        for (Iterator<org.keycloak.social.SocialProvider> itr = ServiceRegistry
                .lookupProviders(org.keycloak.social.SocialProvider.class); itr.hasNext();) {
            org.keycloak.social.SocialProvider p = itr.next();
            providers.add(new SocialProvider(p.getId(), p.getName()));
        }
    }

    private void addErrors(HttpServletRequest request) {
        error = (String) request.getAttribute(FormFlows.ERROR_MESSAGE);

        if (error != null) {
            if (view.equals("login")) {
                errorDetails = error;
                error = "Login failed";
            } else if (view.equals("register")) {
                errorDetails = error;
                error = "Registration failed";
            }
        }
    }

    public class Property {
        private String name;
        private String value;

        public Property(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    public class RequiredCredential {
        private String type;
        private boolean secret;
        private String formLabel;

        public RequiredCredential(String type, boolean secure, String formLabel) {
            this.type = type;
            this.secret = secure;
            this.formLabel = formLabel;
        }

        public String getName() {
            return type;
        }

        public String getLabel() {
            return formLabel;
        }

        public String getInputType() {
            return secret ? "password" : "text";
        }
    }

    public List<SocialProvider> getProviders() {
        return providers;
    }

    public class SocialProvider {
        private String id;
        private String name;

        public SocialProvider(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getLoginUrl() {
            return socialLoginUrlBuilder.replaceQueryParam("provider_id", id).build().toString();
        }
    }

}
