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

    public class LoginSettingsForm extends Form {

        @FindByJQuery("div[class='onoffswitch']:eq(0)")
        private OnOffSwitch registrationAllowed;

        @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='registrationEmailAsUsername']]")
        private OnOffSwitch emailAsUsernameOnOffSwitch;

        @FindByJQuery("div[class='onoffswitch']:eq(1)")
        private OnOffSwitch resetPasswordAllowed;

        @FindByJQuery("div[class='onoffswitch']:eq(2)")
        private OnOffSwitch rememberMeEnabled;

        @FindByJQuery("div[class='onoffswitch']:eq(3)")
        private OnOffSwitch verifyEmailEnabled;

        @FindByJQuery("div[class='onoffswitch']:eq(4)")
        private OnOffSwitch directGrantApiEnabled;

        @FindByJQuery("div[class='onoffswitch']:eq(5)")
        private OnOffSwitch requireSsl;

        public boolean isRegistrationAllowed() {
            return registrationAllowed.isOn();
        }
        
        public void setRegistrationAllowed(boolean allowed) {
            registrationAllowed.setOn(allowed);
        }
        
        public void setEmailAsUsername(boolean emailAsUsername) {
            emailAsUsernameOnOffSwitch.setOn(emailAsUsername);
        }
        
    }

}
