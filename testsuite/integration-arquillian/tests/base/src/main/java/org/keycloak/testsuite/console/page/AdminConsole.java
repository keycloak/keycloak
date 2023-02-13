/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console.page;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.testsuite.auth.page.AuthServer;
import org.keycloak.testsuite.auth.page.login.PageWithLoginUrl;
import org.keycloak.testsuite.console.page.fragment.Menu;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import org.keycloak.testsuite.page.PageWithLogOutAction;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;

/**
 *
 * @author Petr Mensik
 */
public class AdminConsole extends AuthServer implements PageWithLoginUrl, PageWithLogOutAction {

    public static final String ADMIN_REALM = "adminRealm";

    public AdminConsole() {
        setUriParameter(ADMIN_REALM, MASTER);
    }

    public AdminConsole setAdminRealm(String adminRealm) {
        setUriParameter(ADMIN_REALM, adminRealm);
        return this;
    }

    public String getAdminRealm() {
        return getUriParameter(ADMIN_REALM).toString();
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("admin/{" + ADMIN_REALM + "}/console");
    }

    @Page
    private Menu menu;

    @FindBy(xpath = "//div[@class='modal-dialog']")
    protected ModalDialog modalDialog;

    /**
     *
     * @return OIDC Login URL for adminRealm parameter
     */
    @Override
    public URI getOIDCLoginUrl() {
        return OIDCLoginProtocolService.authUrl(UriBuilder.fromPath(getAuthRoot()))
                .build(getAdminRealm());
    }

    @FindBy(css = ".btn-danger")
    protected WebElement dangerButton;

    //@FindByJQuery(".btn-primary:visible")
    @FindBy(css = ".btn-primary")
    protected WebElement primaryButton;

    @FindBy(css = "navbar-brand")
    protected WebElement brandLink;

    @Override
    public void logOut() {
        menu.logOut();
    }

}
