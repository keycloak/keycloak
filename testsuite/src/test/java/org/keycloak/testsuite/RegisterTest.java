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
public class RegisterTest extends AbstractDroneTest {

    @Test
    public void registerExistingUser() {
        appPage.open();
        loginPage.register();

        Assert.assertTrue(registerPage.isCurrent());

        registerPage.register("name", "email", "username", null, null);

        Assert.assertTrue(registerPage.isCurrent());
        Assert.assertEquals("Please specify password", registerPage.getError());
    }

    @Test
    public void registerUserInvalidPasswordConfirm() {
        appPage.open();
        loginPage.register();

        Assert.assertTrue(registerPage.isCurrent());

        registerPage.register("name", "email", "bburke@redhat.com", "password", "invalid");

        Assert.assertTrue(registerPage.isCurrent());
        Assert.assertEquals("Password confirmation doesn't match", registerPage.getError());
    }

    @Test
    public void registerUserMissingPassword() {
        appPage.open();
        loginPage.register();

        Assert.assertTrue(registerPage.isCurrent());

        registerPage.register("name", "email", "username", null, null);

        Assert.assertTrue(registerPage.isCurrent());
        Assert.assertEquals("Please specify password", registerPage.getError());
    }

    @Test
    public void registerUserMissingUsername() {
        appPage.open();
        loginPage.register();

        Assert.assertTrue(registerPage.isCurrent());

        registerPage.register("name", "email", null, "password", "password");

        Assert.assertTrue(registerPage.isCurrent());
        Assert.assertEquals("Please specify username", registerPage.getError());
    }

    @Test
    public void registerUserSuccess() {
        appPage.open();
        loginPage.register();

        Assert.assertTrue(registerPage.isCurrent());

        registerPage.register("name", "email", "username", "password", "password");
        Assert.assertTrue(appPage.isCurrent());
    }

}
