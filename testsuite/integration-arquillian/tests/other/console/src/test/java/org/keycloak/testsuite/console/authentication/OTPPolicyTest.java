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
import org.keycloak.testsuite.console.page.authentication.otppolicy.OTPPolicy;
import org.keycloak.testsuite.console.page.authentication.otppolicy.OTPPolicyForm.Digits;
import org.keycloak.testsuite.console.page.authentication.otppolicy.OTPPolicyForm.OTPHashAlg;
import org.keycloak.testsuite.console.page.authentication.otppolicy.OTPPolicyForm.OTPType;
import org.keycloak.testsuite.util.WaitUtils;

import static org.junit.Assert.*;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class OTPPolicyTest extends AbstractConsoleTest {
    
    @Page
    private OTPPolicy otpPolicyPage;
    
    @Before
    public void beforeOTPPolicyTest() {
        otpPolicyPage.navigateTo();
        WaitUtils.pause(1000); // wait for the form to fully render
    }
    
    @Test
    public void otpPolicyTest() {
        otpPolicyPage.form().setValues(OTPType.COUNTER_BASED, OTPHashAlg.SHA256, Digits.EIGHT, "10", "50");
        assertAlertSuccess();
        
        RealmRepresentation realm = testRealmResource().toRepresentation();
        assertEquals("hotp", realm.getOtpPolicyType());
        assertEquals("HmacSHA256", realm.getOtpPolicyAlgorithm());
        assertEquals(Integer.valueOf(8), realm.getOtpPolicyDigits());
        assertEquals(Integer.valueOf(10), realm.getOtpPolicyLookAheadWindow());
        assertEquals(Integer.valueOf(50), realm.getOtpPolicyInitialCounter());
        
        otpPolicyPage.form().setValues(OTPType.TIME_BASED, OTPHashAlg.SHA512, Digits.EIGHT, "10", "40");
        assertAlertSuccess();
        
        realm = testRealmResource().toRepresentation();
        assertEquals(Integer.valueOf(40), realm.getOtpPolicyPeriod());
    }      
    
    @Test
    public void invalidValuesTest() {
        otpPolicyPage.form().setValues(OTPType.TIME_BASED, OTPHashAlg.SHA1, Digits.SIX, "", "30");
        assertAlertDanger();
        otpPolicyPage.navigateTo();// workaround: input.clear() doesn't work when <input type="number" ...
        
        otpPolicyPage.form().setValues(OTPType.TIME_BASED, OTPHashAlg.SHA1, Digits.SIX, " ", "30");
        assertAlertDanger();
        otpPolicyPage.navigateTo();
        
        otpPolicyPage.form().setValues(OTPType.TIME_BASED, OTPHashAlg.SHA1, Digits.SIX, "no number", "30");
        assertAlertDanger();
        otpPolicyPage.navigateTo();
        
        RealmRepresentation realm = testRealmResource().toRepresentation();
        assertEquals(Integer.valueOf(1), realm.getOtpPolicyLookAheadWindow());

        otpPolicyPage.form().setValues(OTPType.TIME_BASED, OTPHashAlg.SHA1, Digits.SIX, "1", "");
        assertAlertDanger();
        otpPolicyPage.navigateTo();
        
        otpPolicyPage.form().setValues(OTPType.TIME_BASED, OTPHashAlg.SHA1, Digits.SIX, "1", " ");
        assertAlertDanger();
        otpPolicyPage.navigateTo();
        
        otpPolicyPage.form().setValues(OTPType.TIME_BASED, OTPHashAlg.SHA1, Digits.SIX, "1", "no number");
        assertAlertDanger();
        otpPolicyPage.navigateTo();
        
        realm = testRealmResource().toRepresentation();
        assertEquals(Integer.valueOf(30), realm.getOtpPolicyPeriod());
        
        otpPolicyPage.form().setValues(OTPType.COUNTER_BASED, OTPHashAlg.SHA1, Digits.SIX, "1", "");
        assertAlertDanger();
        otpPolicyPage.navigateTo();
        
        otpPolicyPage.form().setValues(OTPType.COUNTER_BASED, OTPHashAlg.SHA1, Digits.SIX, "1", " ");
        assertAlertDanger();
        otpPolicyPage.navigateTo();
        
        otpPolicyPage.form().setValues(OTPType.COUNTER_BASED, OTPHashAlg.SHA1, Digits.SIX, "1", "no number");
        assertAlertDanger();
        otpPolicyPage.navigateTo();
        
        realm = testRealmResource().toRepresentation();
        assertEquals(Integer.valueOf(0), realm.getOtpPolicyInitialCounter());
    }
}
