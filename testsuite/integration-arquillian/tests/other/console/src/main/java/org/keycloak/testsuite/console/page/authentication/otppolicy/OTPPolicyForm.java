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
package org.keycloak.testsuite.console.page.authentication.otppolicy;

import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class OTPPolicyForm extends Form {
    
    @FindBy(id = "type")
    private Select otpType;
    
    @FindBy(id = "alg")
    private Select otpHashAlg;
    
    @FindBy(id = "digits")
    private Select digits;
    
    @FindBy(id = "lookAhead")
    private WebElement lookAhead;
    
    @FindBy(id = "period")
    private WebElement period;
    
    @FindBy(id = "counter")
    private WebElement counter;
    
    public void setValues(OTPType otpType, OTPHashAlg otpHashAlg, Digits digits, String lookAhead, String periodOrCounter) {
        this.otpType.selectByValue(otpType.getName());
        this.otpHashAlg.selectByValue(otpHashAlg.getName());
        this.digits.selectByVisibleText("" + digits.getName());

        UIUtils.setTextInputValue(this.lookAhead, lookAhead);
        
        switch (otpType) {
            case TIME_BASED:
                UIUtils.setTextInputValue(period, periodOrCounter);
                break;
            case COUNTER_BASED:
                UIUtils.setTextInputValue(counter, periodOrCounter);
                break;
        }
        save();
    }
    
    public enum OTPType {

        TIME_BASED("totp"),
        COUNTER_BASED("hotp");

        private final String name;

        private OTPType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
    
    public enum OTPHashAlg {

        SHA1("HmacSHA1"),
        SHA256("HmacSHA256"),
        SHA512("HmacSHA512");

        private final String name;

        private OTPHashAlg(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
    
    public enum Digits {

        SIX(6),
        EIGHT(8);

        private final int name;

        private Digits(int name) {
            this.name = name;
        }

        public int getName() {
            return name;
        }
    }
}
