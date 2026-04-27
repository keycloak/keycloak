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

package org.keycloak.models.utils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * TOTP: Time-based One-time Password Algorithm Based on http://tools.ietf.org/html/draft-mraihi-totp-timebased-06
 *
 * @author anil saldhana
 * @since Sep 20, 2010
 */
public class TimeBasedOTP extends HmacOTP {

    public static final int DEFAULT_INTERVAL_SECONDS = 30;
    public static final int DEFAULT_DELAY_WINDOW = 1;

    private Clock clock;

    public TimeBasedOTP() {
        this(DEFAULT_ALGORITHM, DEFAULT_NUMBER_DIGITS, DEFAULT_INTERVAL_SECONDS, DEFAULT_DELAY_WINDOW);
    }

    /**
     * @param algorithm the encryption algorithm
     * @param numberDigits the number of digits for tokens
     * @param timeIntervalInSeconds the number of seconds a token is valid
     * @param lookAroundWindow the number of previous and following intervals that should be used to validate tokens.
     */
    public TimeBasedOTP(String algorithm, int numberDigits, int timeIntervalInSeconds, int lookAroundWindow) {
        super(numberDigits, algorithm, lookAroundWindow);
        this.clock = new Clock(timeIntervalInSeconds);
    }

    /**
     * <p>Generates a token.</p>
     *
     * @param secretKey the secret key to derive the token from.
     */
    public String generateTOTP(byte[] secretKey) {
        long T = this.clock.getCurrentInterval();

        String steps = Long.toHexString(T).toUpperCase();

        // Just get a 16 digit string
        while (steps.length() < 16) {
            steps = "0" + steps;
        }

        return generateOTP(secretKey, steps, this.numberDigits, this.algorithm);
    }

    public String generateTOTP(String secretKey) {
        return generateTOTP(secretKey.getBytes());
    }

    /**
     * <p>Validates a token using a secret key.</p>
     *
     * @param token  OTP string to validate
     * @param secret Shared secret
     * @return true of the token is valid
     */
    public boolean validateTOTP(String token, byte[] secret) {
        long currentInterval = this.clock.getCurrentInterval();

        for (int i = 0; i <= (lookAroundWindow * 2); i++) {
            long delta = clockSkewIndexToDelta(i);
            long adjustedInterval = currentInterval + delta;

            String steps = Long.toHexString(adjustedInterval).toUpperCase();

            // Just get a 16 digit string
            while (steps.length() < 16) {
                steps = "0" + steps;
            }

            String candidate = generateOTP(secret, steps, this.numberDigits, this.algorithm);

            if (candidate.equals(token)) {
                return true;
            }
        }

        return false;
    }

    /**
     * maps 0, 1, 2, 3, 4, 5, 6, 7, ... to 0, -1, 1, -2, 2, -3, 3, ...
     */
    private long clockSkewIndexToDelta(int idx) {
        return (idx + 1) / 2 * (1 - (idx % 2) * 2);
    }

    public void setCalendar(Calendar calendar) {
        this.clock.setCalendar(calendar);
    }

    private static class Clock {

        private final int interval;
        private Calendar calendar;

        public Clock(int interval) {
            this.interval = interval;
        }

        public long getCurrentInterval() {
            Calendar currentCalendar = this.calendar;

            if (currentCalendar == null) {
                currentCalendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
            }

            return (currentCalendar.getTimeInMillis() / 1000) / this.interval;
        }

        public void setCalendar(Calendar calendar) {
            this.calendar = calendar;
        }
    }
}