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
package org.keycloak.testsuite;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.testsuite.pages.ChangePasswordPage;
import org.keycloak.testsuite.pages.UpdateProfilePage;
import org.keycloak.testsuite.rule.Page;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@RunWith(Arquillian.class)
public class AccountTest extends AbstractDroneTest {

    @Page
    protected ChangePasswordPage changePasswordPage;

    @Page
    protected UpdateProfilePage profilePage;

    @Test
    public void changePassword() {
        appPage.open();
        loginPage.register();
        registerPage.register("name", "email", "changePassword", "password", "password");

        changePasswordPage.open();
        changePasswordPage.changePassword("password", "new-password", "new-password");

        appPage.open();
        Assert.assertTrue(appPage.isCurrent());
        appPage.logout();

        Assert.assertTrue(loginPage.isCurrent());

        loginPage.login("changePassword", "password");

        Assert.assertEquals("Invalid username or password", loginPage.getError());

        loginPage.login("changePassword", "new-password");

        Assert.assertTrue(appPage.isCurrent());
        Assert.assertEquals("changePassword", appPage.getUser());
    }

    @Test
    public void changeProfile() {
        appPage.open();
        loginPage.register();
        registerPage.register("first last", "old@email.com", "changeProfile", "password", "password");

        profilePage.open();

        Assert.assertEquals("first", profilePage.getFirstName());
        Assert.assertEquals("last", profilePage.getLastName());
        Assert.assertEquals("old@email.com", profilePage.getEmail());

        profilePage.updateProfile("New first", "New last", "new@email.com");

        Assert.assertEquals("New first", profilePage.getFirstName());
        Assert.assertEquals("New last", profilePage.getLastName());
        Assert.assertEquals("new@email.com", profilePage.getEmail());
    }

}
