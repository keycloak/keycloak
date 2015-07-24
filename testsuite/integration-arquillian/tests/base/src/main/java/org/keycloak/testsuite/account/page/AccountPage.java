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
package org.keycloak.testsuite.account.page;

import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.page.auth.AuthServer;
import static org.keycloak.testsuite.console.page.Realm.MASTER;
import org.openqa.selenium.WebElement;

/**
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 */
public class AccountPage extends AuthServer {
    
    public static final String ACCOUNT_REALM = "accountRealm";
    
    public AccountPage() {
        setUriParameter(ACCOUNT_REALM, MASTER);
    }
    
    public void setAccountRealm(String accountRealm) {
        setUriParameter(ACCOUNT_REALM, accountRealm);
    }
    
    public String getAccountRealm() {
        return getUriParameter(ACCOUNT_REALM).toString();
    }
    
    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("realms/{" + ACCOUNT_REALM + "}/account");
    }
    
    @FindByJQuery(".nav li:eq(0) a")
    private WebElement keyclockConsole;
    
    @FindByJQuery(".nav li:eq(1) a")
    private WebElement signOutLink;
    
    @FindByJQuery(".bs-sidebar ul li:eq(0) a")
    private WebElement accountLink;
    
    @FindByJQuery(".bs-sidebar ul li:eq(1) a")
    private WebElement passwordLink;
    
    @FindByJQuery(".bs-sidebar ul li:eq(2) a")
    private WebElement authenticatorLink;
    
    @FindByJQuery(".bs-sidebar ul li:eq(3) a")
    private WebElement sessionsLink;
    
    @FindByJQuery("button[value='Save']")
    private WebElement save;
    
    public void keycloakConsole() {
        keyclockConsole.click();
    }
    
    public void signOut() {
        signOutLink.click();
    }
    
    public void account() {
        accountLink.click();
    }
    
    public void password() {
        passwordLink.click();
    }
    
    public void authenticator() {
        authenticatorLink.click();
    }
    
    public void sessions() {
        sessionsLink.click();
    }
    
    public void save() {
        save.click();
    }
}
