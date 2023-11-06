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

package org.keycloak.services.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.keycloak.common.util.Time;

/**
 * @author priftil
 *
 */
public class VerifyMailUtil {
    private static final int MAX_NUMBER_RETRIES = 3;

    private static final Map<String, Integer> numberOfRetries = new HashMap<>();
    private static final Map<String, Long> mailSendingTime = new HashMap<>();

    public static void store(String email) {
        Integer retries = numberOfRetries.getOrDefault(email, 0);
        if (retries < MAX_NUMBER_RETRIES) {
            retries = retries + 1;
            numberOfRetries.put(email, retries);
        }
        mailSendingTime.put(email, Time.currentTimeMillis());
    }

    public static void remove(String email) {
        mailSendingTime.remove(email);
        numberOfRetries.remove(email);
    }

    public static Long getSendingTimeByEmail(String email) {
        return mailSendingTime.get(email);
    }

    public static boolean canSendMail(String email) {
        Integer retries = numberOfRetries.getOrDefault(email, 0);
        if (retries < MAX_NUMBER_RETRIES) {
            return true;
        }
        Long lastSendingTime = mailSendingTime.get(email);

        boolean canSendMail = Time.currentTimeMillis() - lastSendingTime > 5 * 60 * 1000;

        if (canSendMail) {
            numberOfRetries.remove(email);
        }

        return canSendMail;
    }

    public static long getNumberOfSecondsRemaining(String email) {
        Long difference = Time.currentTimeMillis() - mailSendingTime.get(email);
        return 300 - TimeUnit.MILLISECONDS.toSeconds(difference);
    }

}
