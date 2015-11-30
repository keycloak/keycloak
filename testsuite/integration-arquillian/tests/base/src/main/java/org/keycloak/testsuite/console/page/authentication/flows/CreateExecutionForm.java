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
 */
public class CreateExecutionForm extends Form {
    public enum ProviderOption {
        DIRECT_GRANT_VALIDATE_USERNAME("direct-grant-validate-username"),
        RESET_OTP("reset-otp"),
        AUTH_COOKIE("auth-cookie"),
        RESET_CREDENTIALS_CHOOSE_USER("reset-credentials-choose-user"),
        DIRECT_GRANT_VALIDATE_PASSWORD("direct-grant-validate-password"),
        AUTH_USERNAME_PASSWORD_FORM("auth-username-password-form"),
        AUTH_OTP_FORM("auth-otp-form"),
        AUTH_SPNEGO("auth-spnego"),
        DIRECT_GRANT_VALIDATE_OPT("direct-grant-validate-otp"),
        RESET_CREDENTIALS_EMAIL("reset-credential-email"),
        RESET_PASSWORD("reset-password");
        
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
