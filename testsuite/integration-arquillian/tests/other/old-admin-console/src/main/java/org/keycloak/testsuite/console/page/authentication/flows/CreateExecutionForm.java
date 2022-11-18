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
package org.keycloak.testsuite.console.page.authentication.flows;

import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 * @author <a href="mailto:pzaoral@redhat.com">Peter Zaoral</a>
 */
public class CreateExecutionForm extends Form {
    public enum ProviderOption {
        IDENTITY_PROVIDER_REDIRECTOR("Identity Provider Redirector"),
        USERNAME_VALIDATION("Username Validation"),
        RESET_OTP("Reset OTP"),
        COOKIE("Cookie"),
        CHOOSE_USER("Choose User"),
        PASSWORD("Password"),
        REVIEW_PROFILE("Review Profile"),
        CONFIRM_LINK_EXISTING_ACCOUNT("Confirm Link Existing Account"),
        CONDITIONAL_OTP("Conditional OTP"),
        USERNAME_PASSWORD("Username Password"),
        KERBEROS("Kerberos"),
        SEND_RESET_EMAIL("Send Reset Email"),
        RESET_PASSWORD("Reset Password"),
        HTTP_BASIC_AUTHETICATION("HTTP Basic Authentication"),
        OTP_FORM("OTP Form"),
        USERNAME_PASSWORD_FORM_FOR_IDENTITY_PROVIDER_REAUTH("Username Password For Identity Provider Reauthentication"),
        VERIFY_EXISTING_ACCOUNT_BY_EMAIL("Verify Existing Account By Email"),
        SCRIPT("Script"),
        OTP("OTP"),
        CREATE_USER_IF_UNIQUE("Create User If Unique");
        
        private final String name;

        private ProviderOption(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
    
    @FindBy(id = "provider")
    private Select providerSelect;   
    
    public void selectProviderOption(ProviderOption value) {
        providerSelect.selectByVisibleText(value.getName());
    }
}
