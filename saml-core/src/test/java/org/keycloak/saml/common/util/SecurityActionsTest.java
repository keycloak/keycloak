/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.saml.common.util;

import java.security.AccessControlException;
import java.security.AllPermission;
import java.security.Policy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 *
 * @author hmlnarik
 */
public class SecurityActionsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final boolean RESTRICTED = System.getSecurityManager() != null && ! Policy.getPolicy().implies(SecurityActionsTest.class.getProtectionDomain(), new AllPermission());

    @Test
    public void testLoadClass() {
        SecurityActions.loadClass(SecurityActionsTest.class, "java.lang.String");
        if (RESTRICTED) {
            expectedException.expect(SecurityException.class);
        }

        // Must be a class from a package listed in package.definition property in java.security properties file
        SecurityActions.loadClass(SecurityActions.class, "sun.misc.Unsafe");
    }

    @Test
    public void testGetTCCL() {
        if (RESTRICTED) {
            expectedException.expect(AccessControlException.class);
        }
        SecurityActions.getTCCL();
    }

    @Test
    public void testSetTCCL() {
        if (RESTRICTED) {
            expectedException.expect(AccessControlException.class);
        }
        SecurityActions.setTCCL(ClassLoader.getSystemClassLoader());
    }

}
