/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.models.credential;

import org.keycloak.common.util.Time;
import org.keycloak.models.utils.RecoveryAuthnCodesUtils;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RecoveryCodesUnitTest {

    private static final Logger logger = Logger.getLogger(RecoveryCodesUnitTest.class);

    @Test
    public void testBasicVerification() {
        Assert.assertTrue(RecoveryAuthnCodesUtils.verifyRecoveryCodeInput("L9RRAUWYKARB", "OnOEi8vsNqnI2s6t2IxU2+A+KrzWAVpR9AHExeQgDzTuoiU1qPzFsOpFIy8wb6EtPEGHKj0ehgHyTbuBTyChhg=="));
        Assert.assertFalse(RecoveryAuthnCodesUtils.verifyRecoveryCodeInput("L9RRAUWYKARC", "OnOEi8vsNqnI2s6t2IxU2+A+KrzWAVpR9AHExeQgDzTuoiU1qPzFsOpFIy8wb6EtPEGHKj0ehgHyTbuBTyChhg=="));
    }

    @Ignore
    @Test
    public void testPerf() {
        testPerf("Successful code verifications", () ->
                Assert.assertTrue(RecoveryAuthnCodesUtils.verifyRecoveryCodeInput("L9RRAUWYKARB", "OnOEi8vsNqnI2s6t2IxU2+A+KrzWAVpR9AHExeQgDzTuoiU1qPzFsOpFIy8wb6EtPEGHKj0ehgHyTbuBTyChhg=="))
        );
        testPerf("Failed code verifications 1", () ->
                Assert.assertFalse(RecoveryAuthnCodesUtils.verifyRecoveryCodeInput("L9RRAUWYKARC", "OnOEi8vsNqnI2s6t2IxU2+A+KrzWAVpR9AHExeQgDzTuoiU1qPzFsOpFIy8wb6EtPEGHKj0ehgHyTbuBTyChhg=="))
        );
        testPerf("Failed code verifications 2", () ->
                Assert.assertFalse(RecoveryAuthnCodesUtils.verifyRecoveryCodeInput("A8CWGYUIUILP", "OnOEi8vsNqnI2s6t2IxU2+A+KrzWAVpR9AHExeQgDzTuoiU1qPzFsOpFIy8wb6EtPEGHKj0ehgHyTbuBTyChhg=="))
        );
    }

    private void testPerf(String prefix, Runnable task) {
        long start = Time.currentTimeMillis();
        int count = 10000000;
        for (int i = 0 ; i < count ; i++) {
            task.run();
        }
        logger.infof("Task '%s' took %d ms", prefix, Time.currentTimeMillis() - start);
    }
}
