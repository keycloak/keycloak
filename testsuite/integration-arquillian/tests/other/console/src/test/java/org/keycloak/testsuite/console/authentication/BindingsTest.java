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
package org.keycloak.testsuite.console.authentication;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.authentication.bindings.Bindings;
import org.keycloak.testsuite.console.page.authentication.bindings.BindingsForm.BindingsOption;
import org.keycloak.testsuite.console.page.authentication.bindings.BindingsForm.BindingsSelect;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class BindingsTest extends AbstractConsoleTest {
    
    @Page
    private Bindings bindingsPage;
    
    @Before
    public void beforeBindingsTest() {
        bindingsPage.navigateTo();
    }
    
    @Test
    public void bindingsTest() {
        bindingsPage.form().select(BindingsSelect.BROWSER, BindingsOption.REGISTRATION);
        bindingsPage.form().select(BindingsSelect.REGISTRATION, BindingsOption.RESET_CREDENTIALS);
        bindingsPage.form().select(BindingsSelect.DIRECT_GRANT, BindingsOption.BROWSER);
        bindingsPage.form().select(BindingsSelect.RESET_CREDENTIALS, BindingsOption.DIRECT_GRANT);
        bindingsPage.form().save();
        
        assertEquals("Success! Your changes have been saved to the realm.", bindingsPage.getSuccessMessage());
        
        RealmRepresentation realm = testRealmResource().toRepresentation();
        
        assertEquals("registration", realm.getBrowserFlow());
        assertEquals("reset credentials", realm.getRegistrationFlow());
        assertEquals("browser", realm.getDirectGrantFlow());
        assertEquals("direct grant", realm.getResetCredentialsFlow());
        assertEquals("clients", realm.getClientAuthenticationFlow());
    }
}
