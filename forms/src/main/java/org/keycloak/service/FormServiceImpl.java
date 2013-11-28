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
package org.keycloak.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.forms.LoginBean;
import org.keycloak.forms.MessageBean;
import org.keycloak.forms.OAuthGrantBean;
import org.keycloak.forms.RealmBean;
import org.keycloak.forms.RegisterBean;
import org.keycloak.forms.SocialBean;
import org.keycloak.forms.TemplateBean;
import org.keycloak.forms.TotpBean;
import org.keycloak.forms.UrlBean;
import org.keycloak.forms.UserBean;
import org.keycloak.models.ApplicationModel;
import org.keycloak.services.FormService;
import org.keycloak.services.resources.flows.Pages;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class FormServiceImpl implements FormService {

    private static final Logger log = Logger.getLogger(FormServiceImpl.class);

    private static final String ID = "FormServiceId";
    private static final String BUNDLE = "org.keycloak.forms.messages";
    private final Map<String, CommandCommon> commandMap = new HashMap<String, CommandCommon>();

    public FormServiceImpl(){
        commandMap.put(Pages.LOGIN, new CommandLogin());
        commandMap.put(Pages.REGISTER, new CommandRegister());
        commandMap.put(Pages.ACCOUNT, new CommandCommon());
        commandMap.put(Pages.LOGIN_UPDATE_PROFILE, new CommandCommon());
        commandMap.put(Pages.PASSWORD, new CommandCommon());
        commandMap.put(Pages.LOGIN_RESET_PASSWORD, new CommandCommon());
        commandMap.put(Pages.LOGIN_UPDATE_PASSWORD, new CommandCommon());
        commandMap.put(Pages.ACCESS, new CommandCommon());
        commandMap.put(Pages.SOCIAL, new CommandCommon());
        commandMap.put(Pages.TOTP, new CommandTotp());
        commandMap.put(Pages.LOGIN_CONFIG_TOTP, new CommandTotp());
        commandMap.put(Pages.LOGIN_TOTP, new CommandLoginTotp());
        commandMap.put(Pages.LOGIN_VERIFY_EMAIL, new CommandCommon());
        commandMap.put(Pages.OAUTH_GRANT, new CommandOAuthGrant());
    }

    public String getId(){
        return ID;
    }

    public String process(String pageId, FormServiceDataBean dataBean){

        Map<String, Object> attributes = new HashMap<String, Object>();

        if (dataBean.getMessage() != null){
            attributes.put("message", new MessageBean(dataBean.getMessage(), dataBean.getMessageType()));
        }

        RealmBean realm = new RealmBean(dataBean.getRealm());
        attributes.put("template", new TemplateBean(realm, dataBean.getContextPath()));

        ResourceBundle rb = ResourceBundle.getBundle(BUNDLE);
        attributes.put("rb", rb);

        if (commandMap.containsKey(pageId)){
            commandMap.get(pageId).exec(attributes, dataBean);
        }

        return processFmTemplate(pageId, attributes);
    }

    private String processFmTemplate(String temp, Map<String, Object> input) {

        Writer out = new StringWriter();
        Configuration cfg = new Configuration();

        try {
            cfg.setClassForTemplateLoading(FormServiceImpl.class,"/META-INF/resources/forms/theme/default");
            Template template = cfg.getTemplate(temp);

            template.process(input, out);
        } catch (IOException e) {
            log.error("Failed to load the template " + temp, e);
        } catch (TemplateException e) {
            log.error("Failed to process template " + temp, e);
        }

        return out.toString();
    }

    private class CommandCommon {
        protected RealmBean realm;
        protected UrlBean url;
        protected UserBean user;
        protected LoginBean login;

        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {
            realm = new RealmBean(dataBean.getRealm());

            String referrer = dataBean.getQueryParam("referrer");
            String referrerUri = null;
            if (referrer != null) {
                for (ApplicationModel a : dataBean.getRealm().getApplications()) {
                    if (a.getName().equals(referrer)) {
                        referrerUri = a.getBaseUrl();
                        break;
                    }
                }
            }

            url = new UrlBean(realm, dataBean.getBaseURI(), referrerUri);
            url.setSocialRegistration(dataBean.getSocialRegistration());
            user = new UserBean(dataBean.getUserModel());
            login = new LoginBean(realm, dataBean.getFormData());

            attributes.put("realm", realm);
            attributes.put("url", url);
            attributes.put("user", user);
            attributes.put("login", login);
        }
    }

    private class CommandTotp extends CommandCommon {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {
            super.exec(attributes, dataBean);

            attributes.put("totp", new TotpBean(user, dataBean.getContextPath()));
        }
    }

    private class CommandLoginTotp extends CommandCommon {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {
            super.exec(attributes, dataBean);

            RegisterBean register = new RegisterBean(dataBean.getFormData(), dataBean.getSocialRegistration());
            SocialBean social = new SocialBean(realm, dataBean.getSocialProviders(), register, url);
            attributes.put("social", social);
        }
    }

    private class CommandLogin extends CommandCommon {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {
            super.exec(attributes, dataBean);

            RegisterBean register = new RegisterBean(dataBean.getFormData(), dataBean.getSocialRegistration());
            SocialBean social = new SocialBean(realm, dataBean.getSocialProviders(), register, url);
            attributes.put("social", social);
        }
    }

    private class CommandRegister extends CommandCommon {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {
            super.exec(attributes, dataBean);

            RegisterBean register = new RegisterBean(dataBean.getFormData(), dataBean.getSocialRegistration());
            attributes.put("register", register);

            SocialBean social = new SocialBean(realm, dataBean.getSocialProviders(), register, url);
            attributes.put("social", social);
        }
    }

    private class CommandOAuthGrant extends CommandCommon {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {
            super.exec(attributes, dataBean);

            OAuthGrantBean oauth = new OAuthGrantBean();
            oauth.setAction(dataBean.getOAuthAction());
            oauth.setResourceRolesRequested(dataBean.getOAuthResourceRolesRequested());
            oauth.setClient(dataBean.getOAuthClient());
            oauth.setoAuthCode(dataBean.getOAuthCode());
            oauth.setRealmRolesRequested(dataBean.getOAuthRealmRolesRequested());

            attributes.put("oauth", oauth);
        }
    }

}