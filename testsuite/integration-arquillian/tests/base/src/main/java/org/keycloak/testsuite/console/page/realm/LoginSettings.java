/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console.page.realm;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author Petr Mensik
 */
public class LoginSettings extends RealmSettings {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/login-settings";
    }

    @Page
    private LoginSettingsForm form;
    
    public LoginSettingsForm form() {
        return form;
    }
    
    public enum RequireSSLOption {
        all, external, none;
    }

    public class LoginSettingsForm extends Form {

        @FindByJQuery("div[class='onoffswitch']:eq(0)")
        private OnOffSwitch registrationAllowed;

        @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='registrationEmailAsUsername']]")
        private OnOffSwitch emailAsUsernameOnOffSwitch;

        @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='editUsernameAllowed']]")
        private OnOffSwitch editUsernameAllowed;
        
        @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='resetPasswordAllowed']]")
        private OnOffSwitch resetPasswordAllowed;
        
        @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='rememberMe']]")
        private OnOffSwitch rememberMeEnabled;

        @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='verifyEmail']]")
        private OnOffSwitch verifyEmailEnabled;

        @FindBy(id = "sslRequired")
        private Select requireSsl;

        public boolean isRegistrationAllowed() {
            return registrationAllowed.isOn();
        }
        
        public void setRegistrationAllowed(boolean allowed) {
            registrationAllowed.setOn(allowed);
        }
        
        public void setEmailAsUsername(boolean emailAsUsername) {
            emailAsUsernameOnOffSwitch.setOn(emailAsUsername);
        }

        public boolean isEmailAsUsername() {
            return emailAsUsernameOnOffSwitch.isOn();
        }
        
        public boolean isEditUsernameAllowed() {
            return editUsernameAllowed.isOn();
        }
        
        public void setEditUsernameAllowed(boolean allowed) {
            editUsernameAllowed.setOn(allowed);
        }
        
        public boolean isResetPasswordAllowed() {
            return resetPasswordAllowed.isOn();
        }
        
        public void setResetPasswordAllowed(boolean allowed) {
            resetPasswordAllowed.setOn(allowed);
        }
        
        public boolean isRememberMeAllowed() {
            return rememberMeEnabled.isOn();
        }
        
        public void setRememberMeAllowed(boolean allowed) {
            rememberMeEnabled.setOn(allowed);
        }
        
        public void setVerifyEmailAllowed(boolean allowed) {
            verifyEmailEnabled.setOn(allowed);
        }
        
        public boolean isVerifyEmailAllowed() {
            return verifyEmailEnabled.isOn();
        }
        
        public void selectRequireSSL(RequireSSLOption option) {
            requireSsl.selectByValue(option.name());
        }
    }

}
