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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.forms.MessageBean;
import org.keycloak.forms.LoginBean;
import org.keycloak.forms.OAuthGrantBean;
import org.keycloak.forms.RealmBean;
import org.keycloak.forms.RegisterBean;
import org.keycloak.forms.SocialBean;
import org.keycloak.forms.TemplateBean;
import org.keycloak.forms.TotpBean;
import org.keycloak.forms.UrlBean;
import org.keycloak.forms.UserBean;
import org.keycloak.services.FormService;
import org.keycloak.services.resources.flows.Pages;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class FormServiceImpl implements FormService {

    private static final Logger log = Logger.getLogger(FormServiceImpl.class);

    private static final String ID = "FormServiceId";
    private static final String BUNDLE = "org.keycloak.forms.messages";
    private final Map<String, Command> commandMap = new HashMap<String,Command>();

    public FormServiceImpl(){
        commandMap.put(Pages.LOGIN, new CommandLogin());
        commandMap.put(Pages.REGISTER, new CommandRegister());
        commandMap.put(Pages.ACCOUNT, new CommandAccount());
        commandMap.put(Pages.LOGIN_UPDATE_PROFILE, new CommandPassword());
        commandMap.put(Pages.PASSWORD, new CommandPassword());
        commandMap.put(Pages.LOGIN_RESET_PASSWORD, new CommandPassword());
        commandMap.put(Pages.LOGIN_UPDATE_PASSWORD, new CommandPassword());
        commandMap.put(Pages.ACCESS, new CommandAccess());
        commandMap.put(Pages.SOCIAL, new CommandSocial());
        commandMap.put(Pages.TOTP, new CommandTotp());
        commandMap.put(Pages.LOGIN_CONFIG_TOTP, new CommandTotp());
        commandMap.put(Pages.LOGIN_TOTP, new CommandLoginTotp());
        commandMap.put(Pages.LOGIN_VERIFY_EMAIL, new CommandVerifyEmail());
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
            cfg.setClassForTemplateLoading(FormServiceImpl.class,"/META-INF/resources");
            Template template = cfg.getTemplate(temp);

            template.process(input, out);
        } catch (IOException e) {
            log.error("Failed to load the template " + temp, e);
        } catch (TemplateException e) {
            log.error("Failed to process template " + temp, e);
        }

        return out.toString();
    }

    private class CommandTotp implements Command {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {
            RealmBean realm = new RealmBean(dataBean.getRealm());

            attributes.put("realm", realm);
            attributes.put("url", new UrlBean(realm, dataBean.getBaseURI()));

            UserBean user = new UserBean(dataBean.getUserModel());
            attributes.put("user", user);

            TotpBean totp = new TotpBean(user, dataBean.getContextPath());
            attributes.put("totp", totp);

            attributes.put("login", new LoginBean(realm, dataBean.getFormData()));
        }
    }

    private class CommandSocial implements Command {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {
            RealmBean realm = new RealmBean(dataBean.getRealm());
            attributes.put("user", new UserBean(dataBean.getUserModel()));
            attributes.put("url", new UrlBean(realm, dataBean.getBaseURI()));
        }
    }

    private class CommandEmail implements Command {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {
        }
    }

    private class CommandPassword implements Command {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {
            RealmBean realm = new RealmBean(dataBean.getRealm());

            attributes.put("realm", realm);
            attributes.put("url", new UrlBean(realm, dataBean.getBaseURI()));
            attributes.put("user", new UserBean(dataBean.getUserModel()));
            attributes.put("login", new LoginBean(realm, dataBean.getFormData()));
        }
    }

    private class CommandLoginTotp implements Command {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {

            RealmBean realm = new RealmBean(dataBean.getRealm());

            attributes.put("realm", realm);

            UrlBean url = new UrlBean(realm, dataBean.getBaseURI());
            url.setSocialRegistration(dataBean.getSocialRegistration());

            attributes.put("url", url);
            attributes.put("user", new UserBean(dataBean.getUserModel()));
            attributes.put("login", new LoginBean(realm, dataBean.getFormData()));

            RegisterBean register = new RegisterBean(dataBean.getFormData(), dataBean.getSocialRegistration());

            SocialBean social = new SocialBean(realm, dataBean.getSocialProviders(), register, url);
            attributes.put("social", social);
        }
    }

    private class CommandAccess implements Command {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {
            RealmBean realm = new RealmBean(dataBean.getRealm());

            attributes.put("realm", realm);
            attributes.put("user", new UserBean(dataBean.getUserModel()));
            attributes.put("url", new UrlBean(realm, dataBean.getBaseURI()));
        }
    }

    private class CommandAccount implements Command {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {
            RealmBean realm = new RealmBean(dataBean.getRealm());

            attributes.put("realm", realm);
            attributes.put("url", new UrlBean(realm, dataBean.getBaseURI()));
            attributes.put("user", new UserBean(dataBean.getUserModel()));
            attributes.put("login", new LoginBean(realm, dataBean.getFormData()));
        }
    }

    private class CommandLogin implements Command {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {
            RealmBean realm = new RealmBean(dataBean.getRealm());

            attributes.put("realm", realm);

            UrlBean url = new UrlBean(realm, dataBean.getBaseURI());
            url.setSocialRegistration(dataBean.getSocialRegistration());

            attributes.put("url", url);
            attributes.put("user", new UserBean(dataBean.getUserModel()));
            attributes.put("login", new LoginBean(realm, dataBean.getFormData()));

            RegisterBean register = new RegisterBean(dataBean.getFormData(), dataBean.getSocialRegistration());

            SocialBean social = new SocialBean(realm, dataBean.getSocialProviders(), register, url);
            attributes.put("social", social);
        }
    }

    private class CommandRegister implements Command {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {

            RealmBean realm = new RealmBean(dataBean.getRealm());

            attributes.put("realm", realm);

            UrlBean url = new UrlBean(realm, dataBean.getBaseURI());
            url.setSocialRegistration(dataBean.getSocialRegistration());

            attributes.put("url", url);
            attributes.put("user", new UserBean(dataBean.getUserModel()));

            RegisterBean register = new RegisterBean(dataBean.getFormData(), dataBean.getSocialRegistration());
            attributes.put("register", register);

            SocialBean social = new SocialBean(realm, dataBean.getSocialProviders(), register, url);
            attributes.put("social", social);
        }
    }

    private class CommandOAuthGrant implements Command {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {

            OAuthGrantBean oauth = new OAuthGrantBean();
            oauth.setAction(dataBean.getOAuthAction());
            oauth.setResourceRolesRequested(dataBean.getOAuthResourceRolesRequested());
            oauth.setClient(dataBean.getOAuthClient());
            oauth.setoAuthCode(dataBean.getOAuthCode());
            oauth.setRealmRolesRequested(dataBean.getOAuthRealmRolesRequested());

            attributes.put("oauth", oauth);
        }
    }

    private class CommandVerifyEmail implements Command {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean) {

            RealmBean realm = new RealmBean(dataBean.getRealm());

            attributes.put("realm", realm);

            UrlBean url = new UrlBean(realm, dataBean.getBaseURI());
            url.setSocialRegistration(dataBean.getSocialRegistration());

            attributes.put("url", url);
        }
    }

    private interface Command {
        public void exec(Map<String, Object> attributes, FormServiceDataBean dataBean);
    }

}