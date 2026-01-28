/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.crypto.elytron.test;

import java.security.SecureRandom;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.rule.CryptoInitRule;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ElytronSecureRandomTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    protected static final Logger logger = Logger.getLogger(ElytronSecureRandomTest.class);

    @Test
    public void testSecureRandom() throws Exception {
        logger.info(CryptoIntegration.dumpJavaSecurityProviders());

        SecureRandom sc1 = new SecureRandom();
        logger.infof(dumpSecureRandom("new SecureRandom()", sc1));

        SecureRandom sc3 = SecureRandom.getInstance("SHA1PRNG");
        logger.infof(dumpSecureRandom("SecureRandom.getInstance(\"SHA1PRNG\")", sc3));
        Assert.assertEquals("SHA1PRNG", sc3.getAlgorithm());
    }


    private String dumpSecureRandom(String prefix, SecureRandom secureRandom) {
        StringBuilder builder = new StringBuilder(prefix + ": algorithm: " + secureRandom.getAlgorithm() + ", provider: " + secureRandom.getProvider() + ", random numbers: ");
        for (int i=0; i < 5; i++) {
            builder.append(secureRandom.nextInt(1000) + ", ");
        }
        return builder.toString();
    }
}
