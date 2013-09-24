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

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@RunWith(Arquillian.class)
public class LoginTest extends AbstractDroneTest {

    @Test
    public void loginInvalidPassword() {
        appPage.open();

        Assert.assertTrue(loginPage.isCurrent());
        loginPage.login("bburke@redhat.com", "invalid");

        Assert.assertEquals("Invalid username or password", loginPage.getError());
    }

    @Test
    public void loginInvalidUsername() {
        appPage.open();

        Assert.assertTrue(loginPage.isCurrent());
        loginPage.login("invalid", "password");

        Assert.assertEquals("Invalid username or password", loginPage.getError());
    }

    @Test
    public void loginSuccess() {
        appPage.open();

        loginPage.login("bburke@redhat.com", "password");
        
        Assert.assertTrue(appPage.isCurrent());
        Assert.assertEquals("bburke@redhat.com", appPage.getUser());
    }

    @Test
    public void logout() {
        loginSuccess();
        appPage.logout();

        appPage.open();
        Assert.assertTrue(loginPage.isCurrent());
    }

}
