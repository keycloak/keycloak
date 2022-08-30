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
package org.keycloak.testsuite.console.page.authentication.bindings;

import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class BindingsForm extends Form {
    
    @FindBy(id = "browser")
    private Select browser;

    @FindBy(id = "registration")
    private Select registration;

    @FindBy(id = "grant")
    private Select grant;

    @FindBy(id = "resetCredentials")
    private Select resetCredentials;

    @FindBy(id = "clientAuthentication")
    private Select clientAuthentication;

    public void select(BindingsSelect select, BindingsOption option) {
        switch (select) {
            case BROWSER:
                browser.selectByVisibleText(option.getName());
                break;
            case REGISTRATION:
                registration.selectByVisibleText(option.getName());
                break;
            case DIRECT_GRANT:
                grant.selectByVisibleText(option.getName());
                break;
            case RESET_CREDENTIALS:
                resetCredentials.selectByVisibleText(option.getName());
                break;
            case CLIENT_AUTHENTICATION:
                clientAuthentication.selectByVisibleText(option.getName());
                break;
        }
    }

    public enum BindingsSelect {
        BROWSER, 
        REGISTRATION, 
        DIRECT_GRANT,
        RESET_CREDENTIALS,
        CLIENT_AUTHENTICATION;
    }
    
    public enum BindingsOption {

        DIRECT_GRANT("direct grant"), 
        REGISTRATION("registration"), 
        BROWSER("browser"),
        RESET_CREDENTIALS("reset credentials");

        private final String name;

        private BindingsOption(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
