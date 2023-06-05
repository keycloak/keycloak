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

package org.keycloak.models;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.HmacOTP;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HmacTest {

    @Test
    public void testHmac() {
        HmacOTP hmacOTP = new HmacOTP(6, HmacOTP.HMAC_SHA1, 10);
        String secret = "JNSVMMTEKZCUGSKJIVGHMNSQOZBDA5JT";
        String decoded = new String(Base32.decode(secret));
        System.out.println(hmacOTP.generateHOTP(decoded, 0));
        System.out.println(hmacOTP.validateHOTP("550233", decoded, 0));
        Assert.assertEquals(1, hmacOTP.validateHOTP("550233", decoded, 0));
    }
    @Test
    public void testHmacBase32() {
        HmacOTP hmacOTP = new HmacOTP(6, HmacOTP.HMAC_SHA1, 10);
        String decoded = "{B32}JNSVMMTEKZCUGSKJIVGHMNSQOZBDA5JT";
        System.out.println(hmacOTP.generateHOTP(decoded, 0));
        System.out.println(hmacOTP.validateHOTP("550233", decoded, 0));
        Assert.assertEquals(1, hmacOTP.validateHOTP("550233", decoded, 0));
    }
    @Test
    public void testHmacBase32Binary() {
        // Secret from https://github.com/keycloak/keycloak/issues/11561
        // Codes values validated aganist https://totp.app/
        HmacOTP hmacOTP = new HmacOTP(6, HmacOTP.HMAC_SHA1, 30);
        String decoded = "{B32}CDLYAYRJ73ORTU4PUWWATWSYQCP4H2QL";
        String counter = "000000000359675C";
        Assert.assertEquals("754397", hmacOTP.generateOTP(decoded, counter, 6, HmacOTP.HMAC_SHA1));
        String counter2 = "0000000003596781";
        Assert.assertEquals("386679", hmacOTP.generateOTP(decoded, counter2, 6, HmacOTP.HMAC_SHA1));
    }    
}
